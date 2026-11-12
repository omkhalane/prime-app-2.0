# PRIME Android release-readiness checklist

This file separates repository work from release tasks that require owner credentials, product decisions, Google Play declarations, or physical-device verification. A successful debug build is not a production release approval.

## Included in this repository

- App launcher icon resources for mdpi through xxxhdpi, adaptive icon XML, and PRIME logo assets.
- Onboarding illustration/background and focus/preset artwork in `app/src/main/res/drawable`.
- Shared black/graphite/white/emerald design tokens and Compose UI components.
- Local Room-backed planner data; empty states are real empty states, not seeded sample activity.
- JVM/Robolectric regression tests for core scheduling, DNS packet parsing, blocking duration, achievement migration, and onboarding name validation.

## Required before publishing

### Product identity and signing
- [ ] Choose and confirm the permanent Android `applicationId`. The current `com.aistudio.applet.rfrquv` is a generated placeholder and must not be published as PRIME's final identity.
- [ ] Align the Gradle namespace and Kotlin package names with the approved identity in a dedicated migration.
- [ ] Create and securely back up the Play upload keystore. Configure `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` in the release environment; never commit the keystore or passwords.
- [ ] Set the production version code/name for the first Play release and define the versioning policy for future updates.
- [ ] Generate and inspect a signed release AAB; verify its certificate and install/update behavior on a clean test device.

### Service configuration
- [ ] Decide whether Firebase AI, App Check, and Google sign-in are part of the first release. The repository currently has Firebase Gradle dependencies/plugins but no committed `google-services.json`; supply the correct project configuration securely if those features are enabled.
- [ ] Verify all AI/network features have a configured endpoint, authentication, quota controls, privacy disclosure, and graceful offline/error states. Do not ship debug App Check configuration to production.
- [ ] Verify backup/data-extraction rules and test restore behavior for user-created schedules, tasks, habits, and blocking settings.

### Permissions and Play declarations
- [ ] Review every manifest permission against an actually shipped user-facing feature and remove anything not needed.
- [ ] Complete Google Play declarations and prominent in-app disclosure for AccessibilityService use. Explain exactly what is observed, why it is needed, and that accessibility data is not sold.
- [ ] Complete the VPN service declaration and user-facing disclosure for on-device website filtering. Verify no traffic is routed to a remote VPN server.
- [ ] Confirm eligibility and Play Console declaration requirements for exact alarms, full-screen intents, foreground-service types, and usage access on the target SDK.
- [ ] Verify notification permission denial, revocation, and Android versions below API 33.
- [ ] Test permission revocation and returning from Android Settings while PRIME remains alive.

### Quality and device validation
- [ ] Run `./gradlew clean :app:assembleDebug :app:testDebugUnitTest`.
- [ ] Run `./gradlew :app:lintRelease` and resolve or explicitly review every warning.
- [ ] Test on API 24 minimum, a current Android release, and at least one Samsung device.
- [ ] Test VPN/DNS filtering, IPv4/IPv6, Wi-Fi/mobile network changes, reboot recovery, and battery saver behavior.
- [ ] Test AccessibilityService blocking, browser search pages, app switching, and service restart/revocation.
- [ ] Test exact alarms, recurring alarms, ringtone playback, Do Not Disturb, lock-screen ringing, and reboot recovery.
- [ ] Test onboarding from a fresh install, every back/continue path, denied permissions, returning from settings, and completing without optional permissions.
- [ ] Test planner create/edit/complete/delete flows, duplicate names, invalid dates, empty habit schedules, and data persistence after process death.
- [ ] Check small/large screens, landscape, display/font scaling, TalkBack labels, touch targets, and dark-theme contrast.

### Store listing and legal
- [ ] Prepare final 512×512 Play icon, feature graphic, phone screenshots, short description, full description, and localized listing text.
- [ ] Publish a public privacy policy and support contact before collecting or processing user data.
- [ ] Complete Data safety, content rating, target audience, ads, account deletion (if accounts are introduced), and app access declarations accurately.
- [ ] Verify all bundled image/audio assets are original or properly licensed for commercial distribution.
- [ ] Run internal and closed testing, collect crash/ANR feedback, and only then request production access.

## Release decision

**Current status: not yet store-ready.** The source tree contains a useful set of PRIME assets and the app can be built by CI, but the generated application ID, release signing, service configuration decisions, Play declarations, store listing, and physical-device QA remain owner/release tasks. Do not mark this checklist complete based only on a successful CI build.
