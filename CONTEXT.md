# CompanionChat Context

CompanionChat is a local-first Anime Companion application. This context names the product concepts that shape companion conversation, memory, role identity, and voice-first interaction.

## Language

**Anime Companion**:
A private local AI companion with durable identity, memory, and voice interaction. It is the product experience, not a generic assistant.
_Avoid_: Chatbot, assistant app

**Companion Turn**:
One user-initiated exchange with the Anime Companion, including input capture, durable memory preparation, model generation, session persistence, optional voice playback, and preference learning.
_Avoid_: Chat request, generation call

**Model Runtime Lifecycle**:
The local model state needed for companion turns: runtime selection, model package resolution, initialization, backend fallback, cancellation, and release.
_Avoid_: Engine setup, model plumbing

**Role Card**:
The active companion identity: persona, speaking style, background, rules, media, and voice profile used to shape companion turns.
_Avoid_: Character config, persona preset

**Skill**:
A task-oriented behavior mode that can be active alongside a Role Card.
_Avoid_: Tool, command mode

**Durable Memory**:
User-owned context that persists across companion turns and is injected when relevant to preserve relationship continuity.
_Avoid_: Chat history, notes

**Privacy Gate**:
The shared decision module that evaluates whether data may leave the device for a remote adapter, based on data type, destination, reason, local alternative, and the user's privacy settings.
_Avoid_: Cloud toggle, endpoint check

**Timeline Event**:
A typed local record of meaningful product activity, such as companion turns, voice notes, image generation, memory use, setup changes, privacy changes, data export, or local deletion.
_Avoid_: Log line, UI feed item

**Preference Learning**:
Background extraction and confirmation of stable user preferences from prior companion turns.
_Avoid_: Summary job, analytics

**Voice-First Interaction**:
The primary interaction path where speech input can trigger a companion turn and speech output can play the assistant response.
_Avoid_: Voice feature
