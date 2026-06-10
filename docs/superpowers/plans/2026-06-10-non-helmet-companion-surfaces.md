# Non-Helmet Companion Product Surfaces Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the remaining local-first companion product surfaces that do not require a physical helmet.

**Architecture:** Add small local repositories and typed UI state models around the existing Compose, Room, SharedPreferences, and ViewModel patterns. Keep real helmet hardware pairing, BLE telemetry, firmware, sensors, ANC, passthrough, LED, wake-word hardware controls, and impact detection out of scope because the user does not have a helmet. Any screen that currently shows helmet hardware data should either use local model/voice readiness, persisted demo-safe state, or copy that clearly says no device is connected.

**Tech Stack:** Android Kotlin, Jetpack Compose, Material 3, Room, SharedPreferences, Kotlin Flow/StateFlow, Gradle wrapper, existing `scripts/android-dev.bat`.

---

## Current State

- Branch: `codex/update-product-ui-ux-doc`
- Relevant current files:
  - `app/src/main/java/com/companion/chat/AppContainer.kt`
  - `app/src/main/java/com/companion/chat/AppViewModelFactory.kt`
  - `app/src/main/java/com/companion/chat/MainActivity.kt`
  - `app/src/main/java/com/companion/chat/data/local/CompanionDatabase.kt`
  - `app/src/main/java/com/companion/chat/data/local/dao/MemoryDao.kt`
  - `app/src/main/java/com/companion/chat/data/local/entity/Memory.kt`
  - `app/src/main/java/com/companion/chat/data/memory/MemoryRepository.kt`
  - `app/src/main/java/com/companion/chat/data/repository/ChatSessionRepository.kt`
  - `app/src/main/java/com/companion/chat/ui/chat/ChatViewModel.kt`
  - `app/src/main/java/com/companion/chat/ui/chat/ChatScreen.kt`
  - `app/src/main/java/com/companion/chat/ui/home/DiscoverViewModel.kt`
  - `app/src/main/java/com/companion/chat/ui/home/HomeScreen.kt`
  - `app/src/main/java/com/companion/chat/ui/memory/MemoryViewModel.kt`
  - `app/src/main/java/com/companion/chat/ui/memory/MemoryScreen.kt`
  - `app/src/main/java/com/companion/chat/ui/settings/SettingsScreen.kt`
  - `app/src/main/java/com/companion/chat/ui/navigation/AppNavigation.kt`
- Open GitHub issues at plan creation: none.
- Repo policy: do not add new test files unless the user explicitly asks. Verify with existing build/smoke commands and manual UI checks.

## Execution Rules

- Keep changes local-first and privacy-preserving.
- Do not add remote services, remote accounts, analytics, or plan enforcement as real dependencies.
- Do not implement real helmet hardware features.
- Do not use `git add -A` when agent worktrees contain unrelated changes.
- Main coordinator owns `CompanionDatabase.kt`, `AppContainer.kt`, `AppViewModelFactory.kt`, and `MainActivity.kt` when multiple agents are active.
- UI agents may propose changes to shared wiring files, but only the coordinator applies those shared edits.
- Run `.\gradlew.bat :app:compileDebugKotlin` after every agent merge.
- Run `.\scripts\android-dev.bat build` before the final commit/push.
- Use frequent commits with the commit messages listed in each task.

## Out Of Scope Because No Helmet Is Available

- BLE pairing and connection state backed by hardware.
- Battery, charging, runtime, firmware, BLE signal, temperature, and sensor telemetry from a device.
- Speaker, mic, ANC, passthrough, LED, wake-word sensitivity, ventilation, auto-shutoff, and impact detection APIs.
- Firmware update and physical health-check execution.
- Hardware diagnostic logs.

The UI may still contain a device diagnostics surface, but it must be backed by local model/voice/image readiness and explicit no-device copy.

## Target File Structure

Create these focused files:

- `app/src/main/java/com/companion/chat/data/dashboard/HomeDashboardModels.kt`
  - Home state models for relationship, quick actions, recent memories, recent activity, and local device readiness.
- `app/src/main/java/com/companion/chat/data/dashboard/HomeDashboardRepository.kt`
  - Reads existing role cards, memories, readiness, and timeline events to build Home state.
- `app/src/main/java/com/companion/chat/data/timeline/TimelineEvent.kt`
  - Room entity for local typed events.
- `app/src/main/java/com/companion/chat/data/local/dao/TimelineEventDao.kt`
  - Queries and mutations for event feed.
- `app/src/main/java/com/companion/chat/data/timeline/TimelineEventRepository.kt`
  - Local event creation and observation.
- `app/src/main/java/com/companion/chat/data/profile/UserProfileRepository.kt`
  - SharedPreferences-backed profile name/avatar/local identity settings.
- `app/src/main/java/com/companion/chat/data/privacy/PrivacySettingsRepository.kt`
  - SharedPreferences-backed privacy toggles with explicit opt-in defaults.
