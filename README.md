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
- Five-second countdown, then automatic return to the app's normal feed
- **Stay 1 more minute** snoozes the overlay for 60 seconds
- 5-second cooldown after an automatic exit to avoid instant re-triggering
- Detection enter/exit events and interventions logged under `OffloopDetect`

## Interruption behaviour

| Moment | What happens |
| --- | --- |
| Enter Shorts/Reels | Detection starts |
| +2 seconds | Full-screen pause overlay appears |
| +5 seconds | Overlay counts down and returns you to the app's normal feed |
| Tap **Return now** | Returns to the app's feed immediately |
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
Exiting to feed (overlay) from YouTube Shorts
Exit strategy: clicked YouTube Home tab
```

After five seconds you're returned to YouTube's Home feed (or Instagram's Home
feed). If the Home tab click fails, you'll see:

```
Exit strategy: Home tab not found, trying Back
Exit strategy: Back successful after 1 attempt(s)
```

If Back also fails after 3 attempts, the fallback is logged:

```
Exit strategy: Back failed, falling back to global Home
```

Leaving the surface logs the matching exit event.

If a surface is not recognised, the service dumps visible view IDs and class
names containing `reel`, `short`, `clip`, `viewer`, `player`, `pager` or
`recycler` at most every five seconds. Add new IDs to
`ShortVideoDetector.kt` and rebuild. Update the home tab view IDs and content
descriptions in `AppTarget.kt` if the navigation changes.

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
- **Periodic verification.** The service runs a lightweight check every 2.5
  seconds while an intervention is scheduled or the overlay is showing to
  verify the target app and surface are still active. If the user switches
  away, the overlay is dismissed and state is reset. The periodic check stops
  when idle to avoid battery cost.
- **Scoped to target packages.** The accessibility service is restricted to
  YouTube and Instagram only (least privilege). It does not receive events from
  payment apps, PIN screens or other apps.
- **Payment app safety.** Before taking action, the service confirms a
  short-form surface is still on screen. If the user jumped to a payment or
  shop app via notification or link and the leave event was missed, the overlay
  is hidden without closing the other app.
- **Back to feed, not home.** Instead of leaving the app, Offloop returns you
  to the app's normal feed while staying inside YouTube or Instagram. The exit
  strategy tries, in order: (a) find and click the app's Home tab in the bottom
  nav via accessibility nodes (by view ID or content description), (b) press
  Back up to 3 times with short delays until the short-form surface is no
  longer detected, (c) fall back to the global Home action if the surface is
  still detected after that. Home tab view IDs and content descriptions for
  each app are stored in AppTarget next to the detection view IDs. If a
  reel/short was opened from somewhere else (e.g. an Instagram DM or a shared
  link), Back naturally returns there. The 5-second cooldown still prevents
  instant re-triggering after returning to the feed; landing on the regular
  feed is not mistaken for a Shorts/Reels surface.

## Google Play caveat

Using `AccessibilityService` for a non-accessibility purpose requires a
prominent disclosure, affirmative consent, and the relevant Play Console
declaration. Validate Play eligibility before committing to that distribution
channel. This is not legal advice.
