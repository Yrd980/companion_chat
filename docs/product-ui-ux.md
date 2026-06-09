# CompanionChat Product UI/UX Direction

## Design Read

Reading this as: a local-first mobile companion product for an AI Smart Helmet, where the phone is the companion hub and the helmet is the private wearable interaction layer. The UI should feel like a companion-device control app, not a generic chatbot, model demo, marketplace, or settings app.

Design dials:

- Variance: 4/10. Familiar mobile product structure with compact card modules.
- Motion: 4/10. Quiet state changes, tactile controls, and subtle voice/session feedback.
- Density: 7/10. Scannable cockpit density for companion, helmet, memory, safety, and diagnostics.

Assumptions from the reference design:

- The reference design anchors four visible screens: `Onboarding & Device Setup`, `Home / Companion Hub`, `Companion Chat & Voice`, and `Helmet Control & Diagnostics`.
- The complete product loop also needs `Memory & Relationship` and `Profile, Privacy & Plan` so conversation output, privacy, plan, data ownership, and emergency setup have clear homes.
- Account and plan surfaces may appear in onboarding, but the product remains local-first. Remote account, cloud endpoints, analytics, and partner sharing must stay explicit opt-in.
- The bottom navigation labels can evolve, but their roles should map to `Chat`, `Helmet`, `Memory`, and `Profile`. Contacts, shared clips, and market/discovery can live inside Memory or Profile instead of replacing Memory.

## Product Shape

CompanionChat should feel like one integrated smart-helmet companion product with six surfaces:

1. **Onboarding & Device Setup**
   Create or sign in to a profile, verify and pair the AI Smart Helmet, grant permissions, choose a plan if needed, and update firmware before daily use.
2. **Home / Companion Hub**
   The default home. Shows the active companion, helmet readiness, session mode, primary actions, memories, suggestions, and recent activity.
3. **Companion Chat & Voice**
   A conversation workspace with pinned memories, voice notes, replay timeline, media/action cards, push-to-talk, privacy mode, and voice personality controls.
4. **Helmet Control & Diagnostics**
   A transparent control surface for battery, connection, firmware, sensors, audio, safety modes, health checks, and recent logs.
5. **Memory & Relationship**
   The user-owned record of important moments, preferences, voice clips, pinned memories, and relationship events that can be inspected, edited, replayed, or injected into a future turn.
6. **Profile, Privacy & Plan**
   Account/local profile, plan status, privacy controls, cloud endpoints, data export/delete, emergency contacts, and advanced settings.

The current app already has the raw capability areas. The redesign should reorganize them into this product hierarchy instead of exposing them as separate engineering features.

## Product Closed Loop

The product loop should be complete, not a set of disconnected screens:

1. **Setup**
   The user creates a local profile or account, pairs the helmet, grants permissions, chooses plan boundaries, validates local model packages, and updates firmware.
   Outcome: Home receives a companion, a paired helmet state, privacy defaults, and readiness status.

2. **Prepare**
   Home shows the active companion, helmet readiness, current mode, memories, suggestions, and recent activity.
   Outcome: the user can start chat, listen live, ride mode, ambience, new memory capture, or SOS from one surface.

3. **Interact**
   Chat & Voice handles text, push-to-talk, live helmet stream, assistant replies, media cards, timeline markers, and privacy mode.
   Outcome: useful moments become candidate memories, clips, actions, or safety events.

4. **Confirm Memory**
   Memory & Relationship lets the user review what was saved, promote or delete extracted memories, replay voice clips, pin important moments, and see which memories affected a turn.
   Outcome: confirmed memories return to Home and Chat as visible continuity, not hidden prompt state.

5. **Control Device**
   Helmet Control & Diagnostics handles battery, sensors, firmware, audio controls, safety modes, health checks, and logs.
   Outcome: healthy/degraded device state returns to Home, Chat, Ride Mode, and SOS availability.

6. **Set Boundaries**
   Profile, Privacy & Plan controls local-only/cloud mode, analytics, partner sharing, account, plan, data export/delete, emergency contacts, and advanced endpoints.
   Outcome: privacy and entitlement rules shape the next capture, generation, sharing, and diagnostics flow.

7. **Return**
   Home reflects the updated companion state, memory continuity, helmet health, and available actions.
   Outcome: the app feels like a durable companion-device loop rather than separate setup, chat, and settings islands.