- `app/src/main/java/com/companion/chat/data/plan/PlanRepository.kt`
  - Local stub entitlement state for UI explanation only.
- `app/src/main/java/com/companion/chat/data/export/DataExportRepository.kt`
  - Export conversations, memories, and role cards into app-private files.
- `app/src/main/java/com/companion/chat/data/setup/SetupRepository.kt`
  - Non-helmet setup checklist for profile, permissions, model readiness, voice readiness, image readiness, privacy.
- `app/src/main/java/com/companion/chat/ui/home/HomeDashboardViewModel.kt`
  - Home dashboard state separate from role discovery.
- `app/src/main/java/com/companion/chat/ui/settings/ProfileViewModel.kt`
  - Profile, privacy, plan, export, delete-local-data, and emergency contact state.
- `app/src/main/java/com/companion/chat/ui/setup/OnboardingScreen.kt`
  - Local-first setup flow.
- `app/src/main/java/com/companion/chat/ui/setup/OnboardingViewModel.kt`
  - Setup state/actions.

Modify these existing files:

- `app/src/main/java/com/companion/chat/data/local/CompanionDatabase.kt`
  - Add `TimelineEvent` and `TimelineEventDao`, bump database version.
- `app/src/main/java/com/companion/chat/AppContainer.kt`
  - Register new repositories.
- `app/src/main/java/com/companion/chat/AppViewModelFactory.kt`
  - Register new ViewModels.
- `app/src/main/java/com/companion/chat/MainActivity.kt`
  - Wire HomeDashboardViewModel, ProfileViewModel, and optional onboarding route.
- `app/src/main/java/com/companion/chat/ui/home/HomeScreen.kt`
  - Replace visual-only home data with `HomeDashboardUiState`.
- `app/src/main/java/com/companion/chat/ui/chat/ChatViewModel.kt`
  - Add privacy state, pinned memory injection entry points, and timeline writes.
- `app/src/main/java/com/companion/chat/ui/chat/ChatScreen.kt`
  - Surface privacy mode, pinned memories, timeline, voice-note metadata.
- `app/src/main/java/com/companion/chat/ui/memory/MemoryUiState.kt`
  - Add review queue, health metrics, pinned memory state.
- `app/src/main/java/com/companion/chat/ui/memory/MemoryViewModel.kt`
  - Add candidate memory review and pin/use-next-turn actions.
- `app/src/main/java/com/companion/chat/ui/memory/MemoryScreen.kt`
  - Add review queue and pinned memory UI.
- `app/src/main/java/com/companion/chat/ui/settings/SettingsScreen.kt`
  - Replace local `remember` state with `ProfileViewModel`.
- `app/src/main/java/com/companion/chat/ui/navigation/AppNavigation.kt`
  - Add setup route if onboarding is included in this pass.

## Canonical Models

Use these signatures so agents do not invent incompatible shapes:

```kotlin
package com.companion.chat.data.timeline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timeline_events")
data class TimelineEvent(
    @PrimaryKey val id: String,
    val type: TimelineEventType,
    val title: String,
    val detail: String,
    val relatedSessionId: String? = null,
    val relatedMemoryId: Long? = null,
    val mediaUri: String? = null,
    val createdAt: Long
)

enum class TimelineEventType {
    CHAT,
    MEMORY_CREATED,
    MEMORY_PINNED,
    VOICE_NOTE,
    IMAGE_GENERATED,
    PRIVACY_CHANGED,
    SETUP_CHANGED,
    DATA_EXPORTED,
    LOCAL_DATA_DELETED
}
```

```kotlin
package com.companion.chat.data.dashboard

data class HomeDashboardUiState(
    val relationship: RelationshipSummary = RelationshipSummary(),
    val localDevice: LocalDeviceSummary = LocalDeviceSummary(),
    val quickActions: List<HomeQuickAction> = emptyList(),
    val recentMemories: List<HomeMemorySummary> = emptyList(),
    val recentActivity: List<HomeActivitySummary> = emptyList(),
    val suggestions: List<HomeSuggestion> = emptyList(),
    val isLoading: Boolean = true
)

data class RelationshipSummary(
    val companionName: String = "Aiko Hoshizora",
    val companionMood: String = "Bright",
    val level: Int = 1,
    val xp: Int = 0,
    val nextLevelXp: Int = 100,
    val closenessLabel: String = "New companion"
)

data class LocalDeviceSummary(
    val modelReady: Boolean = false,
    val voiceReady: Boolean = false,
    val imageReady: Boolean = false,
    val noHelmetMode: Boolean = true,
    val statusLabel: String = "Local companion mode"
)

data class HomeQuickAction(
    val id: String,
    val title: String,
    val subtitle: String,
    val enabled: Boolean = true,
    val disabledReason: String = ""
)

data class HomeMemorySummary(
    val id: Long,
    val title: String,
    val detail: String,
    val category: String,
    val mediaUri: String? = null
)

data class HomeActivitySummary(
    val id: String,
    val title: String,
    val detail: String,
    val timestampLabel: String
)

data class HomeSuggestion(
    val id: String,
    val text: String,
    val routeHint: String
)
```

