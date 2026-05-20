package com.companion.chat.engine

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer
import java.nio.IntBuffer

class AndroidMossTtsNanoRunner(
    private val sessionFactory: MossTtsNanoOnnxSessionFactory = OrtMossTtsNanoOnnxSessionFactory(),
    private val randomProvider: MossTtsNanoRandomProvider = DefaultMossTtsNanoRandomProvider()
) : MossTtsNanoRunner {

    override fun synthesize(request: MossTtsNanoRunnerRequest): FloatArray {
        val sessions = loadSessions(request)
        try {
            val prefillSession = sessions.firstOrNull { it.stageName == "prefill" }
                ?: error("moss-tts-nano 执行计划缺少 prefill 会话")
            val promptAudioCodes = request.plan.builtinVoices.firstOrNull()?.promptAudioCodes
                ?: error("MOSS browser manifest 缺少内置 voice prompt_audio_codes")
            val prefill = runPrefill(
                session = prefillSession.session,
                promptAudioCodes = promptAudioCodes,
                textTokenIds = request.textTokenIds,
                plan = request.plan
            )
            val decodeSession = sessions.firstOrNull { it.stageName == "decode" }
                ?: error("moss-tts-nano 执行计划缺少 decode 会话")
            val localFixedSession = sessions.firstOrNull { it.stageName == "localFixedSampledFrame" }
                ?: error("moss-tts-nano 执行计划缺少 localFixedSampledFrame 会话")
            val codecDecodeSession = sessions.firstOrNull { it.stageName == "codecDecode" }
                ?: error("moss-tts-nano 执行计划缺少 codecDecode 会话")
            val frame = runLocalFixedSampledFrame(
                session = localFixedSession.session,
                globalHidden = prefill.globalHidden,
                previousTokenSetsByChannel = List(request.plan.frameChannels) { emptySet<Int>() },
                plan = request.plan
            ).takeIf { it.shouldContinue }?.frame
                ?: error("moss-tts-nano 未生成音频帧")
            val decode = runDecodeStep(
                session = decodeSession.session,
                prefill = prefill,
                frame = frame,
                pastValidLength = prefill.validLength,
                plan = request.plan
            )
            val waveform = decodeFullAudio(codecDecodeSession.session, listOf(frame), request.plan)
            check(decode.presentState.isNotEmpty() || request.plan.ttsDecodeOutputNames.size <= 1) {
                "decode 未输出 cache state"
            }
            return waveform
        } finally {
            sessions.asReversed().forEach { it.close() }
        }
    }

    private fun loadSessions(request: MossTtsNanoRunnerRequest): List<MossTtsNanoLoadedSession> {
        return request.plan.sessionOrder.map { spec ->
            val modelFile = File(request.modelDirectory, spec.relativePath)
            require(modelFile.isFile) { "moss-tts-nano ONNX 会话文件缺失：${spec.name} -> ${spec.relativePath}" }
            MossTtsNanoLoadedSession(
                stageName = spec.name,
                session = sessionFactory.createSession(modelFile)
            )
        }
    }

    private fun runPrefill(
        session: MossTtsNanoOnnxSession,
        promptAudioCodes: List<IntArray>,
        textTokenIds: IntArray,
        plan: com.companion.chat.engine.voice.MossTtsNanoRunnerPlan
    ): MossTtsNanoPrefillResult {
        val rows = buildVoiceCloneRequestRows(promptAudioCodes, textTokenIds, plan)
        val inputIds = rows.flattenToIntArray()
        val attentionMask = IntArray(rows.size) { 1 }
        val outputs = session.run(
            mapOf(
                "input_ids" to MossTtsNanoTensor.intTensor(inputIds, longArrayOf(1, rows.size.toLong(), (plan.frameChannels + 1).toLong())),
                "attention_mask" to MossTtsNanoTensor.intTensor(attentionMask, longArrayOf(1, rows.size.toLong()))
            )
        )
        val globalHidden = outputs["global_hidden"] ?: error("prefill 输出缺少 global_hidden")
        return MossTtsNanoPrefillResult(
            globalHidden = globalHidden.extractLastHidden(),
            presentState = outputs.filterKeys { it != "global_hidden" },
            validLength = rows.size
        )
    }

    private fun runDecodeStep(
        session: MossTtsNanoOnnxSession,
        prefill: MossTtsNanoPrefillResult,
        frame: IntArray,
        pastValidLength: Int,
        plan: com.companion.chat.engine.voice.MossTtsNanoRunnerPlan
    ): MossTtsNanoDecodeResult {
        val row = IntArray(plan.frameChannels + 1) { index ->
            when (index) {
                0 -> plan.audioAssistantSlotTokenId
                else -> frame.getOrElse(index - 1) { plan.audioPadTokenId }
            }
        }
        val feeds = mutableMapOf(
            "input_ids" to MossTtsNanoTensor.intTensor(row, longArrayOf(1, 1, row.size.toLong())),
            "past_valid_lengths" to MossTtsNanoTensor.intTensor(intArrayOf(pastValidLength), longArrayOf(1))
        )
        val pastInputNames = plan.ttsDecodeInputNames.drop(2)
        val presentOutputNames = plan.ttsDecodeOutputNames.drop(1)
        require(pastInputNames.size == presentOutputNames.size) {
            "decode past/present 名称数量不一致：inputs=${pastInputNames.size}, outputs=${presentOutputNames.size}"
        }
        pastInputNames.zip(presentOutputNames).forEach { (inputName, outputName) ->
            feeds[inputName] = prefill.presentState[outputName]
                ?: error("prefill 输出缺少 decode cache：$outputName")
        }
        val outputs = session.run(feeds)
        val globalHidden = outputs["global_hidden"] ?: error("decode 输出缺少 global_hidden")
        return MossTtsNanoDecodeResult(
            globalHidden = globalHidden.extractLastHidden(),
            presentState = outputs.filterKeys { it != "global_hidden" }
        )
    }

    private fun runLocalFixedSampledFrame(
        session: MossTtsNanoOnnxSession,
        globalHidden: MossTtsNanoTensor,
        previousTokenSetsByChannel: List<Set<Int>>,
        plan: com.companion.chat.engine.voice.MossTtsNanoRunnerPlan
    ): MossTtsNanoFrameResult {
        val audioCodebookSize = 1024
        val seenMask = IntArray(plan.frameChannels * audioCodebookSize)
        previousTokenSetsByChannel.forEachIndexed { channelIndex, tokenSet ->
            val offset = channelIndex * audioCodebookSize
            tokenSet.forEach { tokenId ->
                if (tokenId in 0 until audioCodebookSize) seenMask[offset + tokenId] = 1
            }
        }
        val outputs = session.run(
            mapOf(
                "global_hidden" to globalHidden,
                "repetition_seen_mask" to MossTtsNanoTensor.intTensor(
                    seenMask,
                    longArrayOf(1, plan.frameChannels.toLong(), audioCodebookSize.toLong())
                ),
                "assistant_random_u" to MossTtsNanoTensor.floatTensor(
                    floatArrayOf(randomProvider.nextFloat()),
                    longArrayOf(1)
                ),
                "audio_random_u" to MossTtsNanoTensor.floatTensor(
                    FloatArray(plan.frameChannels) { randomProvider.nextFloat() },
                    longArrayOf(1, plan.frameChannels.toLong())
                )
            )
        )
        val shouldContinue = outputs["should_continue"]?.intData?.firstOrNull()?.let { it > 0 }
            ?: error("localFixedSampledFrame 输出缺少 should_continue")
        val frame = outputs["frame_token_ids"]?.intData
            ?: error("localFixedSampledFrame 输出缺少 frame_token_ids")
        require(frame.size == plan.frameChannels) {
            "localFixedSampledFrame frame_token_ids 数量不匹配：${frame.size}"
        }
        return MossTtsNanoFrameResult(shouldContinue = shouldContinue, frame = frame)
    }

    private fun decodeFullAudio(
        session: MossTtsNanoOnnxSession,
        generatedFrames: List<IntArray>,
        plan: com.companion.chat.engine.voice.MossTtsNanoRunnerPlan
    ): FloatArray {
        require(generatedFrames.isNotEmpty()) { "moss-tts-nano 未生成音频帧" }
        val flattened = IntArray(generatedFrames.size * plan.frameChannels)
        generatedFrames.forEachIndexed { frameIndex, frame ->
            require(frame.size == plan.frameChannels) { "音频帧宽度不匹配：${frame.size}" }
            frame.copyInto(flattened, destinationOffset = frameIndex * plan.frameChannels)
        }
        val outputs = session.run(
            mapOf(
                "audio_codes" to MossTtsNanoTensor.intTensor(
                    flattened,
                    longArrayOf(1, generatedFrames.size.toLong(), plan.frameChannels.toLong())
                ),
                "audio_code_lengths" to MossTtsNanoTensor.intTensor(
                    intArrayOf(generatedFrames.size),
                    longArrayOf(1)
                )
            )
        )
        val audio = outputs["audio"] ?: error("codecDecode 输出缺少 audio")
        val audioLength = outputs["audio_lengths"]?.intData?.firstOrNull()
            ?: error("codecDecode 输出缺少 audio_lengths")
        return audio.sliceChannelMajorToMono(audioLength)
    }

    private fun buildVoiceCloneRequestRows(
        promptAudioCodes: List<IntArray>,
        textTokenIds: IntArray,
        plan: com.companion.chat.engine.voice.MossTtsNanoRunnerPlan
    ): List<IntArray> {
        require(plan.frameChannels > 0) { "frameChannels 必须大于 0" }
        require(textTokenIds.isNotEmpty()) { "tokenizer 未输出文本 token" }
        val rowWidth = plan.frameChannels + 1
        return buildList {
            plan.userPromptPrefixTokenIds.forEach { tokenId ->
                add(textRow(tokenId, rowWidth))
            }
            add(textRow(plan.audioStartTokenId, rowWidth))
            promptAudioCodes.forEach { codeRow ->
                add(audioRow(plan.audioUserSlotTokenId, codeRow, plan))
            }
            add(textRow(plan.audioEndTokenId, rowWidth))
            plan.userPromptAfterReferenceTokenIds.forEach { tokenId ->
                add(textRow(tokenId, rowWidth))
            }
            textTokenIds.forEach { tokenId ->
                add(textRow(tokenId, rowWidth))
            }
            plan.assistantPromptPrefixTokenIds.forEach { tokenId ->
                add(textRow(tokenId, rowWidth))
            }
            add(textRow(plan.audioStartTokenId, rowWidth))
        }
    }

    private fun textRow(tokenId: Int, rowWidth: Int): IntArray {
        return IntArray(rowWidth) { index -> if (index == 0) tokenId else 0 }
    }

    private fun audioRow(
        slotTokenId: Int,
        codeRow: IntArray,
        plan: com.companion.chat.engine.voice.MossTtsNanoRunnerPlan
    ): IntArray {
        val rowWidth = plan.frameChannels + 1
        return IntArray(rowWidth) { index ->
            when (index) {
                0 -> slotTokenId
                else -> codeRow.getOrElse(index - 1) { plan.audioPadTokenId }
            }
        }
    }

    private fun List<IntArray>.flattenToIntArray(): IntArray {
        val width = firstOrNull()?.size ?: 0
        val flattened = IntArray(size * width)
        forEachIndexed { rowIndex, row ->
            row.copyInto(flattened, destinationOffset = rowIndex * width)
        }
        return flattened
    }
}