Closed-loop rules:

- Every action that creates user-owned context must have a review or undo path.
- Every device problem must surface a repair path and flow back to Home readiness.
- Every cloud or sharing decision must be visible before data leaves the device.
- Every premium or plan-gated feature must explain the local alternative when one exists.
- Home is the loop anchor. Memory, Helmet, and Profile surfaces should return meaningful state to Home.

## Shared UI System

The interface should be light, quiet, and device-like:

- White or near-white canvas.
- White cards with thin soft-gray borders.
- One green accent for primary actions, healthy states, selected modes, active toggles, and session-live indicators.
- Pale green fills for selected cards, paired devices, active chips, consent highlights, and successful readiness.
- Charcoal text for primary content and muted olive-gray text for metadata.
- Red only for emergency, destructive, or severe safety actions.
- Rounded rectangular panels with 8-12dp radius.
- Compact cards with clear thumbnail, title, metadata, and one or two actions.
- Small status pills for readiness, connection, battery, firmware, privacy, and session state.
- Familiar mobile controls: chips for modes, toggles for binary settings, sliders for audio controls, progress bars for firmware and diagnostics, and icon buttons for quick actions.

Avoid anime-fan-site visual language. The app can contain anime character artwork, but the product shell should feel like a reliable wearable companion device.

## Color Palette

- **Canvas White** `#FBFCF8`: primary app background.
- **Panel White** `#FFFFFF`: cards, controls, elevated panels.
- **Soft Field** `#F2F6EF`: low-emphasis green-tinted surfaces.
- **Line Gray** `#E5E8E0`: dividers and card borders.
- **Charcoal Ink** `#171A15`: primary text.
- **Muted Olive Gray** `#687061`: secondary text and metadata.
- **Companion Green** `#43A51B`: primary action, active state, session live, selected mode.
- **Green Soft Fill** `#DFF3D2`: active pills, paired device cards, positive status backgrounds.
- **Alert Red** `#F04452`: Emergency SOS, severe impact, destructive actions.
- **Warning Amber** `#D99620`: degraded readiness, pending setup, firmware warnings.
- **Neutral Black** `#050505`: limited to platform sign-in, high-contrast secondary account action, or system-level affordance.

Rules:

- Use green as the only product accent.
- Do not use purple/blue AI gradients.
- Do not use neon glows.
- Do not let dynamic system color override the brand palette on key product screens.
- Emergency red must never be reused for normal errors or decorative emphasis.

## Typography

Use Material typography, but tune hierarchy toward product utility:

- Screen title: 20-22sp, semibold.
- Companion name: 22-26sp, semibold.
- Device metric title: 20-24sp, semibold.
- Section title: 15-16sp, semibold.
- Body: 14-16sp, regular.
- Metadata and status: 11-12sp, medium.
- Numeric values: use tabular or monospace styling where possible for battery, runtime, firmware, signal, sensor values, and timestamps.

Avoid large hero typography inside the app. This is a daily-use product, not a marketing page.

## Navigation Model

After onboarding, use bottom navigation focused on the daily product loop:

- **Chat**: default route. Opens Home / Companion Hub and gives fast entry into companion chat.
- **Helmet**: helmet control, safety modes, connection, firmware, and diagnostics.
- **Memory**: inspect memories, relationship events, role cards, friends, shared clips, or companion discovery.
- **Profile**: account, plan, privacy, data export/delete, and advanced settings.

Product rules:

- Do not make `Contacts`, `Market`, or role discovery the default entry. The product should open into the active relationship.
- Do not bury helmet readiness inside generic settings. Helmet state is a primary product surface.
- Memory remains visible inside Home and Chat. It can have a deeper management view, but it should not disappear behind a settings route.

## Screen 1: Onboarding & Device Setup

Purpose:

Get the user from first launch to a paired, permissioned, firmware-ready AI Smart Helmet with explicit privacy and plan boundaries.

Layout:

1. **App identity and progress**
   - App icon/name: `Anime Companion`.
   - Step progress such as `Step 2 of 7`.
   - Short setup context.

2. **Welcome and account**
   - Welcome headline for CompanionChat.
   - Primary action: `Create Account` or `Create Local Profile`.
   - Secondary account action when sign-in is required.
   - Google and Apple sign-in as optional account providers.
   - If local-only mode is available, show it with the same visual weight as other privacy-preserving paths.