```kotlin
package com.companion.chat.data.privacy

data class PrivacySettings(
    val localOnlyMode: Boolean = true,
    val allowCloudAsr: Boolean = false,
    val allowHttpVoiceClone: Boolean = false,
    val allowHttpImageGeneration: Boolean = false,
    val allowAnalytics: Boolean = false,
    val allowPartnerSharing: Boolean = false
)
```

## Agent Waves

### Wave 0: Coordinator Setup

**Agent:** Coordinator

**Files:**
- Read: `docs/frontend-backend-gaps.md`
- Read: `docs/product-ui-ux.md`
- Read: `app/src/main/java/com/companion/chat/AppContainer.kt`
- Read: `app/src/main/java/com/companion/chat/data/local/CompanionDatabase.kt`

- [ ] **Step 1: Confirm clean worktree**

Run:

```powershell
git status -sb
```

Expected: current branch is `codex/update-product-ui-ux-doc` and no local changes exist before starting.

- [ ] **Step 2: Create execution branch only if needed**

If already on `codex/update-product-ui-ux-doc`, stay there. If on `main` or `master`, create:

```powershell
git switch -c codex/non-helmet-companion-surfaces
```

- [ ] **Step 3: Re-state scope in the session**

Post this before dispatch:

```text
Scope: implement non-helmet local-first surfaces. Real helmet hardware, BLE, firmware, sensors, and hardware controls are out of scope. Use existing checks; do not add test files unless the user explicitly asks.
```

### Wave 1: Persistence And Repository Foundations

**Agent:** Agent 1, persistence owner. Do not run this in parallel with other agents because it owns database migrations and shared repositories.

**Files:**
- Create: `app/src/main/java/com/companion/chat/data/timeline/TimelineEvent.kt`
- Create: `app/src/main/java/com/companion/chat/data/local/dao/TimelineEventDao.kt`
- Create: `app/src/main/java/com/companion/chat/data/timeline/TimelineEventRepository.kt`
- Create: `app/src/main/java/com/companion/chat/data/dashboard/HomeDashboardModels.kt`
- Create: `app/src/main/java/com/companion/chat/data/dashboard/HomeDashboardRepository.kt`
- Create: `app/src/main/java/com/companion/chat/data/profile/UserProfileRepository.kt`
- Create: `app/src/main/java/com/companion/chat/data/privacy/PrivacySettingsRepository.kt`
- Create: `app/src/main/java/com/companion/chat/data/plan/PlanRepository.kt`
- Create: `app/src/main/java/com/companion/chat/data/setup/SetupRepository.kt`
- Modify: `app/src/main/java/com/companion/chat/data/local/CompanionDatabase.kt`
- Modify: `app/src/main/java/com/companion/chat/AppContainer.kt`

- [ ] **Step 1: Add timeline entity and DAO**

Use the canonical `TimelineEvent` model above. DAO shape:

```kotlin
package com.companion.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.companion.chat.data.timeline.TimelineEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: TimelineEvent)

    @Query("SELECT * FROM timeline_events ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TimelineEvent>>

    @Query("SELECT * FROM timeline_events ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TimelineEvent>

    @Query("DELETE FROM timeline_events")
    suspend fun deleteAll()
}
```

- [ ] **Step 2: Bump Room version and add migration**

In `CompanionDatabase.kt`:

- Add `TimelineEvent::class` to `entities`.
- Bump `version` from `3` to `4`.
- Add `abstract fun timelineEventDao(): TimelineEventDao`.
- Add `MIGRATION_3_4` that creates `timeline_events`.
- Register `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)`.

Migration SQL:

```sql
CREATE TABLE IF NOT EXISTS timeline_events (
    id TEXT NOT NULL PRIMARY KEY,
    type TEXT NOT NULL,
    title TEXT NOT NULL,
    detail TEXT NOT NULL,
    relatedSessionId TEXT,
    relatedMemoryId INTEGER,
    mediaUri TEXT,
    createdAt INTEGER NOT NULL
)
```

- [ ] **Step 3: Add timeline repository**

Repository responsibilities:

- `observeRecent(limit: Int = 20): Flow<List<TimelineEvent>>`
- `suspend fun add(type, title, detail, relatedSessionId, relatedMemoryId, mediaUri)`
- `suspend fun clear()`
- Generate ids with `UUID.randomUUID().toString()`.

- [ ] **Step 4: Add SharedPreferences repositories**

`UserProfileRepository` stores:

- `displayName`, default `"You"`
- `avatarUri`, default `""`
- `emergencyContactName`, default `""`
- `emergencyContactPhone`, default `""`

`PrivacySettingsRepository` stores the canonical `PrivacySettings` booleans. Defaults must keep remote paths off:

- `localOnlyMode = true`
- cloud/analytics/sharing toggles false

`PlanRepository` exposes a local-only state:

```kotlin
data class PlanState(
    val planName: String = "Local",
    val premiumVoiceEnabled: Boolean = false,
    val cloudFeaturesEnabled: Boolean = false,
    val renewalLabel: String = "No renewal"
)
```

`SetupRepository` exposes:

- profile complete
- microphone permission observed through UI input
- text model readiness from `CompanionReadinessRepository`
- voice readiness from `CompanionReadinessRepository`
- image readiness from `CompanionReadinessRepository`
- privacy reviewed

- [ ] **Step 5: Add home dashboard repository**

`HomeDashboardRepository` should compose:

- active role from `RoleCardRepository`
- recent memories from `MemoryRepository.getAllMemories()`
- readiness from `CompanionReadinessRepository.getSnapshot()`
- recent events from `TimelineEventRepository`
- no helmet mode always true

Do not query hardware.

- [ ] **Step 6: Register repositories**

In `AppContainer.kt`, add lazy properties:

- `timelineEventRepository`
- `userProfileRepository`
- `privacySettingsRepository`
- `planRepository`
- `setupRepository`
- `homeDashboardRepository`

- [ ] **Step 7: Verify**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/java/com/companion/chat/data app/src/main/java/com/companion/chat/AppContainer.kt
git commit -m "Add local product state repositories"
```

### Wave 2A: Home Dashboard Wiring

**Agent:** Agent 2, Home UI owner. Can run after Wave 1.

**Files:**
- Create: `app/src/main/java/com/companion/chat/ui/home/HomeDashboardViewModel.kt`
- Modify: `app/src/main/java/com/companion/chat/AppViewModelFactory.kt`
- Modify: `app/src/main/java/com/companion/chat/MainActivity.kt`
- Modify: `app/src/main/java/com/companion/chat/ui/home/HomeScreen.kt`

- [ ] **Step 1: Add HomeDashboardViewModel**

Shape:

```kotlin
class HomeDashboardViewModel(
    private val repository: HomeDashboardRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeDashboardUiState())
    val uiState: StateFlow<HomeDashboardUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = repository.getDashboardState()
        }
    }
}
```

- [ ] **Step 2: Register in factory**

Add `HomeDashboardViewModel` to `AppViewModelFactory` using `container.homeDashboardRepository`.

- [ ] **Step 3: Wire MainActivity**

In the Home route, create:

```kotlin
val homeDashboardViewModel: HomeDashboardViewModel = viewModel(factory = viewModelFactory)
```

Pass it to `HomeScreen`.

- [ ] **Step 4: Replace visual-only Home data**

In `HomeScreen.kt`, consume `HomeDashboardUiState` for:

- relationship level/XP/mood/closeness
- readiness summary
- quick actions
- recent memories
- recent activity
- suggestions

Keep `DiscoverViewModel` only for role discovery/import/generation.

- [ ] **Step 5: No-helmet copy**

Replace any home copy that implies a connected helmet with local mode copy when `localDevice.noHelmetMode` is true:

- `"Local companion mode"`
- `"No helmet connected"`
- `"Model, voice, and memory stay on this device"`

- [ ] **Step 6: Verify**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Manual smoke:

- Launch app.
- Home loads without crash.
- Home still opens Chat, Memory, Profile, and role detail.
- English and Chinese language switch still changes Home labels.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/companion/chat/ui/home app/src/main/java/com/companion/chat/AppViewModelFactory.kt app/src/main/java/com/companion/chat/MainActivity.kt
git commit -m "Wire home dashboard state"
```

### Wave 2B: Memory Review, Pinning, And Health

**Agent:** Agent 3, Memory owner. Can run after Wave 1.

**Files:**
- Create: `app/src/main/java/com/companion/chat/data/memory/MemoryReviewModels.kt`
- Modify: `app/src/main/java/com/companion/chat/data/local/entity/Memory.kt`
- Modify: `app/src/main/java/com/companion/chat/data/local/dao/MemoryDao.kt`
- Modify: `app/src/main/java/com/companion/chat/data/local/CompanionDatabase.kt`
- Modify: `app/src/main/java/com/companion/chat/data/memory/MemoryRepository.kt`
- Modify: `app/src/main/java/com/companion/chat/ui/memory/MemoryUiState.kt`
- Modify: `app/src/main/java/com/companion/chat/ui/memory/MemoryViewModel.kt`
- Modify: `app/src/main/java/com/companion/chat/ui/memory/MemoryScreen.kt`

- [ ] **Step 1: Extend Memory fields**

Add columns to `Memory`:

