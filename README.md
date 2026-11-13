# PRIME

**PRIME is an Android digital-wellbeing and productivity app** built to help people understand their device usage, reduce distractions, protect focus time, and organize everyday work.

The app combines screen-time insights, app and website blocking, focus timers, Pomodoro sessions, alarms, achievements, and a locally persisted task-and-habit planner in one Android application.

> **Project status:** Active development. This repository contains implemented application code as well as roadmap items that are not yet complete. PRIME is not yet declared ready for a public Play Store release; see [Release readiness](RELEASE_READINESS.md).

## Download PRIME for Android

**Get the Android APK:** [View PRIME releases and download the latest APK](https://github.com/omkhalane/prime-app-2.0/releases/latest).

Each published release includes its version, release notes, and downloadable APK under **Assets**. If no release is published yet, check the [all releases page](https://github.com/omkhalane/prime-app-2.0/releases) again later.

### Install from GitHub Releases

1. Open the [latest PRIME release](https://github.com/omkhalane/prime-app-2.0/releases/latest).
2. Expand **Assets** and download the file named `PRIME-<version>.apk`.
3. Open the downloaded APK on your Android device and approve installation from that source if Android asks.
4. Launch PRIME and grant optional Android special access only when you enable the related feature.

> **Beta notice:** PRIME is under active development. Device compatibility and feature availability can vary. Review the release notes and known limitations before installing. A GitHub APK is not a Google Play Store release.

---

## Contents

- [Product overview](#product-overview)
- [Features](#features)
- [Technology stack](#technology-stack)
- [Architecture](#architecture)
- [Repository structure](#repository-structure)
- [How the main systems work](#how-the-main-systems-work)
- [Requirements](#requirements)
- [Getting started](#getting-started)
- [Configuration and secrets](#configuration-and-secrets)
- [Permissions and privacy](#permissions-and-privacy)
- [Data storage](#data-storage)
- [Testing](#testing)
- [Known limitations and release checklist](#known-limitations-and-release-checklist)
- [Roadmap](#roadmap)
- [Contributing](#contributing)

---

## Product overview

PRIME is organized around five primary navigation areas:

| Area | Purpose |
|---|---|
| **Home** | Personal dashboard, wellbeing status, and shortcuts |
| **Focus** | Focus timer, presets, Pomodoro, and stopwatch |
| **Shields** | App blocking, website blocking, and schedules |
| **Insights** | Screen-time analytics, reports, and progress |
| **You** | Profile, achievements, targets, and settings |

The product direction is to make digital wellbeing actionable: show real device data, let users configure their own boundaries, and help them follow through with focused work.

## Features

The descriptions below reflect functionality represented in the current source tree. Android permissions, device support, and configuration can affect whether a feature is available on a particular device.

### 1. Digital wellbeing and screen-time tracking

- Reads app-usage information through Android's `UsageStatsManager`.
- Identifies launchable applications and associates usage with installed apps.
- Provides today and weekly usage data, category summaries, hourly usage information, and trend data.
- Tracks app opens and notification-interruption events where Android exposes the required signals.
- Excludes PRIME itself and selected system, launcher, keyboard, and background packages from screen-time totals.
- Checks Usage Access permission and represents permission or fetch errors instead of treating missing data as measured usage.

**Important:** Android Usage Access must be granted by the user in system settings. Metrics and event availability can vary by Android version and device manufacturer.

### 2. App blocking and focus shields

- Configurable blocked application targets.
- Time-based blocking rules with selected weekdays and start/end times.
- Focus-session blocking targets.
- Pause/resume controls and temporary snooze support.
- A dedicated blocked-app screen.
- Blocking state reconciliation and recovery support.

App interception is implemented with an Android Accessibility Service. Users must explicitly enable the service in Android settings. Blocking behaviour depends on Android lifecycle behaviour and OEM restrictions; it should not be described as impossible to bypass.

### 3. Website blocking

- User-managed website/domain blocklists.
- Domain normalization and validation.
- Website categories and category-based rules.
- Scheduled website blocking.
- Blocking analytics, including recent blocked events and frequently attempted domains.
- Local VPN-based DNS filtering through Android's `VpnService`.
- Accessibility-based browser URL detection as an additional enforcement path.
- Blocking-state recovery and periodic rule reconciliation.

The VPN implementation creates a local TUN interface and processes DNS traffic for its configured synthetic DNS endpoints. The source is designed as on-device filtering; it is not a conventional remote VPN service that routes all user traffic through a PRIME server.

Website enforcement can differ across browsers, encrypted DNS configurations, apps, Android releases, and network conditions. A local DNS filter cannot guarantee interception of every possible network path.

### 4. Focus timer and productivity sessions

- Configurable countdown/focus timer.
- Focus presets, including study and other session types.
- Option to block selected applications during a timer session.
- Pomodoro focus and break sessions with configurable settings.
- Stopwatch and lap state.
- Timer/Pomodoro completion notifications and sound preferences.
- Timer state persistence and completion handling outside the foreground UI.

### 5. Alarms and clock tools

- One-time and recurring alarms.
- Daily, weekday, weekend, and custom-day recurrence.
- Specific-date alarms.
- Snooze scheduling.
- Ringtone selection and imported ringtone URI support.
- Vibration and alarm challenge configuration.
- Lock-screen alarm presentation.
- Alarm restoration after device boot or app replacement.

Exact delivery depends on Android alarm permissions, system settings, and device power-management behaviour.

### 6. Progress, scores, and achievements

- PRIME score and score breakdown models.
- Focus, sleep, and rest score fields.
- Streak tracking.
- Badges, daily quests, and weekly mission models.
- Personal targets and progression/rank models.
- Achievement dashboard UI.

A displayed score should be interpreted according to the underlying data and permission availability. The roadmap calls for further explainability and device validation.

### 7. Tasks, projects, and habits

The planner uses a dedicated Room database and includes:

- Tasks with title, notes, priority, optional due date, recurrence, and completion state.
- Projects with names and colors.
- Habits with weekday schedules.
- Date-specific habit check-ins.
- Task completion and recurring-task generation for supported recurrence options.
- Habit archive and check-in history data.
- Validation for task/habit input and duplicate names.

The planner database is separate from the older preference-backed application data. Importing legacy planner data is still a migration task; do not assume old planner records have already been imported into Room.

### 8. AI and network integrations

The Android build includes Firebase AI-related dependencies and a Gradle secrets configuration convention. The example environment file documents a Gemini API key placeholder.

**A dependency in Gradle does not by itself mean an AI feature is fully configured or production-ready.** Provider credentials, actual call paths, consent, quotas, privacy controls, and error handling must be verified before enabling or advertising an AI workflow.

---

## Technology stack

| Layer | Technologies |
|---|---|
| Language | Kotlin |
| Android UI | Jetpack Compose |
| Design system | Material 3, Compose UI |
| App architecture | ViewModel, state-driven UI, Kotlin Flow |
| Local persistence | Room, SharedPreferences |
| Background execution | WorkManager, Android services and receivers |
| Async/concurrency | Kotlin Coroutines and Flow |
| Screen-time data | Android UsageStatsManager and UsageEvents |
| App enforcement | Android AccessibilityService |
| Website filtering | Android VpnService, local DNS filtering |
| Alarms/timers | AlarmManager, PendingIntent, BroadcastReceiver |
| Networking libraries | Retrofit, OkHttp |
| JSON serialization | Moshi |
| Image loading | Coil |
| AI-related dependencies | Firebase AI |
| App Check dependencies | Firebase App Check |
| Dependency injection | Dependencies are currently wired directly; no dedicated DI framework is configured |
| Build | Gradle Kotlin DSL, Gradle Version Catalog, KSP |
| Tests | JUnit, AndroidX test libraries, Coroutines Test, Robolectric |

### Android build configuration

The current Gradle configuration specifies:

| Setting | Current value |
|---|---|
| Android namespace | `com.example` |
| Application ID | `com.aistudio.applet.rfrquv` |
| Minimum SDK | 24 |
| Compile SDK | 36 (minor API level 1) |
| Target SDK | 36 |
| Version name | `1.0` |
| Version code | `1` |
| Java source/target compatibility | 11 |
| Gradle wrapper | 9.3.1 |
| Android Gradle Plugin | 9.1.1 |
| Kotlin | 2.2.10 |

The application ID and namespace are current repository values, not a recommendation for the final public package identity.

---

## Architecture

PRIME uses a feature-oriented package structure with a central Compose entry point and a ViewModel exposing UI state.

At a high level:

```text
Android application
       |
       v
MainActivity (Jetpack Compose)
       |
       v
OpalViewModel -----> OpalUiState (StateFlow)
       |
       +---- OpalRepository
       |       +---- SharedPreferences-backed app/profile/timer state
       |       +---- alarms and focus rules
       |
       +---- WellbeingRepository
       |       +---- RealScreenTimeTracker
       |       +---- UsageStatsManager / UsageEvents
       |
       +---- BlockingManager
       |       +---- BlocklistRepository
       |       +---- BlockingScheduleRepository
       |       +---- BlockingAnalyticsRepository
       |       +---- PrimeVpnService / DNS filter
       |       +---- OpalAccessibilityService
       |
       +---- PrimeAchievementRepository
       |
       +---- Productivity Room database
               +---- tasks
               +---- projects
               +---- habits
               +---- habit check-ins
```

This is a conceptual map of the main source components, not a claim that every feature follows a strict clean-architecture or dependency-injection boundary.

### Important implementation components

| Component | Responsibility |
|---|---|
| `MainActivity` | Compose app entry point, screen composition, and permission-result integration |
| `OpalViewModel` | UI state, user actions, and coordination between repositories/services |
| `OpalRepository` | App, profile, focus-rule, timer, alarm, and preference-backed state |
| `WellbeingRepository` | Daily/weekly usage queries and wellbeing analytics |
| `RealScreenTimeTracker` | Installed-app discovery and screen-time aggregation |
| `BlockingManager` | Coordinates app/website blocking, schedules, analytics, and recovery |
| `PrimeVpnService` | Local VPN interface and DNS filtering lifecycle |
| `OpalAccessibilityService` | App interception and browser URL inspection |
| `AlarmSchedulerHelper` | Alarm calculation, scheduling, snoozing, and cancellation |
| `BootReceiver` | Restores alarms/timers and blocking state after boot/update |
| `ProductivityDatabase` | Room database for planner entities |
| `ProductivityDao` | Reactive queries and task/project/habit persistence |

---

## Repository structure

The main source layout is:

```text
prime-app-2.0/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt
│       │   │   ├── alarm/
│       │   │   ├── data/
│       │   │   ├── model/
│       │   │   ├── productivity/
│       │   │   │   └── data/
│       │   │   ├── service/
│       │   │   ├── ui/
│       │   │   │   ├── components/
│       │   │   │   ├── screens/
│       │   │   │   └── theme/
│       │   │   ├── util/
│       │   │   ├── viewmodel/
│       │   │   └── vpn/
│       │   │       ├── db/
│       │   │       ├── dns/
│       │   │       ├── repo/
│       │   │       └── service/
│       │   └── res/
│       └── test/
├── docs/
│   └── PRODUCTIVITY_ECOSYSTEM_ROADMAP.md
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── .env.example
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
└── RELEASE_READINESS.md
```

The tree above highlights the main modules and packages; it is not an exhaustive listing of every source/resource file.

---

## How the main systems work

### Screen-time pipeline

1. PRIME checks whether the user has granted Usage Access.
2. It discovers launchable applications through Android's PackageManager.
3. It queries Android usage aggregates/events for the requested date range.
4. It filters PRIME and selected system/background packages.
5. It maps measured usage into app and category data for the UI.
6. Permission failures or unavailable data should be surfaced as unavailable/error states, not replaced with fabricated usage.

### App and website blocking pipeline

1. The user configures app/domain targets and optional schedules.
2. Blocking repositories persist website blocklists, categories, schedules, and event data.
3. The blocking manager reconciles active rules and updates enforcement targets.
4. The Accessibility Service handles app switching and browser URL inspection.
5. The local VPN service processes configured DNS traffic through the DNS filter.
6. Blocked attempts are sent to the analytics repository.
7. Recovery logic reconciles blocking after service or device restarts.

### Alarm restoration pipeline

1. Alarm definitions are stored by the application repository.
2. The scheduler calculates the next trigger based on recurrence and selected days.
3. Android AlarmManager receives the corresponding PendingIntent.
4. The receiver handles the alarm event and updates the stored alarm state.
5. Boot/package-replacement handling schedules enabled alarms again.

### Planner persistence pipeline

1. Planner operations are exposed through the productivity data layer.
2. Room entities store tasks, projects, habits, and date-keyed check-ins.
3. DAO queries expose observable Kotlin Flows.
4. Task completion can transactionally mark the current task complete and insert its next recurring instance.

---

## Requirements

To build and work on PRIME, install:

- Android Studio with support for the repository's Android Gradle Plugin.
- A compatible JDK for the configured Android/Gradle toolchain.
- Android SDK Platform 36 and required build tools.
- An Android emulator or physical Android device for runtime testing.

Some features require a physical device and explicit system permissions. An emulator build alone cannot validate all usage tracking, Accessibility Service, VPN, exact alarm, reboot, and OEM power-management behaviour.

## Getting started

### 1. Clone the repository

```bash
git clone https://github.com/omkhalane/prime-app-2.0.git
cd prime-app-2.0
```

### 2. Open in Android Studio

Open the cloned project directory and allow Gradle to sync. Install any SDK components requested by Android Studio.

### 3. Configure optional secrets

Review [Configuration and secrets](#configuration-and-secrets). Do not commit API keys, signing credentials, keystores, or local configuration files.

### 4. Build the debug APK

On Linux/macOS:

```bash
./gradlew :app:assembleDebug
```

On Windows:

```powershell
gradlew.bat :app:assembleDebug
```

The debug APK is normally generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 5. Install on a connected device

```bash
./gradlew :app:installDebug
```

After installation, grant only the permissions required for the features you want to test. Usage Access and Accessibility Service are enabled from Android Settings rather than ordinary runtime permission dialogs.

---

## Configuration and secrets

The project applies the Google Secrets Gradle Plugin and points it at:

- `.env` — local secret values; ignored by Git.
- `.env.example` — checked-in example/template.

The current example documents a `GEMINI_API_KEY` placeholder. It is commented out by default. Configure a real key only if the relevant AI code path is enabled and you understand the associated data flow.

Firebase-related Gradle plugins/dependencies are present. The repository's release-readiness checklist notes that a production `google-services.json` is not committed. Supply project configuration through the appropriate secure setup if Firebase services are actually required.

Never commit:

- API keys or service-account credentials.
- `google-services.json` if it contains project configuration your team does not intend to publish.
- Upload keystores or signing passwords.
- Local `.env` files, debug signing material, or machine-specific configuration.

Release signing is configured to read `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` from the build environment. The current release configuration expects an upload key alias of `upload`. Keep the keystore and passwords outside source control.

---

## Permissions and privacy

The manifest declares permissions/capabilities for network access, notifications, vibration, wake locks, boot restoration, exact alarms, full-screen alarm presentation, Usage Access, and foreground services. It also declares an Accessibility Service and a VPN Service.

| Permission/access | Used for |
|---|---|
| Usage Access | Reading app usage statistics |
| Accessibility Service | App blocking and browser URL detection |
| VPN consent | Local DNS-based website filtering |
| Notifications | Timer, alarm, and service notifications |
| Exact alarm access | User-configured alarm delivery |
| Full-screen intent eligibility | Alarm ringing presentation on supported Android versions |
| Internet/network state | Network-dependent functionality and configured integrations |
| Boot completed | Restoring eligible alarms/timers/blocking after restart |

PRIME should request special access only when the user enables the related feature, explain the purpose clearly, and provide a usable experience when optional access is denied.

Before public release, publish a privacy policy and accurately disclose Accessibility Service and VPN behaviour in-app and in Play Console. Review every manifest permission against the shipped feature set. Do not claim that PRIME collects no data without auditing all code paths, SDKs, network calls, and disclosures.

---

## Data storage

The current codebase uses more than one persistence mechanism:

| Store | Data represented |
|---|---|
| SharedPreferences (`prime_focus_prefs`) | Focus/app state, rules, timer and related preferences |
| SharedPreferences (`prime_blocking_manager_prefs`) | Blocking manager state and protection preferences |
| Room website-block database | Website domains, categories, schedules, and blocked-event analytics |
| Room productivity database (`prime_productivity.db`) | Tasks, projects, habits, and habit check-ins |

The productivity Room database is separate from legacy preference-backed planner storage. A safe one-time migration/import remains necessary before the old planner data can be retired. Do not clear app data or remove legacy storage as a substitute for migration.

---

## Testing

The project includes JVM/Robolectric and AndroidX test dependencies, along with unit tests for areas such as:

- App-name formatting.
- Streak calculations.
- Website domain normalization and validation.
- Repository defaults and rule pause/resume behaviour.
- App-blocking schedule calculations.
- DNS packet parsing and related blocking logic.
- Planner validation and persistence logic.

Run unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

Run the debug build and unit tests together:

```bash
./gradlew clean :app:assembleDebug :app:testDebugUnitTest
```

Run release lint:

```bash
./gradlew :app:lintRelease
```

A successful build or unit-test run does not replace device testing. Validate permission denial/revocation, background execution, process death, reboot, network changes, battery saver, date/time boundaries, and supported Android/OEM combinations.

---

## Known limitations and release checklist

The repository includes a dedicated [release-readiness checklist](RELEASE_READINESS.md). Key outstanding items include:

- Replace the generated application ID with the approved permanent PRIME identity and plan any package/namespace migration.
- Configure and securely back up production release signing credentials.
- Decide which Firebase/AI services are part of the release and configure them securely.
- Verify AI/network features end-to-end, including consent, authentication, quotas, privacy controls, and offline/error states.
- Complete Play Console declarations and prominent disclosures for Accessibility Service, VPN, exact alarms, full-screen intents, and foreground services as applicable.
- Validate screen-time and blocking accuracy on real devices, including Samsung and other OEM variants.
- Test alarm/timer delivery, ringtone playback, reboot recovery, and permission changes.
- Complete accessibility, TalkBack, font scaling, screen-size, backup/restore, and data deletion checks.
- Prepare store listing assets, privacy policy, support contact, and legal declarations.

**Do not treat roadmap entries as shipped features.** The roadmap explicitly separates current baseline functionality from planned work and requires UI, persistence, permission handling, tests, and device behaviour before a feature is called complete.

---

## Roadmap

The detailed product plan lives in [docs/PRODUCTIVITY_ECOSYSTEM_ROADMAP.md](docs/PRODUCTIVITY_ECOSYSTEM_ROADMAP.md).

Broad planned areas include:

1. **Planner completion:** legacy data migration, project/task editing, subtasks, reminders, calendar integration, and habit history.
2. **Wellbeing accuracy:** stronger usage validation, goals, data freshness, notification analytics, and export.
3. **Focus intelligence:** task-linked focus sessions, advanced blocking controls, ambient audio, and explainable scoring.
4. **AI productivity coach:** opt-in insights and planning with data preview, consent, and privacy controls.
5. **Sleep and digital detox:** user-configured routines and supported sleep-related data.
6. **Platform polish:** widgets, Quick Settings, backup/restore, sync, accessibility, and device QA.

---

## Contributing

Contributions are welcome. A useful contribution should:

1. Explain the user problem and expected behaviour.
2. Keep user activity and analytics grounded in real device/provider data.
3. Avoid demo records, fabricated usage, canned AI responses presented as personalized, or silent fallback data.
4. Handle permission denial, missing providers, offline states, and errors explicitly.
5. Add or update tests for business logic and edge cases.
6. Test relevant behaviour on an Android device when platform services are involved.
7. Update documentation when architecture, permissions, setup, or feature status changes.

For substantial changes, open an issue or discussion before starting implementation.

---

## Project links

- **Repository:** https://github.com/omkhalane/prime-app-2.0
- **Product roadmap:** [docs/PRODUCTIVITY_ECOSYSTEM_ROADMAP.md](docs/PRODUCTIVITY_ECOSYSTEM_ROADMAP.md)
- **Release checklist:** [RELEASE_READINESS.md](RELEASE_READINESS.md)

---

<p align="center">
  Built with Kotlin and Jetpack Compose.
</p>