3. **Verify & Pair Helmet**
   - Verification code input.
   - `Resend` action.
   - AI Smart Helmet pairing card with product thumbnail.
   - `Scan QR` primary action.
   - Manual 6-digit pairing code input.
   - `Pair` action.
   - Helper copy explaining where the one-time code lives.

4. **Privacy & Permissions**
   - Voice Recording toggle.
   - Usage Analytics toggle.
   - Share anonymized data with partners toggle.
   - Explicit voice-recording consent text.
   - `I Consent` primary action and `Decline` secondary action.

5. **Choose a Plan**
   - Trial, Standard, and Premium cards.
   - Selected plan uses pale green fill and green border.
   - Plan choice must not block local-only core use unless the product explicitly requires cloud or warranty services.

6. **Firmware Update**
   - Firmware package thumbnail.
   - Required/optional label.
   - Available version.
   - Short changelog.
   - Progress bar when update is running.
   - `Update Now` primary action and `Later` secondary action.

Behavior:

- Each setup step must show `Ready`, `Required`, `Optional`, `Skipped`, or `Needs attention`.
- Missing local model packages should never crash the app. Show path, missing files, and fix action.
- Helmet pairing failure should keep the manual code path visible.
- Camera permission denial should fall back to manual pairing code.
- Firmware update failure should preserve paired state and offer retry.
- Cloud endpoints, analytics, partner sharing, and remote accounts must be explicit opt-in.

## Screen 2: Home / Companion Hub

Purpose:

The daily home screen. It should answer: who is with me, is my helmet ready, what mode am I in, what can I do now, and what did we recently remember?

Layout:

1. **Top app bar**
   - App icon/name: `CompanionChat`.
   - Subtitle: `Anime Companion`.
   - Compact action: `Try voice wake word` or repair action.

2. **Companion identity card**
   - Avatar.
   - Companion name, such as `Aiko Hoshizora`.
   - Short state line, such as `Curious - Cheerful`.
   - Mood chip, such as `Mood Bright`.
   - Active state and level, such as `Active Lv. 8`.

3. **Helmet readiness strip**
   - Battery percentage.
   - Signal strength.
   - Firmware version.
   - Keep this strip near the top so device readiness is always visible.

4. **Mode chips**
   - `Idle`.
   - `Active`.
   - `Driving`.
   - `Sleep-safe`.
   - Selected mode uses green fill. Unselected modes use white fill and gray border.

5. **Primary action grid**
   - `Start Chat`: dominant green filled card.
   - `Listen Live`: white outlined card.
   - `Play Ambient`: white card.
   - `Start Ride Mode`: white card.
   - `New Memory`: white card.
   - `Emergency SOS`: large red card with clear iconography.

6. **Memory cards**
   - Horizontal cards with image thumbnail.
   - Title, source/time, and short summary.
   - Examples: `Seaside Memory`, `Midnight Piano`, `Umbrella Joke`.
   - Actions such as `Play`, `View`, `Pin`, or `Clip saved`.

7. **Recommended**
   - Small companion or mode cards.
   - This area can suggest companions, routines, or stories, but must not become the dominant marketplace surface.

8. **Suggestions**
   - Compact 2-column cards for routine actions.
   - Examples: morning ambience, commute ride mode, wind-down ambient lullaby, voice wake-word trial.

9. **Recent activity**
   - Saved memory.
   - Live listen started.
   - Clip added to favorites.
   - Firmware or model fallback happened.
   - Each row includes one small action such as `View`, `Open`, or `Play`.

States:

- No companion: show "Choose or create a companion" plus one primary action.
- Helmet not paired: keep Home usable, but replace helmet metrics with a pairing repair card.
- Firmware required: keep chat available only if safe, and surface update status near the top.
- Low battery: degrade long-running voice and ride actions before the user starts them.
- Voice not ready: show `Listen Live` disabled with inline reason.
- Emergency unavailable: explain the missing permission or contact setup.

## Screen 3: Companion Chat & Voice

Purpose:

A rich conversation workspace, not just a message list. Chat should be multimodal, voice-first, timeline-aware, and memory-aware.

Top area:

- App/companion identity.
- Helmet connection pill, such as `Helmet: connected`.
- Session state pill, such as `Session Live`.
- Listening, speaking, and idle states must be visible as primary status, not hidden inside one microphone icon.