```kotlin
val isPinned: Boolean = false,
val reviewState: String = "confirmed",
val lastUsedAt: Long? = null
```

Add Room migration from current version to next version. If Wave 1 already bumped to version 4, bump to `5` and add `MIGRATION_4_5`.

Migration SQL:

```sql
ALTER TABLE memories ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0
ALTER TABLE memories ADD COLUMN reviewState TEXT NOT NULL DEFAULT 'confirmed'
ALTER TABLE memories ADD COLUMN lastUsedAt INTEGER
```

- [ ] **Step 2: Add DAO methods**

Add:

```kotlin
@Query("SELECT * FROM memories WHERE reviewState = 'candidate' ORDER BY createdAt DESC")
suspend fun getCandidateMemories(): List<Memory>

@Query("SELECT * FROM memories WHERE isPinned = 1 ORDER BY updatedAt DESC")
suspend fun getPinnedMemories(): List<Memory>

@Query("UPDATE memories SET reviewState = 'confirmed', updatedAt = :now WHERE id = :id")
suspend fun confirmCandidate(id: Long, now: Long): Int

@Query("UPDATE memories SET isPinned = :pinned, updatedAt = :now WHERE id = :id")
suspend fun setPinned(id: Long, pinned: Boolean, now: Long): Int

@Query("UPDATE memories SET lastUsedAt = :now, referenceCount = referenceCount + 1 WHERE id = :id")
suspend fun markUsed(id: Long, now: Long): Int
```

- [ ] **Step 3: Change model-extracted memories to candidates**

In `MemoryRepository.storeModelExtractedMemories`, insert model memories with `reviewState = "candidate"`. Manual memories remain `confirmed`.

- [ ] **Step 4: Add repository actions**

Add:

- `getCandidateMemories()`
- `getPinnedMemories()`
- `confirmCandidate(memoryId)`
- `deleteCandidate(memory)`
- `pinMemory(memoryId)`
- `unpinMemory(memoryId)`
- `markMemoryUsed(memoryId)`
- `getHealthMetrics()`

Health metrics:

```kotlin
data class MemoryHealthMetrics(
    val total: Int,
    val pinned: Int,
    val candidates: Int,
    val longTerm: Int,
    val shortTerm: Int
)
```

- [ ] **Step 5: Extend MemoryUiState**

Add:

- `candidateMemories`
- `pinnedMemories`
- `healthMetrics`
- `selectedUseNextTurnMemoryId`
- `message`

- [ ] **Step 6: Add ViewModel actions**

Add:

- `keepCandidate(memoryId)`
- `deleteCandidate(memory)`
- `pinMemory(memoryId)`
- `unpinMemory(memoryId)`
- `useNextTurn(memoryId)`
- `clearUseNextTurn()`

`useNextTurn` only updates UI state in this task. Chat injection is Agent 4.

- [ ] **Step 7: Update MemoryScreen**

Add sections:

- Review Queue with Keep/Edit/Delete/Pin.
- Pinned Memories with Use Next Turn/Unpin/Edit.
- Memory Health summary.

Keep existing filters and manual memory actions working.

- [ ] **Step 8: Verify**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Manual smoke:

- Add manual memory.
- Pin and unpin it.
- Edit and delete it.
- Memory screen does not resize awkwardly on narrow viewport.

- [ ] **Step 9: Commit**

```powershell
git add app/src/main/java/com/companion/chat/data app/src/main/java/com/companion/chat/ui/memory
git commit -m "Add memory review and pinning"
```

### Wave 2C: Profile, Privacy, Plan, Export, Delete

**Agent:** Agent 4, Profile owner. Can run after Wave 1.

**Files:**
- Create: `app/src/main/java/com/companion/chat/ui/settings/ProfileViewModel.kt`
- Modify: `app/src/main/java/com/companion/chat/AppViewModelFactory.kt`
- Modify: `app/src/main/java/com/companion/chat/MainActivity.kt`
- Modify: `app/src/main/java/com/companion/chat/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/companion/chat/data/export/DataExportRepository.kt`
- Modify: `app/src/main/java/com/companion/chat/data/timeline/TimelineEventRepository.kt`

- [ ] **Step 1: Add ProfileViewModel**

State includes:

- display name
- avatar URI
- plan state
- privacy settings
- emergency contact name/phone
- export status message
- delete confirmation state
- runtime readiness summary

Actions include:

- `updateDisplayName`
- `updateAvatarUri`
- `updateEmergencyContact`
- `updatePrivacySettings`
- `exportLocalData`
- `requestDeleteLocalData`
- `confirmDeleteLocalData`
- `cancelDeleteLocalData`

- [ ] **Step 2: Implement export**

`DataExportRepository.exportAll()` writes a JSON file under app-private files, for example:

```text
files/exports/companion-export-<timestamp>.json
```

Include:

- conversations
- messages
- memories
- role cards
- preferences when available

