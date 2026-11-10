# Prime Productivity Ecosystem — Implementation Roadmap

Prime is being expanded from a digital-wellbeing and clock app into a privacy-first productivity ecosystem. This roadmap records the complete requested scope and distinguishes shipped work from planned work; a roadmap item is not considered implemented until its UI, persistence, permission flow, tests and device behaviour are complete.

## Current baseline

- Digital wellbeing dashboard and screen-time tracking.
- App and website blocking, schedules, blocking analytics and recovery.
- Alarm, countdown timer, Pomodoro and stopwatch experiences.
- Alarm themes, ringtone selection/import, completion alerts and reboot restoration (PR #4 merged into main).
- Prime score, achievements, streak and weekly-report foundations.
- **New in this branch:** offline-first Daily Planner tasks and daily habit check-ins on the Home screen. Tasks persist locally; habit check-ins reset by local calendar day.

## Product modules and complete feature scope

### 1. Digital Wellbeing
- Validate foreground usage against Android UsageStatsManager and installed-package metadata.
- Daily/weekly/monthly app usage, hourly heatmap, unlock/session counts where the platform exposes reliable signals.
- Notification counts and categories through an explicitly enabled NotificationListenerService; never collect notification content by default.
- Per-app goals, usage warnings, category budgets, usage trends and export.
- Explain data freshness, permission state, unavailable metrics and Android/OEM limitations in the UI.

### 2. Focus and app blocking
- Focus sessions linked to tasks and projects.
- App blocking profiles, schedules, strict mode, break allowances and emergency cooldown.
- Keep enforcement transparent and use supported Android APIs; never promise an unbypassable blocker on ordinary consumer devices.
- Focus environments: locally bundled ambient sound loops, mix controls, sleep timer, offline playback and audio focus handling.
- XP, achievements, streaks and opt-in personal challenges.

### 3. Tasks, habits and planning
- Current first slice: local task list and daily habit check-ins.
- Next: task priorities, due dates, recurring tasks, projects, subtasks, calendar view, reminders and completion history.
- Link tasks to Pomodoro sessions and calculate focus time per task.
- Habit creation, schedules, streaks, calendar heatmaps and progress charts.
- AI-assisted daily planning only after the deterministic planner and data model are stable.

### 4. AI productivity coach
- Opt-in daily and weekly insights based on locally aggregated metrics.
- Explain recommendations with the source metrics and time range.
- AI task prioritisation and schedule suggestions; user approves all changes.
- Privacy controls: preview what is sent, redact app names, disable cloud analysis, delete AI history.
- Keep essential wellbeing, timer, task and alarm functions usable offline. Do not make unsupported medical or diagnostic claims.

### 5. Clock and focus tools
- Continue device testing and hardening of alarms, timer, stopwatch and Pomodoro.
- Multiple alarm profiles, smart snooze controls, gradual volume, challenge dismissal, local audio library and wallpaper selection.
- Spotify can provide supported playlist discovery/deep links; protected Spotify tracks must not be represented as locally playable alarm tones.
- Ambient audio and sound packs must support offline use and correct audio-focus/foreground-service behaviour.

### 6. Sleep and detox
- Bedtime schedule, wind-down reminders, sleep consistency tracking based on user-entered or supported device data.
- Digital detox schedules, focus profiles and mindful break reminders.
- Do not claim clinical sleep tracking without a supported sensor/data source.

### 7. Platform and premium quality
- Home-screen widgets and Android Quick Settings tile.
- Theme editor, accessible typography, contrast and reduced-motion option.
- Backup/restore and optional cloud sync with encryption and conflict handling.
- Monthly reports, data export/deletion and clear privacy settings.
- Battery, accessibility, TalkBack, large-font and small-screen QA.

## Delivery phases

1. **Foundation (active):** task/habit local persistence, model and repository design, test coverage, UX entry points.
2. **Planner:** recurring tasks, priorities, reminders, calendar, task-to-focus linking and habit history.
3. **Wellbeing accuracy:** reconcile all usage metrics, data freshness, permission education, notification analytics and goals.
4. **Focus intelligence:** focus environments, focus score explainability, challenges and advanced blocking controls.
5. **AI coach:** opt-in insights, AI planner and weekly reports with privacy controls.
6. **Platform polish:** widgets, Quick Settings, sync, backup, sleep/detox and accessibility QA.

## Engineering rules

- Prefer Room for structured tasks, habits, recurring schedules and history; DataStore for settings. Migrate SharedPreferences data safely rather than dropping existing user data.
- Use WorkManager for deferrable reminders/sync; AlarmManager only for user-visible exact alarm use cases and handle denied exact-alarm access.
- Use Android runtime/special access permissions only when the feature is enabled and explain why before requesting them.
- Add unit tests for date boundaries, recurring schedules, streak calculation, score calculations and state restoration.
- Test background execution, process death, reboot, time-zone changes, daylight-saving changes, battery saver and OEM restrictions on physical devices.
- Do not claim a feature is complete based on a screen mockup alone. Each feature needs real behaviour, persistence, error states, tests and documented platform limitations.