Pinned memories:

- Horizontal row above the conversation.
- `Manage` action at the section level.
- Each card includes avatar/thumbnail, title, date, `Play`, and pin state.
- Pinned memories can be replayed, injected, pinned/unpinned, or managed.

Conversation timeline:

- Compact strip showing total duration, markers, replay points, or session segments.
- Markers can be tapped to replay relevant voice moments.
- `Open Timeline` reveals deeper session replay.
- Useful for voice-first sessions, memory replay, and clip creation.

Message cards:

- Assistant messages can include:
  - Avatar, name, timestamp.
  - Text response.
  - Audio or waveform preview.
  - Media thumbnail.
  - `Play`, `More`, `Loop`, `Save`, `Share`, and `Save to Memories`.
  - Pinned/reaction counts.
- User voice note cards should show:
  - Audio length.
  - Transcript.
  - Sent state.
  - `Save to Memories`.
  - `Clip & Share`.
  - Privacy indicator: cloud or local-only.
- Helmet action cards can include:
  - Requested helmet action.
  - Send-to-friend or contact action.
  - Reaction chips.

Input dock:

- Large green push-to-talk button.
- Helmet Stream status.
- Continuous streaming indicator when active.
- Privacy selector: cloud or local-only when relevant.
- `Clip & Share`.
- `Save`.
- Text input and image attachment remain available as secondary controls.

Voice personality panel:

- Tone.
- Language.
- Verbosity.
- Role voice mode.
- `Edit` action.

Behavior:

- Streaming assistant text remains supported.
- Voice should feel first-class, not an extra icon.
- Save-to-memory should be one tap from any important exchange.
- Voice sessions should support idle, listening, processing, speaking, interrupted, disconnected, and permission-denied states.
- Cloud mode should be visible before capture starts, not after audio has already been sent.

## Screen 4: Helmet Control & Diagnostics

Purpose:

Make helmet health, safety, audio behavior, firmware, and diagnostics visible without turning the screen into an engineering settings dump.

Layout:

1. **App bar**
   - Back action.
   - Title: `Helmet Control & Diagnostics`.
   - Overflow or info action only if needed.

2. **Battery and connection summary**
   - Battery percentage as the dominant metric.
   - Charging state.
   - Estimated runtime.
   - Product thumbnail.
   - Connection status, such as `BLE - Strong`.
   - Firmware version.
   - Recent firmware changelog.

3. **Environment and sensor cards**
   - Temperature card.
   - Sensors card with accelerometer, gyroscope, proximity, and microphone status.
   - Healthy values use green dots and short labels such as `OK` or `Good`.

4. **Controls**
   - Speaker Volume slider.
   - Mic Sensitivity value or segmented control.
   - Active Noise Cancellation toggle.
   - Ambient Passthrough toggle or row.
   - LED Personalization row with current color/pattern.
   - Voice Wake-Word Sensitivity row.

5. **Safety modes**
   - Ventilation Reminders.
   - Auto-Shutoff.
   - Impact Detection.
   - Each row includes short purpose copy and edit/toggle action.

6. **Diagnostics**
   - Last check time.
   - `Run Health Check` primary action.
   - Health check progress bar and current step.
   - Recent logs with severity icon, timestamp, summary, recommended action, and optional update/retry action.

Advanced settings:

- Move engineering controls behind disclosure:
  - GGUF path.
  - LiteRT backend.
  - Context size.
  - Max tokens.
  - Temperature.
  - Top K/top P.
  - HTTP request templates.
  - Vulkan.
  - Cloud ASR endpoint.
  - HTTP voice clone endpoint.
  - HTTP image generation endpoint.

States:

- Disconnected: show repair actions before any controls.
- Charging: show charging state and runtime estimate.
- Firmware update available: show version and changelog before update action.
- Sensor degraded: identify the sensor and recommended calibration or reboot.
- Microphone sensitivity issue: offer calibration or reboot.
- Health check running: progress must show the current check step.
- Cloud endpoint blocked: explain that local-only privacy mode is active.

## Role Card UX

Role Card is not a preset. It is the companion identity.

Role editor sections:

- Basics: name, short intro, avatar.
- Identity: persona, style, background.
- Boundaries: rules and taboos.
- Voice: mode, reference audio, display name.
- Visual: avatar, gallery, image style.
- Opening: first message and sample dialogue.

Product rules:

- The active Role Card should be visible on Home / Companion Hub.
- Starting a role conversation should feel like entering that role's room.
- Editing role voice and image should preview the result where possible.
- Contacts or Market can help discover/import companions as secondary flows, but the active companion relationship remains the center of the app.

## Memory UX

Memory is a product feature, not hidden model context.

Purpose:

Close the loop between conversation and long-term companionship. The user should be able to see what the app remembered, why it matters, where it came from, and whether it should affect future turns.

Required views:

- Pinned memories.
- Conversation memories.
- Voice clips.
- Short-term memories.
- Long-term memories.
- Preferences.
- Relationship events.

Layout:

1. **Relationship summary**
   - Active companion avatar/name.
   - Relationship level or continuity metric.
   - Memory health: how many pinned memories, preferences, voice clips, and recent events are active.
   - Last memory update time.

2. **Review queue**
   - Candidate memories extracted from recent chat or voice sessions.
   - Each candidate shows source, confidence label, and short reason.
   - Actions: `Keep`, `Edit`, `Delete`, `Pin`.

3. **Pinned memories**
   - Horizontal cards matching Home and Chat memory cards.
   - Playable when the source is voice or media.
   - Actions: `Use Next Turn`, `Unpin`, `Edit`.

4. **Timeline**
   - Relationship events grouped by date.
   - Voice clips, saved media, ride moments, role changes, and important conversation markers.
   - Each item can open the source turn without exposing raw prompt internals.

5. **Preference chips**
   - Confirmed preferences.
   - Unconfirmed learned preferences.
   - User can approve, edit, or disable each preference.

Memory card:

- Thumbnail or avatar when available.
- Content.
- Category.
- Layer.
- Source: manual, rule, model extracted, voice clip, or session marker.
- Last updated.
- Actions: play, edit, delete, pin, promote, use in next turn, clip, share.

Memory injection:

- The user should be able to see which memories affected a turn.
- Do not expose raw prompt internals by default. Show human-readable memory chips.
- Pinned memories should be playable and reusable from Chat, not only editable in a management list.

States:

- Empty: show why memory matters and one action to save the first memory.
- Review needed: show candidate memories before they silently affect relationship state.
- Local-only: show that memory is stored on device.
- Sync/cloud blocked: explain which optional sharing or backup action is unavailable.
- Delete pending: use a confirm state for destructive memory actions.

## Privacy & Boundaries

Privacy controls should appear where the decision happens:

- Voice capture consent appears during onboarding.
- Cloud/local privacy selector appears in the Chat input dock when voice or media capture can leave the device.
- Partner sharing and analytics are explicit onboarding toggles and remain editable in Profile.
- Helmet sensors and microphone permissions show repair actions near device controls.
- Data export/delete lives in Profile, not in the chat surface.

Rules:

- Cloud ASR, HTTP voice clone, HTTP image generation, analytics, and partner sharing are opt-in.
- Local-only mode must be visible before capture starts.
- Declining optional consent must not block core local companion use unless a feature truly depends on it.
- Do not expose raw prompt internals as a privacy substitute. Show user-readable memory, sensor, and cloud-use state.

## Profile, Privacy & Plan UX

Purpose:

Give account, plan, privacy, cloud, emergency, and advanced configuration a stable home without letting those controls dominate the daily companion surfaces.

Layout:

1. **Profile summary**
   - Local profile or signed-in account state.
   - Plan status.
   - Active companion and paired helmet summary.
   - Local-first status label.

2. **Plan and entitlement**
   - Current plan.
   - Trial/Premium renewal state if applicable.
   - Feature availability for cloud backup, partner sharing, premium voices, or warranty services.
   - Local alternatives for features that can work without cloud.

3. **Privacy controls**
   - Voice recording.
   - Usage analytics.
   - Partner sharing.
   - Cloud ASR.
   - HTTP voice clone.
   - HTTP image generation.
   - Memory learning.
   - Each toggle shows what data is affected.

4. **Data ownership**
   - Export memories.
   - Export conversations.
   - Export role cards.
   - Delete local data.
   - Delete cloud/account data when signed in.

5. **Emergency contacts**
   - Emergency contact setup.
   - SOS availability.
   - Impact detection notification rules.
   - Test contact action.

