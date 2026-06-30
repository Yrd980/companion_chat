# CompanionChat Context

CompanionChat is the Companion Cockpit for a local-first Anime Companion product system. This context names the product concepts that shape helmet-first companion interaction, memory, role identity, privacy, and voice-first use.

## Language

**Anime Companion**:
A helmet-first local AI companion system with durable identity, memory, and private voice interaction. It is the whole product experience across wearable presence and companion management, not a generic assistant.
_Avoid_: Chatbot, assistant app, helmet optional app

**AI Smart Helmet**:
The wearable product that gives the Anime Companion private presence through head-worn interaction, audio, sensing, and future spatial context. It is the live interaction surface, not a passive accessory.
_Avoid_: Peripheral, accessory, demo hardware

**Companion Cockpit**:
The phone-side control surface for the Anime Companion, where the user manages role identity, durable memory, privacy boundaries, model packages, and device readiness. It preserves user control without replacing helmet presence.
_Avoid_: Mobile frontend, settings app, non-helmet app

**Helmet Presence**:
The feeling that the Anime Companion is privately with the user through the AI Smart Helmet's worn state, voice channel, physical controls, sensing, and contextual awareness.
_Avoid_: Hardware feature, connected device state

**Helmet Companion Loop**:
The core product loop where the user prepares through the Companion Hub, enters a Companion Mode, interacts through helmet-first voice, reviews or confirms resulting memory, and returns to a more continuous future Companion Session.
_Avoid_: Chat flow, onboarding funnel, feature walkthrough

**Companion Hub**:
The daily cockpit surface that shows the active companion, AI Smart Helmet readiness, current companion mode, available actions, and memory continuity.
_Avoid_: Home page, dashboard, launcher

**Companion Session**:
An intentional period of being with the Anime Companion, usually shaped by voice-first interaction, helmet state, role identity, memory, and the selected mode. It is the canonical user-facing concept for what older surfaces may have called chat.
_Avoid_: Chat session, conversation tab, generation session, chat room

**Companion Turn**:
One user-initiated exchange with the Anime Companion, including input capture, durable memory preparation, model generation, session persistence, optional voice playback, and preference learning.
_Avoid_: Chat request, generation call

**Session Message**:
A single user, companion, or system message inside a Companion Session.
_Avoid_: Chat message

**Companion Mode**:
A named posture for a Companion Session that shapes interruption level, voice behavior, memory use, safety boundaries, and role behavior for a real-life context. A Companion Mode answers how the Anime Companion is with the user right now.
_Avoid_: Prompt preset, feature toggle, chat mode

**Daily Companion Mode**:
The default Companion Mode for open-ended presence, light conversation, role continuity, and ordinary memory use.
_Avoid_: General chat, default prompt

**Commute Companion Mode**:
A low-interruption Companion Mode for being with the user while moving through daily transit or public space.
_Avoid_: Navigation mode, driving assistant

**Focus Companion Mode**:
A restrained Companion Mode that helps the user stay with a task through quiet presence and occasional lightweight check-ins.
_Avoid_: Productivity assistant, timer

**Sleep Companion Mode**:
A low-stimulation Companion Mode for winding down through calm voice, slower pacing, and gentle memory continuity.
_Avoid_: Meditation app, sleep tracker

**Emotional Companion Mode**:
A high-empathy Companion Mode that prioritizes emotional reception and continuity over advice, productivity, or task completion.
_Avoid_: Therapy mode, crisis service

**Model Runtime Lifecycle**:
The local model state needed for companion turns: runtime selection, model package resolution, initialization, backend fallback, cancellation, and release.
_Avoid_: Engine setup, model plumbing

**Model Package**:
A user-configurable local or explicitly remote capability package that powers companion language, speech, voice, image, or perception while preserving Role Card identity and Durable Memory continuity.
_Avoid_: Model backend, AI provider, character brain

**Role Card**:
The active companion identity: persona, speaking style, background, rules, media, and voice profile used to shape companion turns.
_Avoid_: Character config, persona preset

**Skill**:
A task-oriented behavior mode that can be active alongside a Role Card. A Skill answers what task is being done, not who is present or how the Companion Session feels.
_Avoid_: Tool, command mode

**Durable Memory**:
User-owned context that persists across companion turns and is injected when relevant to preserve relationship continuity.
_Avoid_: Chat history, notes

**User Memory**:
Durable Memory about the user that may apply across Role Cards when the user allows it. Sensitive User Memory requires explicit user control before broad use.
_Avoid_: Global prompt, shared profile facts

**Role Memory**:
Durable Memory scoped to the relationship between the user and one Role Card. It is private to that role relationship unless the user promotes it.
_Avoid_: Character notes, private chat history

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
