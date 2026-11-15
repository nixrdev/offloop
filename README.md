# Offloop

**Break the loop. Take back your time.**

Offloop is an Android utility that recognises when you enter endless short-form
video feeds and — eventually — interrupts them. Everything runs on the device.
No account, no backend, no network permission.

## Current milestone

The app now detects and interrupts short-form feeds:

- Jetpack Compose home screen with accessibility-service status
- `AccessibilityService` that watches YouTube and Instagram
- View-ID based detection for YouTube Shorts and Instagram Reels
- Full-screen pause overlay two seconds after a surface is detected
- Five-second countdown, then an automatic global **Home** action
- **Stay 1 more minute** snoozes the overlay for 60 seconds
- 5-second cooldown after an automatic exit to avoid instant re-triggering
- Detection enter/exit events and interventions logged under `OffloopDetect`

## Interruption behaviour

| Moment | What happens |
| --- | --- |
| Enter Shorts/Reels | Detection starts |
| +2 seconds | Full-screen pause overlay appears |
| +5 seconds | Overlay counts down and sends the phone home automatically |
| Tap **Leave now** | Sends the phone home immediately |
| Tap **Stay 1 more minute** | Overlay closes; re-arms after 60 seconds if still in the feed |
| Auto-exit | 5-second cooldown before the overlay can trigger again |

## Requirements

| Component | Version |
| --- | --- |
| JDK | 21 |
| Android SDK platform | 37 (`platforms;android-37.0`) |
| Build tools | 37.0.0 |
| Android Gradle Plugin | 9.4.0 |
| Gradle | 9.6.0 |
| Kotlin | 2.4.21 (overrides AGP's bundled 2.2.10) |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |

## Build and install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Enable detection

1. Open Offloop on the phone.
2. Tap **Enable detection**.
3. Select **Offloop** under installed accessibility services and allow it.
4. Return to the app; the status card should read **Detection is on**.

## Test detection

Watch the log while navigating the phone:

```bash
adb logcat -s OffloopDetect
```

Enter YouTube Shorts or Instagram Reels. A successful match looks like:

```
Detected YouTube Shorts (viewId=com.google.android.youtube:id/reel_recycler)
Pause overlay shown for YouTube Shorts
Auto-exit to home (overlay) from YouTube Shorts
```

After five seconds the phone returns to the home screen. Leaving the surface
logs the matching exit event.

If a surface is not recognised, the service dumps visible view IDs and class
names containing `reel`, `short`, `clip`, `viewer`, `player`, `pager` or
`recycler` at most every five seconds. Add new IDs to
`ShortVideoDetector.kt` and rebuild.

## Structure

```
app/src/main/
├── java/com/offloop/app/
│   ├── MainActivity.kt
│   ├── accessibility/
│   │   └── OffloopAccessibilityService.kt
│   ├── blocking/
│   │   └── PauseOverlay.kt
│   ├── detection/
│   │   ├── AppTarget.kt
│   │   ├── Detection.kt
│   │   └── ShortVideoDetector.kt
│   └── ui/
│       ├── HomeScreen.kt
│       └── theme/Theme.kt
├── res/
│   ├── drawable/
│   │   ├── bg_overlay_primary.xml
│   │   └── bg_overlay_secondary.xml
│   ├── layout/overlay_pause.xml
│   ├── values/strings.xml
│   └── xml/accessibility_service_config.xml
└── AndroidManifest.xml
```

## Design notes

- **View IDs over text.** Labels are localised and words like "Shorts" and
  "Reels" appear on the normal feed too. A visible, feature-specific view ID is
  the strongest signal available without screenshots or ML.
- **No screenshots.** The current detector reads only the accessibility node
  tree, which keeps battery use and privacy impact low.
- **Detection state is edge-triggered.** The service logs only when the surface
  changes, not on every scroll or content change.
- **No extra overlay permission.** The pause screen is a
  `TYPE_ACCESSIBILITY_OVERLAY` window owned by the running accessibility
  service, not a `SYSTEM_ALERT_WINDOW`.
- **The overlay freezes detection evaluation.** While it is visible, the
  service ignores content events so its own window cannot be mistaken for
  leaving the feed. A window change only dismisses it once the active window
  really belongs to another app, so the notification shade or a keyboard does
  not count as leaving.
- **Snooze and cooldown postpone, not cancel.** After **Stay 1 more minute**,
  the service keeps checking the surface and schedules the next pause at the
  end of the snooze instead of forgetting it. Opening Offloop itself counts as
  leaving the feed.

## Google Play caveat

Using `AccessibilityService` for a non-accessibility purpose requires a
prominent disclosure, affirmative consent, and the relevant Play Console
declaration. Validate Play eligibility before committing to that distribution
channel. This is not legal advice.