data class MossTtsNanoLoadedSession(
    val stageName: String,
    val session: MossTtsNanoOnnxSession
) : AutoCloseable {
    override fun close() {
        session.close()
    }
}

interface MossTtsNanoOnnxSessionFactory {
    fun createSession(modelFile: File): MossTtsNanoOnnxSession
}

interface MossTtsNanoOnnxSession : AutoCloseable {
    fun run(inputs: Map<String, MossTtsNanoTensor>): Map<String, MossTtsNanoTensor>
}

interface MossTtsNanoRandomProvider {
    fun nextFloat(): Float
}

class DefaultMossTtsNanoRandomProvider : MossTtsNanoRandomProvider {
    override fun nextFloat(): Float = kotlin.random.Random.nextFloat().coerceIn(0f, 0.99999994f)
}

data class MossTtsNanoPrefillResult(
    val globalHidden: MossTtsNanoTensor,
    val presentState: Map<String, MossTtsNanoTensor>,
    val validLength: Int
)

data class MossTtsNanoDecodeResult(
    val globalHidden: MossTtsNanoTensor,
    val presentState: Map<String, MossTtsNanoTensor>
)

data class MossTtsNanoFrameResult(
    val shouldContinue: Boolean,
    val frame: IntArray
)

data class MossTtsNanoTensor(
    val type: MossTtsNanoTensorType,
    val intData: IntArray = IntArray(0),
    val floatData: FloatArray = FloatArray(0),
    val shape: LongArray
) {
    fun extractLastHidden(): MossTtsNanoTensor {
        require(type == MossTtsNanoTensorType.FLOAT) { "global_hidden 必须是 float32 tensor" }
        if (shape.size == 2) return this
        require(shape.size == 3) { "Unexpected global_hidden rank: ${shape.size}" }
        val batchSize = shape[0].toInt()
        val sequenceLength = shape[1].toInt()
        val hiddenSize = shape[2].toInt()
        require(batchSize == 1) { "Only batch_size=1 is supported in Android MOSS runner, got $batchSize" }
        val start = (sequenceLength - 1) * hiddenSize
        return floatTensor(
            data = floatData.copyOfRange(start, start + hiddenSize),
            shape = longArrayOf(1, hiddenSize.toLong())
        )
    }

    fun sliceChannelMajorToMono(audioLength: Int): FloatArray {
        require(type == MossTtsNanoTensorType.FLOAT) { "audio 必须是 float32 tensor" }
        require(shape.size == 3) { "Unexpected audio rank: ${shape.size}" }
        val batchSize = shape[0].toInt()
        val channels = shape[1].toInt()
        val totalSamples = shape[2].toInt()
        require(batchSize == 1) { "Only batch_size=1 is supported in Android MOSS runner, got $batchSize" }
        val length = audioLength.coerceIn(0, totalSamples)
        if (channels == 1) return floatData.copyOfRange(0, length)
        val mono = FloatArray(length)
        for (sampleIndex in 0 until length) {
            var sum = 0f
            for (channelIndex in 0 until channels) {
                sum += floatData[channelIndex * totalSamples + sampleIndex]
            }
            mono[sampleIndex] = sum / channels
        }
        return mono
    }

    fun requireIntScalar(name: String): Int {
        require(type == MossTtsNanoTensorType.INT) { "$name 必须是 int32 tensor" }
        require(intData.isNotEmpty()) { "$name 为空" }
        return intData[0]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MossTtsNanoTensor) return false
        return type == other.type &&
            intData.contentEquals(other.intData) &&
            floatData.contentEquals(other.floatData) &&
            shape.contentEquals(other.shape)
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + intData.contentHashCode()
        result = 31 * result + floatData.contentHashCode()
        result = 31 * result + shape.contentHashCode()
        return result
    }

    companion object {
        fun intTensor(data: IntArray, shape: LongArray) = MossTtsNanoTensor(
            type = MossTtsNanoTensorType.INT,
            intData = data,
            shape = shape
        )

        fun floatTensor(data: FloatArray, shape: LongArray) = MossTtsNanoTensor(
            type = MossTtsNanoTensorType.FLOAT,
            floatData = data,
            shape = shape
        )
    }
}

