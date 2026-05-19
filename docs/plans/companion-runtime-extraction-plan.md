# CompanionRuntime Extraction Plan

This plan turns the DDD boundary design into a safe implementation sequence. It is based on parallel exploration of `ChatViewModel`, post-turn learning, and the current test harness.

## Goal

Extract the **AI 伴侣** turn orchestration from `ChatViewModel` into a small `CompanionRuntime` module without changing product behavior first.

The target seam should hide:

- active **RoleCard** and **Skill** prompt composition
- **Memory** and **UserPreference** context injection
- context rebuild and replay
- inference streaming
- post-turn learning scheduling

The `ChatViewModel` should keep:

- UI state projection
- text/image input state
- voice permission state
- session drawer and title editing state
- streaming token projection into `ChatUiState`
- manual voice playback controls

## First Deepening Opportunity

### Files

- `app/src/main/java/com/companion/chat/ui/chat/ChatViewModel.kt`
- `app/src/main/java/com/companion/chat/companion/PreferenceLearningCoordinator.kt`
- `app/src/main/java/com/companion/chat/context/*`
- `app/src/main/java/com/companion/chat/memory/*`
- `app/src/main/java/com/companion/chat/preference/*`
- `app/src/main/java/com/companion/chat/data/memory/*`
- `app/src/main/java/com/companion/chat/data/preferences/*`
- `app/src/main/java/com/companion/chat/identity/RoleCardPromptBuilder.kt`
- `app/src/main/java/com/companion/chat/capability/SkillRepository.kt`
- `app/src/main/java/com/companion/chat/engine/InferenceEngine.kt`
- `app/src/main/java/com/companion/chat/engine/image/*`

### Problem

`ChatViewModel` currently has a shallow public surface but a very large implementation. Understanding one **AI 伴侣** turn requires reading UI state mutation, prompt composition, memory retrieval, preference injection, engine state mutation, session persistence, and post-turn learning in one class.

The friction is not that `ChatViewModel` is long by itself. The deeper issue is locality: bugs in one turn can hide across several private functions with UI state, engine state, and background learning all interleaved.

### Solution

Create a `CompanionRuntime` module with a small interface for one companion turn and prompt rebuild events.

First interface sketch:

```kotlin
class CompanionRuntime {
    suspend fun refreshBasePrompt(): String
    suspend fun runTurn(request: CompanionTurnRequest): kotlinx.coroutines.flow.Flow<CompanionTurnEvent>
    suspend fun rebuildForPromptChange(request: PromptChangeRequest): PromptChangeResult
    fun onTurnFinished(sessionId: String, messages: List<ChatMessage>)
    fun onConversationBoundary(reason: String, sessionId: String, messages: List<ChatMessage>)
    fun cancelPostTurnLearning()
    fun release()
}
```

The first extraction should preserve current behavior and avoid package moves. The module can initially live near the existing chat code, then move after tests stabilize.

### Benefits

- **Leverage**: one runtime test can exercise RoleCard + Skill + Memory + UserPreference + context rebuild + engine calls.
- **Locality**: turn orchestration bugs concentrate in `CompanionRuntime` instead of leaking through UI state.
- **Test surface**: tests can use fake repositories and a fake `InferenceEngine` without Android `Application` or Compose state.
- **AI-navigability**: future agents can start from `CompanionRuntime` when investigating companion behavior instead of spelunking `ChatViewModel`.

## Keep In ChatViewModel

These should not move in the first extraction:

- `ChatUiState`
- `updateInputText`
- selected image add/remove
- voice permission dialog state
- drawer/search/date filter/title editing state
- `appendAssistantToken` and UI placeholder projection
- manual `speakMessage`, `speakLatestAssistantMessage`, `stopSpeaking`

Runtime may emit events such as `AssistantToken`, but the ViewModel remains responsible for how those events become UI state.

## Move First

Move these responsibilities behind the runtime seam first:

1. Base prompt composition from default prompt + active **RoleCard** + active **Skill**.
2. **Memory** and **UserPreference** context lookup and prompt formatting.
3. Context rebuild, recent message replay, and fallback prompt rebuild.
4. Turn streaming through `InferenceEngine`.
5. Post-turn learning scheduling and cancellation hooks.

## Riskiest Couplings To Preserve

### Engine message timing

Current behavior captures `messages` before context rebuild, rebuilds/replays filtered stable messages, then calls `sendMessageStream(messages)`. The runtime tests must lock this in before refactoring.

### Mutable engine conversation

`InferenceEngine` exposes separate rebuild, replay, and send calls. Cancellation, RoleCard switching, or Skill switching between those calls can diverge UI messages from engine state.

### RoleCard and Skill prompt continuity

**RoleCard** defines companion identity. **Skill** layers task behavior. Runtime extraction must preserve active RoleCard when Skill changes, and preserve active Skill when RoleCard changes.

### Learning and sending share engine assumptions

The post-turn learning flow checks foreground generation and uses a second engine. Sending cancels running summaries. Moving one without the other risks races.

### Streaming placeholder persistence

`ChatViewModel` currently persists UI message state around streaming placeholders. The first runtime should not silently change whether placeholders are persisted or filtered.

## Test Plan Before Code Movement

Add tests against a pure Kotlin runtime harness, not `AndroidViewModel`.

