# Frontend Backend Gaps

This document tracks backend/data gaps exposed by the imagegen-led UI rewrite.
The UI is intentionally implemented first so product review can happen before
the missing runtime surfaces are built.

## Scope

Implemented frontend screens:

- Home
- Chat / Voice
- Memory & Relationship
- Helmet Control & Diagnostics
- Profile / Privacy / Plan

Existing ViewModels and navigation callbacks are still wired where they existed.
Sections below marked "placeholder" are visual-only until a backend surface is
added.

## Home

Current data wired:

- Active/recommended companion cards use `DiscoverViewModel`.
- Model/voice/image readiness uses `CompanionReadinessRepository`.
- Start chat, helmet, memory, profile, and role-card actions use existing
  callbacks.

Backend gaps:

- Companion relationship level, XP, mood, and active percentage.
- Helmet battery, signal, firmware version, and online status.
- Ambient playback state and ambient audio library.
- Ride mode state and safety availability.
- Emergency SOS readiness and contact status.
- Recent memories with thumbnail/media metadata.
- Recent activity feed with typed events and timestamps.

## Chat / Voice

Current data wired:

- Messages, sessions, voice input, voice output, image generation, and drawer
  behavior still use `ChatViewModel`.
- Text/image input remains available through the existing `ChatInputBar`.

Backend gaps:

- Pinned memories injected into the current conversation.
- Conversation timeline event model.
- Helmet stream clips and location/media metadata.
- Voice-note transcript persistence and clipping.
- Per-session privacy mode selection.
- Voice personality settings: tone, language, verbosity.
- Assistant response audio waveform and duration metadata.

## Memory & Relationship

Current data wired:

- Memory CRUD, filters, promote, and editor still use `MemoryViewModel`.
- Confirmed memories come from the existing Room-backed `Memory` entity.

Backend gaps:

- Relationship profile: companion identity, level, XP, closeness label.
- Memory health metrics: accuracy, capacity, review backlog, confidence.
- Candidate memory review queue before commit.
- Keep/edit/delete/pin review actions.
- Pinned memory playback/use-next-turn actions.
- Relationship timeline event types and media thumbnails.
- Learned preference confirmation/disable workflow.
- Local storage health and sync status.

## Helmet Control & Diagnostics

Current data wired:

- LLM, ASR, TTS, and image readiness use `CompanionReadinessSnapshot`.
- Model and voice settings navigation callbacks remain wired.
- Real helmet telemetry and controls are out of scope until hardware is
  available. The current app should expose local device/model/voice diagnostics,
  keep Helmet as the product surface, and mark hardware controls as unavailable
  when no helmet is connected.

Backend gaps:

- Real helmet pairing state.
- Battery, charging, runtime, firmware, BLE signal, and changelog data.
- Temperature and sensor telemetry.
- Speaker, mic, ANC, passthrough, LED, and wake-word control APIs.
- Safety mode settings: ventilation, auto-shutoff, impact detection.
- Hardware health-check runner and progress reporting.
- Diagnostic log persistence and detail navigation.

## Profile / Privacy / Plan

Current data wired:

- Memory learning toggle uses `ContextConfigRepository`.
- Runtime readiness uses `CompanionReadinessRepository`.
- Existing navigation callbacks are preserved.

Backend gaps:

- User profile entity and avatar source.
- Subscription/plan entitlement state.
- Renewal dates, premium voice entitlement, warranty/cloud feature flags.
- Export memories/conversations/role cards.
- Delete local data flow with scoped confirmation.
- Emergency contact management and SOS test flow.
- Privacy controls for analytics, partner sharing, cloud ASR, HTTP voice clone,
  and HTTP image generation.
- Advanced diagnostic screens for endpoint templates and backend logs.

## Suggested Backend Order

1. Add typed UI state models for Home dashboard and Profile privacy settings.
2. Build candidate memory review queue on top of the existing memory pipeline.
3. Add helmet pairing/telemetry repository with mockable local implementation.
4. Add timeline/event feed shared by Home, Chat, and Memory.
5. Add export/delete local data workflows.
6. Add subscription/plan entitlement only after local-first flows are stable.