enum class MossTtsNanoTensorType {
    INT,
    FLOAT
}

class OrtMossTtsNanoOnnxSessionFactory : MossTtsNanoOnnxSessionFactory {
    override fun createSession(modelFile: File): MossTtsNanoOnnxSession {
        val environment = OrtEnvironment.getEnvironment()
        val options = OrtSession.SessionOptions()
        return OrtMossTtsNanoOnnxSession(
            environment = environment,
            session = environment.createSession(modelFile.absolutePath, options),
            options = options
        )
    }
}

private class OrtMossTtsNanoOnnxSession(
    private val environment: OrtEnvironment,
    private val session: OrtSession,
    private val options: OrtSession.SessionOptions
) : MossTtsNanoOnnxSession {
    override fun run(inputs: Map<String, MossTtsNanoTensor>): Map<String, MossTtsNanoTensor> {
        val ortInputs = inputs.mapValues { (_, tensor) -> tensor.toOnnxTensor(environment) }
        return ortInputs.useAll {
            session.run(ortInputs).use { outputs ->
                outputs.associate { namedValue ->
                    val value = namedValue.value
                    val tensor = value as? OnnxTensor
                        ?: error("ONNX 输出 ${namedValue.key} 不是 tensor")
                    namedValue.key to tensor.toMossTensor()
                }
            }
        }
    }

    override fun close() {
        session.close()
        options.close()
    }

    private fun MossTtsNanoTensor.toOnnxTensor(environment: OrtEnvironment): OnnxTensor {
        return when (type) {
            MossTtsNanoTensorType.INT -> OnnxTensor.createTensor(environment, IntBuffer.wrap(intData), shape)
            MossTtsNanoTensorType.FLOAT -> OnnxTensor.createTensor(environment, FloatBuffer.wrap(floatData), shape)
        }
    }

    private fun OnnxTensor.toMossTensor(): MossTtsNanoTensor {
        val tensorShape = info.shape
        return when (val value = value) {
            is IntArray -> MossTtsNanoTensor.intTensor(value, tensorShape)
            is FloatArray -> MossTtsNanoTensor.floatTensor(value, tensorShape)
            else -> error("不支持的 ONNX tensor 数据类型：${value::class.java.simpleName}")
        }
    }

    private inline fun <T> Map<String, OnnxTensor>.useAll(block: () -> T): T {
        try {
            return block()
        } finally {
            values.forEach { it.close() }
        }
    }
}