Return the absolute path string for display.

- [ ] **Step 3: Implement scoped delete**

Delete action must support scopes:

- memories only
- conversations only
- role cards only
- all local user data

Use explicit confirmation text in UI. Do not wipe model files.

- [ ] **Step 4: Wire Profile screen**

Replace `remember` state in `SettingsScreen` with `ProfileViewModel`.

Privacy toggles:

- Local-only mode
- Cloud ASR opt-in
- HTTP voice clone opt-in
- HTTP image generation opt-in
- Analytics opt-in
- Partner sharing opt-in

Show disabled reason for cloud toggles when local-only mode is on.

- [ ] **Step 5: Timeline events**

Write timeline events when:

- privacy changes
- export completes
- delete completes
- emergency contact changes

- [ ] **Step 6: Verify**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Manual smoke:

- Toggle local-only mode.
- Attempt to enable cloud ASR with local-only mode on; UI explains disabled state.
- Export local data and see a file path message.
- Open delete flow and cancel it.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/companion/chat/data app/src/main/java/com/companion/chat/ui/settings app/src/main/java/com/companion/chat/AppViewModelFactory.kt app/src/main/java/com/companion/chat/MainActivity.kt
git commit -m "Add profile privacy and data ownership state"
```

### Wave 2D: Non-Helmet Setup Flow

**Agent:** Agent 5, Setup owner. Can run after Wave 1.

**Files:**
- Create: `app/src/main/java/com/companion/chat/ui/setup/OnboardingViewModel.kt`
- Create: `app/src/main/java/com/companion/chat/ui/setup/OnboardingScreen.kt`
- Modify: `app/src/main/java/com/companion/chat/ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/companion/chat/MainActivity.kt`
- Modify: `app/src/main/java/com/companion/chat/AppViewModelFactory.kt`

- [ ] **Step 1: Add setup route**

Add:

```kotlin
object SetupRoutes {
    const val ONBOARDING = "setup/onboarding"
}
```

- [ ] **Step 2: Add setup ViewModel**

State steps:

- Profile
- Microphone permission
- Text model
- Voice input
- Voice output
- Image generation
- Privacy

Each step has:

```kotlin
data class SetupStepUiState(
    val id: String,
    val title: String,
    val status: SetupStatus,
    val detail: String,
    val actionLabel: String,
    val routeHint: String
)

enum class SetupStatus {
    READY,
    REQUIRED,
    OPTIONAL,
    SKIPPED,
    NEEDS_ATTENTION
}
```

- [ ] **Step 3: Build OnboardingScreen**

Compose screen sections:

- Local profile
- Permissions
- Model readiness
- Voice readiness
- Image readiness
- Privacy review

No helmet pairing step.

- [ ] **Step 4: Add navigation actions**

Actions should route to existing screens:

- model setup -> `SettingsRoutes.MODEL`
- voice setup -> `SettingsRoutes.VOICE`
- privacy -> `Screen.PROFILE.route`
- language -> `SettingsRoutes.LANGUAGE`

- [ ] **Step 5: Entry point**

Add a visible action from Profile advanced/settings card:

```text
Run Setup Check
```

Do not force onboarding before local-only use.

- [ ] **Step 6: Verify**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Manual smoke:

- Open Profile.
- Tap Run Setup Check.
- Open model and voice settings from setup.
- Back navigation returns cleanly.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/companion/chat/ui/setup app/src/main/java/com/companion/chat/ui/navigation app/src/main/java/com/companion/chat/MainActivity.kt app/src/main/java/com/companion/chat/AppViewModelFactory.kt
git commit -m "Add non-helmet setup checklist"
```

### Wave 3: Chat Timeline, Privacy, And Memory Injection

**Agent:** Agent 6, Chat owner. Run after Wave 1 and Wave 2B.

**Files:**
- Modify: `app/src/main/java/com/companion/chat/companion/turn/CompanionTurnModule.kt`
- Modify: `app/src/main/java/com/companion/chat/companion/turn/DefaultCompanionTurnModule.kt`
- Modify: `app/src/main/java/com/companion/chat/context/PromptAssembler.kt`
- Modify: `app/src/main/java/com/companion/chat/ui/chat/ChatViewModel.kt`
- Modify: `app/src/main/java/com/companion/chat/ui/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/companion/chat/ui/chat/components/MessageBubble.kt`

- [ ] **Step 1: Add privacy mode to ChatUiState**

Add:

- `privacyModeLabel`
- `localOnlyMode`
- `pinnedMemories`
- `useNextTurnMemory`
- `timelineEvents`
- `voiceNoteDurationLabel`

Read privacy from `PrivacySettingsRepository`.

- [ ] **Step 2: Add one-turn memory injection**

Extend `CompanionTurnRequest`:

```kotlin
data class CompanionTurnRequest(
    val text: String,
    val images: List<Uri> = emptyList(),
    val delivery: CompanionTurnDelivery = CompanionTurnDelivery.TextOnly,
    val oneTurnMemoryIds: List<Long> = emptyList()
)
```

In `DefaultCompanionTurnModule`, fetch those memories and pass them into prompt assembly as an extra section. Mark them used with `MemoryRepository.markMemoryUsed`.

- [ ] **Step 3: Add PromptAssembler support**

Add parameter:

```kotlin
oneTurnMemoryPrompt: String = ""
```

Place it after persistent memories and before history summary.

Use this section label:

```text
Memory selected for this turn:
```

Keep existing Chinese prompt sections for model behavior unless the user explicitly asks to localize internal prompts.

- [ ] **Step 4: Timeline writes**

Write events when:

- user sends a message
- assistant completes a message
- voice note final transcript arrives
- image generation succeeds
- user saves/pins/uses a memory

- [ ] **Step 5: Chat UI**

Add compact visible state:

- privacy chip near input or header
- pinned memory strip
- timeline/replay section in drawer or top sheet
- voice note transcript card metadata

Do not bury local-only/cloud state behind settings.

- [ ] **Step 6: Verify**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Manual smoke:

- Open Chat.
- Confirm local-only/privacy state is visible.
- Send a text message.
- Confirm timeline event appears.
- Select a pinned memory in Memory, use it for next turn, return to Chat, and confirm it is shown once.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/companion/chat/companion app/src/main/java/com/companion/chat/context app/src/main/java/com/companion/chat/ui/chat
git commit -m "Add chat memory and privacy context"
```

### Wave 4A: No-Helmet Device Diagnostics Cleanup

**Agent:** Agent 7, No-helmet UX owner. Run after Wave 1.

**Files:**
- Modify: `app/src/main/java/com/companion/chat/ui/navigation/AppNavigation.kt`
- Modify: `app/src/main/java/com/companion/chat/ui/helmet/HelmetScreen.kt`
- Modify: `app/src/main/java/com/companion/chat/ui/home/HomeScreen.kt`
- Modify: `docs/frontend-backend-gaps.md`

- [ ] **Step 1: Decide label**

Preferred no-hardware label:

- English: `Device`
- Chinese: `设备`

Change bottom nav label from Helmet to Device only if product direction accepts no-helmet mode. Keep route name `helmet` to avoid navigation churn.

- [ ] **Step 2: Remove hardware-implying primary copy**

Replace primary hardware claims with no-device local diagnostics copy:

- `"No helmet connected"`
- `"Local model, voice, and image readiness"`
- `"Pairing is skipped in this build"`
- `"Hardware controls require a real helmet"`

- [ ] **Step 3: Keep useful diagnostics**

Keep:

- LLM readiness
- ASR readiness
- TTS readiness
- image generation readiness
- links to model and voice settings
- diagnostic logs if they exist

Disable or visually de-emphasize:

- battery
- firmware
- BLE signal
- sensors
- ANC
- passthrough
- LED
- impact detection

- [ ] **Step 4: Update gap doc**

In `docs/frontend-backend-gaps.md`, add a no-helmet note:

```text
Real helmet telemetry and controls are out of scope until hardware is available. The current app should expose local device/model/voice diagnostics instead.
```

- [ ] **Step 5: Verify**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Manual smoke:

- Bottom nav text fits.
- Device/Helmet screen no longer reads as if hardware is present.
- Model and Voice settings links still work.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/companion/chat/ui/navigation app/src/main/java/com/companion/chat/ui/helmet app/src/main/java/com/companion/chat/ui/home docs/frontend-backend-gaps.md
git commit -m "Adapt diagnostics for no-helmet mode"
```

### Wave 4B: Localization, Copy, And Accessibility Audit

**Agent:** Agent 8, QA/localization owner. Run after all feature agents.

**Files:**
- Modify: `app/src/main/java/com/companion/chat/ui/language/LocalizedReadiness.kt`
- Modify: all changed UI files from previous waves
- Modify: `docs/demo.html` if demo pages are in scope

- [ ] **Step 1: Scan direct Chinese UI strings**

Run:

```powershell
rg -n 'Text\(\s*"[^"]*\p{Han}' app/src/main/java/com/companion/chat
rg -n 'contentDescription\s*=\s*"[^"]*\p{Han}' app/src/main/java/com/companion/chat
```

Expected: no direct UI display strings. Chinese should appear through `uiText(...)` or localization helpers.

- [ ] **Step 2: Scan obvious placeholder copy**

Run:

```powershell
rg -n "placeholder|Clone placeholder|demo|sample" app/src/main/java/com/companion/chat docs
```

Replace user-facing placeholders in app code with local-first real state or no-device copy. Leave technical docs alone unless the copy appears in a demo page that users see.

- [ ] **Step 3: Content descriptions**

