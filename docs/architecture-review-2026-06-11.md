# Architecture Review - 2026-06-11

This records the architecture review produced from the `improve-codebase-architecture`
pass on CompanionChat. The visual HTML report was generated outside the repo at:

```text
C:\Users\Yrd98\AppData\Local\Temp\architecture-review-20260611-192924.html
```

## Context

CompanionChat is a local-first Anime Companion application. The review focused on
the first-party Android Kotlin code under:

```text
app/src/main/java/com/companion/chat
```

The scan excluded `third_party`, Gradle output, generated output, and large
vendored code. No ADR files were found in the usual `docs/adr` location.

The review used the domain language from `CONTEXT.md`:

- Anime Companion
- Companion Turn
- Model Runtime Lifecycle
- Role Card
- Skill
- Durable Memory
- Preference Learning
- Voice-First Interaction

## Findings

### 1. Deepen the Companion Turn transaction

Recommendation strength: Strong

Relevant modules:

- `app/src/main/java/com/companion/chat/companion/turn/CompanionTurnModule.kt`
- `app/src/main/java/com/companion/chat/companion/turn/DefaultCompanionTurnModule.kt`
- `app/src/main/java/com/companion/chat/ui/chat/ChatViewModel.kt`
- `app/src/main/java/com/companion/chat/companion/CompanionRuntime.kt`
- `app/src/main/java/com/companion/chat/data/model/ChatMessage.kt`
- `app/src/main/java/com/companion/chat/data/local/entity/MessageEntity.kt`

Problem:

The Companion Turn seam is doing real work, but its interface is still too wide.
`ChatViewModel` coordinates turn submission alongside voice state, timeline writes,
input cleanup, pinned memory refresh, and UI event handling. The implementation
also orders persistence, voice playback, and Preference Learning as side effects
after streaming.

Risk:

Process death or cancellation between the first session save and final streaming
save can leave awkward durable state. Tests for one Companion Turn would need to
know about sessions, runtime state, voice playback, memory refresh, timeline
events, and Preference Learning.

Deepening direction:

Create a deeper Companion Turn transaction module whose interface accepts a
Companion Turn intent and emits explicit outcomes:

- accepted or rejected turn
- streaming assistant text
- final assistant message commit
- voice playback request
- timeline event
- Preference Learning trigger
- Durable Memory refresh signal

Expected leverage:

- Locality: side effects concentrate in one module.
- Leverage: one Companion Turn test surface covers persistence, voice, timeline,
  and learning behavior.
- The UI submits intent instead of reassembling product workflow details.

### 2. Deepen Durable Memory review and injection

Recommendation strength: Strong

Relevant modules:

- `app/src/main/java/com/companion/chat/companion/CompanionRuntime.kt`
- `app/src/main/java/com/companion/chat/context/DefaultContextManager.kt`
- `app/src/main/java/com/companion/chat/context/PromptAssembler.kt`
- `app/src/main/java/com/companion/chat/data/memory/MemoryRepository.kt`
- `app/src/main/java/com/companion/chat/ui/memory/MemoryViewModel.kt`
- `app/src/main/java/com/companion/chat/data/dashboard/HomeDashboardRepository.kt`

Problem:

Durable Memory leaks as storage flags and rendered prompt strings. Candidate,
pinned, confirmed, persistent, retrieved, and one-turn memories are handled across
repository calls, UI filtering, Home projections, and prompt assembly.

Risk:

The interface encourages tests to assert prompt text instead of Durable Memory
behavior. Review-state rules can drift between Home, Memory, Chat, and the
Companion Turn path.

Deepening direction:

Create a Durable Memory module that owns:

- candidate review queue
- confirmed memory projection
- pinned memory projection
- one-turn memory use
- prompt-ready memory injection
- memory health metrics

Expected leverage:

- Locality: review rules and injection rules live in one place.
- Leverage: Home, Memory, and Chat consume product projections.
- Prompt-string formatting becomes implementation detail.