### Runtime Turn Tests

1. `runTurn_rebuilds_context_with_role_skill_preferences_and_memories`

Seed active RoleCard, active Skill, confirmed UserPreference, Long-term Memory, relevant Memory, and prior messages. Assert the fake engine receives a rebuilt prompt containing those sections and then receives the expected send call.

2. `runTurn_falls_back_to_recent_snippet_when_replay_fails`

Fake engine returns success for rebuild, failure for replay, then assert fallback rebuild prompt contains `最近几轮对话片段`.

3. `runTurn_with_auto_learning_disabled_stores_rule_based_memory_before_generation`

Assert rule-based memory extraction runs when automatic preference learning is disabled.

### Prompt Change Tests

4. `activateRoleCard_rebuilds_prompt_without_losing_active_skill`

Assert RoleCard activation changes identity prompt while Skill prompt remains layered.

5. `activateSkill_rebuilds_prompt_without_replacing_role_identity`

Assert Skill activation changes task behavior while RoleCard identity prompt remains.

### Learning Tests

6. `turnFinished_schedules_idle_learning`

Assert finishing a turn schedules idle learning through `PreferenceLearningCoordinator`.

7. `conversationBoundary_triggers_immediate_learning_for_old_session`

Assert switching session, creating a new session, starting role conversation, and app background trigger learning with the previous session/messages.

8. `completed_learning_summary_stores_memories_and_merges_preferences`

Assert parsed memories plus derived preference memories go to `MemoryRepository.storeModelExtractedMemories`, then preferences go to `PreferenceRepository.mergePreferences`.

9. `failed_learning_summary_stores_fallback_rule_memories_from_user_messages_only`

Assert fallback uses user messages only.

## Implementation Sequence

1. Add fake `InferenceEngine` and fake runtime collaborators in tests.
2. Add `CompanionRuntime` with dependencies injected directly.
3. Move base prompt composition into runtime.
4. Move memory/preference context lookup into runtime.
5. Move context rebuild/replay/fallback into runtime.
6. Move turn streaming into runtime as events.
7. Move post-turn learning hooks behind runtime.
8. Adapt `ChatViewModel` to map runtime events into `ChatUiState`.
9. Run focused unit tests.
10. Only then consider package moves into `companion`, `conversation`, `identity`, `memory`, `preference`, `capability`, and `engine`.

## Current Status

The first runtime seam is in place:

- `CompanionRuntime` owns base prompt composition, memory/preference prompt lookup, context rebuild/replay/fallback, inference streaming events, and post-turn learning hooks.
- `ChatViewModel` maps runtime turn events into UI state and diagnostic logs while retaining session, voice, image, drawer, and streaming placeholder projection.
- `CompanionTurnEvent.ContextRebuildCompleted` preserves send-path rebuild/replay/fallback diagnostics before assistant tokens.
- `CompanionTurnEvent.TurnFailed` reports stream failures while preserving the existing UI error projection.
- The ADR package migration slice is complete for non-persistence modules: RoleCard business code lives in `identity`, Skill business code lives in `capability`, context orchestration lives in `context`, pure Memory helpers live in `memory`, pure UserPreference helpers live in `preference`, model/voice engine ports live in `engine`, and image generation ports/adapters live in `engine/image`.
- Room persistence remains in `data/local`, with persistence-facing repositories still under `data/memory`, `data/preferences`, and `data/repository`.

Remaining work:

- Re-verify cancellation on device after the cancellation-error handling fix.
- Consider persistence adapter migration only after Room/KSP risk is isolated.
- MOSS voice cloning remains fallback-oriented until a tokenizer/reference-audio/autoregressive ONNX decode pipeline is implemented.

Device smoke pass:

- Debug build installed and launched on a connected device; `MainActivity` resumed without a fatal logcat entry.
- Switching the device to vivo input method allowed ADB text injection into the Compose input field.
- Sending `hello_vivo` completed successfully on device and emitted send-path context diagnostics plus a normal assistant response.
- Generation now exposes a `取消生成` control in the chat input. An earlier device build proved the control can call into cancellation and the engine logged `推理被取消`; the follow-up fix prevents cancellation from being reported as a turn failure in code/tests, but could not be reinstalled for device confirmation because the device rejected the install permission prompt.
- App backgrounding produced the expected post-turn learning boundary log.
- Existing device logs showed RoleCard prompt-change no-user rebuild and session-switch boundary learning logs.

Test coverage added after the first runtime seam:

- `CompanionRuntimeTest` covers send-path rebuild diagnostics before tokens, stream failures, cancellation propagation, no-user prompt-change rebuilds, and conversation boundary learning events.
- `ChatRuntimeActionsTest` covers the ViewModel-facing wiring rules for auto-learning-off rule memory writes and boundary-trigger forwarding.
- `MemoryRepositoryWriteTest` covers real rule-extractor persistence for the fallback memory path.
- `LocalImageGenerationEngineTest` covers per-request Stable Diffusion seed/step overrides before native image generation.

## Explicit Non-goals

- Do not introduce **Relationship State** in this extraction.
- Do not split **Short-term Memory** and **Long-term Memory** into separate model families.
- Do not move concrete model engines into the domain layer.
- Do not rewrite session drawer or voice permission behavior.
- Do not mechanically move packages before the runtime seam is covered.