6. **Advanced**
   - Model package paths.
   - Cloud endpoint templates.
   - Backend diagnostics.
   - Developer/runtime controls.

Behavior:

- Profile must not be required for local-only first use unless account-dependent services are selected.
- Plan-gated states should explain what is disabled and which local path remains.
- Privacy toggles must update the relevant Home, Chat, Memory, and Helmet states.
- Data export/delete actions require confirmation and clear scope.

## Interaction States

Every screen must define:

- Empty.
- Loading.
- Ready.
- Degraded.
- Error.
- Permission denied.
- Helmet disconnected.
- Missing local model.
- Firmware required.
- Health check running.
- Cloud endpoint blocked.

No blank screens.
No generic "Error" messages.
Every failure needs a repair action.

## Motion & Feedback

Use motion sparingly:

- Button press: slight scale or tonal change.
- Mode chip selection: quick color transition.
- Voice listening: subtle breathing pulse on the push-to-talk button.
- Session live: quiet pulse or status shimmer.
- Streaming: typing indicator or waveform shimmer.
- Firmware update and health check: progress bar with clear status text.
- Pairing: short transition from scanning/manual code to connected state.

Do not animate layout size aggressively. Use opacity and transform.

## Copy Guidelines

Use product language:

- `陪伴`
- `角色卡`
- `记忆`
- `设备设置`
- `头盔控制`
- `健康检查`
- `本地模型`
- `语音待命`
- `本地模型就绪`
- `头盔已连接`
- `需要配置`
- `本地优先`
- `隐私模式`

Avoid engineering-first labels on primary surfaces:

- `GGUF`
- `LiteRT`
- `Top K`
- `Request Template`
- `Backend`
- `Runtime`
- `HTTP`
- `Vulkan`

These can appear in advanced settings only.

## Implementation Priorities

1. **Home / Companion Hub first**
   Replace the current chat-first default with a home screen that contains identity, helmet readiness, actions, memories, suggestions, and recent activity.

2. **Companion Chat & Voice second**
   Add pinned memories, replay timeline, message actions, voice notes, privacy state, and voice-first dock.

3. **Helmet Control & Diagnostics third**
   Keep current model and voice functionality, but reorganize the surface around helmet status, safety, controls, health checks, and repair actions.

4. **Memory & Relationship fourth**
   Add the review queue, pinned memories, timeline, preference chips, and memory-source visibility so Chat output can become confirmed durable memory.

5. **Profile, Privacy & Plan fifth**
   Add the stable home for privacy, plan, data ownership, emergency contacts, and advanced settings so boundaries are not scattered across daily surfaces.

6. **Onboarding & Device Setup sixth**
   Add guided setup for account/local profile, helmet pairing, permissions, plan selection, model readiness, and firmware update after the daily surfaces are coherent.

7. **Visual system**
   Lock palette, radius scale, spacing, status chips, card modules, icon treatment, and interaction states.

## Acceptance Checklist

- The original four reference screens keep their names and hierarchy.
- Memory & Relationship and Profile, Privacy & Plan complete the product loop beyond the reference screens.
- The complete loop exists: setup, prepare, interact, confirm memory, control device, set boundaries, and return to Home.
- The app opens into Home / Companion Hub, not a catalog.
- The active companion is visible above the fold.
- Helmet/device readiness is visible as a product surface, not buried in advanced settings.
- The user can tell whether helmet, local model, voice, memory, image generation, and privacy mode are ready.
- Chat, voice, memory save, helmet controls, SOS, and role switching are reachable from daily surfaces.
- Memories created from chat or voice can be reviewed, edited, pinned, deleted, replayed, and reused.
- Profile owns account, plan, privacy, data export/delete, emergency contacts, and advanced settings.
- Helmet diagnostics show battery, connection, firmware, sensors, controls, safety modes, health checks, and recent logs.
- Advanced model settings are discoverable but not visually dominant.
- Memory is inspectable, playable where relevant, and editable.
- Cloud usage is explicit before capture or generation.
- Missing models, missing permissions, firmware issues, and disconnected helmet states produce repair flows, not crashes.
- The UI uses one accent color consistently.
- Emergency red is reserved for SOS and severe safety actions.
- The primary experience feels like a smart companion helmet product, not a model demo.
