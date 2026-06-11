# Frontend Backend Gaps

This document tracks backend/data gaps exposed by the product UI rewrite. The
latest `main` now backs several previously visual-only surfaces, so this file is
organized as implemented data wiring plus the runtime gaps that still remain.

## Scope

Implemented product screens:

- Home
- Chat / Voice
- Memory & Relationship
- Helmet Control & Diagnostics
- Profile / Privacy / Plan
- Onboarding / Setup entry point

Existing ViewModels and navigation callbacks are still wired where they existed.
The remaining gaps are mostly hardware runtime, richer media metadata, and real
cloud/account services.

## Home

Current data wired:

- Bottom navigation starts at `Screen.HOME`, with Home, Helmet, Memory, and
  Profile as first-class destinations.
- Active/recommended companion cards use `DiscoverViewModel`.
- `HomeDashboardRepository` builds typed `HomeDashboardUiState` from the active
  Role Card, `DurableMemoryModule`, `CompanionReadinessRepository`, and
  `TimelineEventRepository`.
- Relationship level, XP, and closeness label are approximated from confirmed
  memory count.
- Local model, voice, and image readiness drive quick-action enabled states,
  disabled reasons, and setup suggestions.
- Recent memories come from the Durable Memory confirmed projection.
- Recent activity uses typed local Timeline Events with relative timestamps.
- Start chat, helmet, memory, profile, and role-card actions use existing
  callbacks.

Remaining gaps:

- Real helmet battery, signal, firmware version, and online status.
- Relationship mood, active percentage, and XP sourced from real interaction
  signals instead of confirmed-memory count.
- Ambient playback state and ambient audio library.
- Ride mode state and safety availability.
- Emergency SOS runtime readiness, permission status, and contact test state.
- Recent memory thumbnail/media metadata beyond the current optional `mediaUri`.

## Chat / Voice

Current data wired:

- Messages, sessions, voice input, voice output, image generation, and drawer
  behavior use `ChatViewModel`.
- Text/image input remains available through the existing `ChatInputBar`.
- `CompanionTurnModule` now emits explicit turn outcomes for acceptance,
  streaming tokens, assistant-message commit, voice playback, timeline event
  requests, Durable Memory refresh, and Preference Learning trigger.
- Chat records typed Timeline Events for voice transcripts, image generation,
  user turns, assistant replies, and selected memories.
- Pinned memories are read from `DurableMemoryModule`.
- `Use Next Turn` passes selected confirmed memory IDs into the next Companion
  Turn for one-turn prompt injection, then clears the selection after acceptance.
- Chat reflects Profile privacy defaults as a local-only/cloud-optional label.
- Remote ASR, HTTP voice clone, and HTTP image generation are guarded by
  `PrivacyGate` at the adapter level.

Remaining gaps:

- Helmet stream clips and location/media metadata.
- Persisted voice-note audio clips, waveform, duration, and replay metadata.
- Per-session or per-capture privacy mode selection before capture starts.
- Voice personality settings in the chat workspace: tone, language, verbosity,
  and role voice mode.
- Assistant response audio waveform and duration metadata.
- Human-readable memory-source chips showing which memories affected a specific
  turn.

## Memory & Relationship

Current data wired:

- `MemoryViewModel` consumes `DurableMemoryModule` instead of reimplementing
  review rules.
- Confirmed memories come from the Room-backed `Memory` entity.
- Candidate review queue is exposed through `DurableMemoryReviewProjection`.
- Candidate actions support keep, delete, and pin.
- Confirmed memory actions support add, edit, delete, promote, pin, unpin, and
  `Use Next Turn`.
- Pinned memories are exposed as a first-class projection for Memory, Chat, and
  Home.
- Memory health metrics exist for total, pinned, candidates, long-term, and
  short-term counts.

Remaining gaps:

- Relationship profile beyond count-derived continuity: companion identity
  history, emotional state, level rules, and XP rules.
- Memory health metrics for accuracy, capacity, source confidence, and stale
  memory risk.
- Rich source metadata for candidate memories, including extraction reason,
  confidence label, originating turn, voice clip, and media thumbnail.
- Pinned memory playback when the source is voice or media.
- Learned preference confirmation, edit, and disable workflow.
- Local storage health, optional cloud backup, and sync status.

## Helmet Control & Diagnostics

Current data wired:

- LLM, ASR, TTS, and image readiness use `CompanionReadinessSnapshot`.
- Model and voice settings navigation callbacks remain wired.
- Real helmet telemetry and controls are out of scope until hardware is
  available. The current app exposes local device/model/voice diagnostics, keeps
  Helmet as the product surface, and marks hardware controls as unavailable when
  no helmet is connected.

Remaining gaps:

- Real helmet pairing state.
- Battery, charging, runtime, firmware, BLE signal, and changelog data.
- Temperature and sensor telemetry.
- Speaker, mic, ANC, passthrough, LED, and wake-word control APIs.
- Safety mode settings: ventilation, auto-shutoff, impact detection.
- Hardware health-check runner and progress reporting.
- Diagnostic log persistence and detail navigation.

## Profile / Privacy / Plan

Current data wired:

- `ProfileViewModel` combines local profile, plan state, privacy settings,
  runtime readiness, export/delete status, and deletion confirmation state.
- `UserProfileRepository` stores display name, avatar URI, and emergency contact
  details in SharedPreferences.
- `PrivacySettingsRepository` stores local-only mode plus separate opt-ins for
  Cloud ASR, HTTP voice clone, HTTP image generation, analytics, and partner
  sharing.
- Local-only mode normalizes cloud, analytics, and sharing opt-ins off.
- `PrivacyGate` is wired through `AppContainer` and enforced by Cloud ASR, HTTP
  voice clone, and HTTP image generation before data leaves the device.
- `DataExportRepository` exports conversations, memories, role cards,
  preferences, and Timeline Events to app-private JSON.
- Scoped local delete supports memories, conversations, role cards, and all local
  user data.
- Profile privacy changes, local export, local delete, and emergency contact
  updates create Timeline Events.
- Runtime readiness uses `CompanionReadinessRepository`.

Remaining gaps:

- Real account and subscription/plan entitlement backend.
- Renewal dates, premium voice entitlement, warranty state, and cloud feature
  flags.
- Emergency SOS runtime, contact test flow, and impact-detection notification
  rules.
- Analytics, partner sharing, diagnostic upload, and cloud backup adapters.
- Pre-action confirmation UI for any capture/upload that can leave the device.
- Advanced diagnostic screens for endpoint templates, backend logs, and remote
  request history.

## Suggested Backend Order

1. Add a helmet pairing/telemetry repository with a mockable local
   implementation, then replace placeholder helmet metrics.
2. Add voice clip persistence with waveform, duration, source-turn linkage, and
   replay metadata.
3. Add per-capture privacy selection/confirmation UI that feeds the existing
   `PrivacyGate` policy.
4. Add richer memory provenance: source confidence, extraction reason, affected
   turn chips, and media thumbnails.
5. Add learned preference confirmation/edit/disable workflow.
6. Add subscription entitlement, cloud backup, and SOS runtime only after the
   local-first flows stay coherent.