### 3. Deepen the Model Runtime Lifecycle

Recommendation strength: Worth exploring

Relevant modules:

- `app/src/main/java/com/companion/chat/engine/InferenceEngine.kt`
- `app/src/main/java/com/companion/chat/engine/InferenceEngineFactory.kt`
- `app/src/main/java/com/companion/chat/engine/ModelConfigRepository.kt`
- `app/src/main/java/com/companion/chat/companion/readiness/CompanionReadinessRepository.kt`
- `app/src/main/java/com/companion/chat/companion/CompanionRuntime.kt`
- `app/src/main/java/com/companion/chat/companion/turn/DefaultCompanionTurnModule.kt`
- `app/src/main/java/com/companion/chat/engine/LlamaCppInferenceEngine.kt`
- `app/src/main/java/com/companion/chat/engine/LiteRTLMInferenceEngine.kt`

Problem:

Callers know too much about package paths, readiness, adapter choice,
initialization, rebuild, replay, warmup, cancellation, release, and fallback
semantics. The llama.cpp and LiteRT adapters differ enough that those differences
shape callers.

Risk:

Model Runtime Lifecycle bugs require tracing through settings, readiness,
runtime switching, context rebuild, and individual adapters.

Deepening direction:

Move lifecycle policy into a deeper Model Runtime Lifecycle module. Keep llama.cpp
and LiteRT as adapters behind that seam.

Expected leverage:

- Locality: fallback and rebuild policy centralize.
- Leverage: readiness and initialization can be tested through one interface.
- Backend variance becomes explicit without leaking to Companion Turn callers.

### 4. Deepen the Privacy Gate for remote adapters

Recommendation strength: Worth exploring

Relevant modules:

- `app/src/main/java/com/companion/chat/data/privacy/PrivacySettingsRepository.kt`
- `app/src/main/java/com/companion/chat/ui/settings/ProfileViewModel.kt`
- `app/src/main/java/com/companion/chat/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/companion/chat/ui/chat/ChatViewModel.kt`
- `app/src/main/java/com/companion/chat/engine/AndroidVoiceInputEngine.kt`
- `app/src/main/java/com/companion/chat/engine/CloudHttpAsrEngine.kt`
- `app/src/main/java/com/companion/chat/engine/HttpVoiceCloneEngine.kt`
- `app/src/main/java/com/companion/chat/engine/image/HttpImageGenerationEngine.kt`

Problem:

User-controlled boundaries are visible in settings, but remote adapters mostly
enforce endpoint shape and configuration. Privacy authorization is not a shared
seam that every remote adapter must cross.

Risk:

Local-first policy can become a UI label rather than an enforced module rule.
Cloud ASR, HTTP voice clone, and HTTP image generation can drift in how they
interpret local-only mode and explicit opt-in.

Deepening direction:

Create a Privacy Gate module that each cloud-substitutable adapter crosses before
sending data off-device. The interface should evaluate:

- data type
- destination
- reason
- local alternative
- configured user boundary
- allow or deny decision
- user-readable denial reason

Expected leverage:

- Locality: user boundaries centralize.
- Leverage: adapters share one enforceable gate.
- Privacy-denial behavior becomes directly testable.

## Top Recommendation

Start with the Companion Turn transaction seam.

That module already sits on the daily Anime Companion loop. Deepening it gives
the broadest locality gain because streaming persistence, voice-first playback,
timeline events, Durable Memory refresh, and Preference Learning all cross that
seam today.

## Suggested Next Questions

Before implementing the top recommendation, settle these design questions:

- What is the smallest interface for a Companion Turn transaction?
- Which side effects are required outcomes, and which are optional adapters?
- Should voice playback be requested after persistence commits instead of being
  awaited inside finalization?
- Should timeline events be emitted by the Companion Turn module or by a separate
  timeline adapter behind the same seam?
- What should happen to a partially streamed assistant message after cancellation
  or process death?

## Notes

No code changes were made during the review. No tests were run because the task
was documentation and architecture analysis only.
