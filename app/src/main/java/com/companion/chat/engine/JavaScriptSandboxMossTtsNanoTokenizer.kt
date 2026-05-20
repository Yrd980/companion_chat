package com.companion.chat.engine

import android.content.Context
import android.util.Base64
import androidx.javascriptengine.JavaScriptSandbox
import kotlinx.coroutines.guava.await
import org.json.JSONArray
import java.io.File

class JavaScriptSandboxMossTtsNanoTokenizer(
    private val context: Context
) : MossTtsNanoTokenizer {

    override suspend fun encode(modelFile: File, text: String): IntArray {
        require(modelFile.isFile) { "MOSS tokenizer.model 文件缺失：${modelFile.absolutePath}" }
        require(JavaScriptSandbox.isSupported()) { "当前设备不支持 AndroidX JavaScriptSandbox tokenizer" }

        val sandbox = JavaScriptSandbox.createConnectedInstanceAsync(context).await()
        try {
            val isolate = sandbox.createIsolate()
            try {
                val tokenizerSource = context.assets.open(TOKENIZER_SANDBOX_ASSET).use { input ->
                    input.bufferedReader().readText()
                }
                isolate.evaluateJavaScriptAsync(
                    "globalThis.window = globalThis; globalThis.addEventListener = function(){};\n$tokenizerSource"
                ).await()
                val escapedText = JSONObjectCompat.quote(text)
                val tokenizerBase64 = JSONObjectCompat.quote(
                    Base64.encodeToString(modelFile.readBytes(), Base64.NO_WRAP)
                )
                val result = isolate.evaluateJavaScriptAsync(
                    """
                        (async () => {
                          const processor = new SentencePieceProcessor();
                          await processor.loadFromB64StringModel($tokenizerBase64);
                          return JSON.stringify(processor.encodeIds($escapedText));
                        })();
                    """.trimIndent()
                ).await()
                val array = JSONArray(result)
                return IntArray(array.length()) { index -> array.getInt(index) }
            } finally {
                isolate.close()
            }
        } finally {
            sandbox.close()
        }
    }

    private companion object {
        const val TOKENIZER_SANDBOX_ASSET = "tokenizer_sandbox.js"
    }
}

private object JSONObjectCompat {
    fun quote(value: String): String = org.json.JSONObject.quote(value)
}