Every icon-only button added by the feature agents must have a localized `contentDescription`.

- [ ] **Step 4: Narrow layout smoke**

Use emulator/device or Compose preview workflow available in the next session:

- Home
- Chat
- Memory
- Device/Helmet diagnostics
- Profile
- Setup

Check:

- no overlapped text
- bottom nav labels fit
- buttons keep at least 48dp touch target
- cards are not nested inside cards

- [ ] **Step 5: Verify**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\scripts\android-dev.bat build
```

Expected: both commands report `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/companion/chat docs
git commit -m "Polish localization and accessibility"
```

## Final Integration Checklist

- [ ] `git status -sb` shows only intended changes before each commit.
- [ ] All database migrations are registered in `CompanionDatabase.kt`.
- [ ] `AppContainer.kt` exposes every repository used by a ViewModel.
- [ ] `AppViewModelFactory.kt` can create every new ViewModel.
- [ ] Home no longer depends on visual-only fake relationship, memory, and activity data.
- [ ] Chat shows privacy state before voice or cloud capture.
- [ ] Memory candidates do not affect prompts until the user keeps, pins, or selects them for next turn.
- [ ] Profile can export local data and delete scoped local user data without deleting model files.
- [ ] Setup has no helmet pairing step.
- [ ] Device/Helmet screen does not imply real connected hardware.
- [ ] English is the default app language.
- [ ] Chinese appears through language-switch helpers, not direct UI literals.
- [ ] `.\gradlew.bat :app:compileDebugKotlin` passes.
- [ ] `.\scripts\android-dev.bat build` passes.
- [ ] Final commit is pushed to the working branch.

## Suggested Agent Dispatch Prompts

Use these prompts in the next session.

### Agent 1 Prompt

```text
Use docs/superpowers/plans/2026-06-10-non-helmet-companion-surfaces.md Wave 1 only. You own persistence and repositories. Do not edit UI except AppContainer registration. Real helmet hardware is out of scope. Do not add test files. Verify with .\gradlew.bat :app:compileDebugKotlin and commit "Add local product state repositories".
```

### Agent 2 Prompt

```text
Use docs/superpowers/plans/2026-06-10-non-helmet-companion-surfaces.md Wave 2A only. You own Home dashboard wiring. Do not edit database. Keep DiscoverViewModel for role discovery only. Real helmet hardware is out of scope. Verify with .\gradlew.bat :app:compileDebugKotlin and commit "Wire home dashboard state".
```

### Agent 3 Prompt

```text
Use docs/superpowers/plans/2026-06-10-non-helmet-companion-surfaces.md Wave 2B only. You own memory review, pinning, and health. Coordinate database migration version with Agent 1 output. Do not add test files. Verify with .\gradlew.bat :app:compileDebugKotlin and commit "Add memory review and pinning".
```

### Agent 4 Prompt

```text
Use docs/superpowers/plans/2026-06-10-non-helmet-companion-surfaces.md Wave 2C only. You own Profile, privacy, plan, export, and delete flows. Keep all cloud features explicit opt-in and disabled by local-only mode. Verify with .\gradlew.bat :app:compileDebugKotlin and commit "Add profile privacy and data ownership state".
```

### Agent 5 Prompt

```text
Use docs/superpowers/plans/2026-06-10-non-helmet-companion-surfaces.md Wave 2D only. You own non-helmet setup. No helmet pairing step. Setup should route to existing model, voice, privacy, and language surfaces. Verify with .\gradlew.bat :app:compileDebugKotlin and commit "Add non-helmet setup checklist".
```

### Agent 6 Prompt

```text
Use docs/superpowers/plans/2026-06-10-non-helmet-companion-surfaces.md Wave 3 only. You own Chat timeline, privacy, and one-turn memory injection. Run after memory pinning exists. Do not add remote services. Verify with .\gradlew.bat :app:compileDebugKotlin and commit "Add chat memory and privacy context".
```

### Agent 7 Prompt

```text
Use docs/superpowers/plans/2026-06-10-non-helmet-companion-surfaces.md Wave 4A only. You own no-helmet diagnostics cleanup. Keep readiness diagnostics, remove claims of connected hardware, and keep hardware controls visibly unavailable. Verify with .\gradlew.bat :app:compileDebugKotlin and commit "Adapt diagnostics for no-helmet mode".
```

### Agent 8 Prompt

```text
Use docs/superpowers/plans/2026-06-10-non-helmet-companion-surfaces.md Wave 4B only. You own localization, copy, accessibility, and final build verification. Do not refactor feature logic. Run direct Chinese UI string scans and .\scripts\android-dev.bat build. Commit "Polish localization and accessibility".
```

## Final Push

After all wave commits:

```powershell
git status -sb
.\scripts\android-dev.bat build
git push -u origin $(git branch --show-current)
```

Expected:

- status shows no uncommitted changes
- build is successful
- branch is pushed
