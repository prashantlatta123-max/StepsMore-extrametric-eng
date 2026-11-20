# StepsMore

**Developer:** Prashant Latta  
**Company:** SAROTECH  
**Package:** `com.sarotech.stepsmore`

## Key Features
- **AMOLED Monochrome UI:** Pure `#000000` canvas, `#0E0E0E` flat rounded cards, monochrome Material 3 switches, and edge-to-edge dark system bars.
- **Real-Time Step Tracking:** Hardware `TYPE_STEP_COUNTER` foreground service with auto-resume on boot (`BootReceiver`) and live progress notifications.
- **Tap-to-Inspect Hourly Breakdown (Custom Feature):** Tap any day's bar in the **Past 7 Days** chart or any date in the **Monthly Calendar** to expand an inline hourly breakdown card showing a 24-hour interactive graph, Morning/Afternoon/Evening/Night summary, and active time slots.
- **Smart Timed Pause:** Tap the bottom-right FAB to pause/resume; long-press for 30 min / 1 hr / 2 hr timed pause.
- **Achievements, Daily Goal, Settings & JSON Backup:** Full offline local storage using Room (`daily_steps` + `hourly_steps`) and DataStore Preferences.

## How to Build
1. Extract `StepsMore-Android.zip`.
2. Open the `StepsMore` folder in **Android Studio** (Ladybug or newer) or push to GitHub / Codespaces.
3. Let Gradle sync (JDK 17, Android SDK 35).
4. Run on a physical Android device to test live hardware step counting.


## Sensor engine and licensing

The hardware-first sensor tracking pipeline is adapted from the Stepsy project
(https://github.com/nvllz/stepsy), licensed under GNU GPL-3.0. Stepsy credits
Motionmate by 0xf4b1. The integration has been modified for StepsMore's Room
database, DataStore preferences, notification flow, and existing Compose UI.
See `THIRD_PARTY_NOTICES.md` and `LICENSE`.

Because this is a combined derivative work incorporating GPL-3.0 code, this
StepsMore source distribution is provided under GPL-3.0. Preserve the license
and notices when redistributing.


## Appearance themes

StepsMore includes three selectable appearance modes in Settings → Appearance:

- **Black** — deep black surfaces for an AMOLED-friendly interface.
- **Snow White** — a clean light interface.
- **Glass** — layered, translucent glass-inspired surfaces over a dark backdrop.

The Accent color setting provides preset colors and a custom HEX color field. Theme and accent selections are saved locally with Android DataStore preferences and restored when the app is reopened.
