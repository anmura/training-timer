# Training Timer

An offline workout timer for Android: build workouts from your own exercises, time them or count reps, add weights, repeat them in cycles, and get reminded when it's time to train.

![Training Timer screens](docs/screenshots/banner.png)

Build a workout ("complex") from your own exercise library. Give each exercise a time or a number of reps (and a weight if it uses one), group exercises into cycles that repeat several rounds, and choose how long to rest between them. Press **Start** and the app guides you through the whole session: a big countdown for timed sets, a **Done** button for reps sets, the next exercise shown during every rest, beeps and vibration at each switch, and controls on the lock screen. Set reminders as a notification or a full-screen alarm so you don't skip a day.

No account, no ads, no internet connection. All data stays on the phone.

> The screenshots in this README are taken from the app's design mockups, so some details and texts differ slightly from the current build.

## Features

**Exercise library**
- Exercises with a name (up to 60 characters) and an icon: 75+ built-in line icons in one style
- Categories with colours; an exercise can belong to several categories or to none
- Filter chips and a grouped list by category, plus search
- **Equipment** (dumbbells, mat, pull-up bar…) with its own icons: an exercise shows only the equipment and categories it has, as a short list; **Add** opens the full list with search and *New equipment* / *New category*
- Tap equipment in an exercise to edit it: name, icon and every exercise that uses it
- Complex cards show the equipment they need (*3 items needed*); **Start** opens an overview — total time, sets, cycles, the equipment you'll need and every exercise — with **Start workout**
- Filter the library (and the exercise pickers in the complex editor) by equipment as well as by category; each exercise shows small icons of its equipment
- Settings › Library: manage categories (name, colour, order) and equipment (name, icon)
- Ready-made exercises for mobility, strength, stretching and wrists, plus 12 pull-up variations
- An exercise can be marked **Uses weight** (dumbbells, kettlebell, barbell)

**Complex builder**
- Pick exercises with checkboxes (with search and category filters), then set the time of each one (−/+ in 5-second steps, or tap the time to type it)
- Switch any exercise between **Time** and **Reps**: a reps set has a target count (for example ×12) instead of a timer
- Cycles: a group of exercises repeated N rounds, with its own optional rest between exercises
- One rest time for the whole complex; no rest is added after the last exercise
- Hold and drag to reorder exercises, cycles and complexes
- Exercise menu: **Move into new cycle**, move into an existing cycle, move out of a cycle, **Remove from complex**
- Cycle menu: **Copy cycle** (same exercises, rounds and rest, placed right below) and Remove cycle
- Long exercise names are shown in full; the Time / Reps switch and the value sit on their own line
- The total workout time is calculated live (rep sets are shown separately, since they take as long as you need)

**Workout**
- Optional "get ready" countdown before the first exercise (0–15 s, set in Settings)
- Large timer ring with the full exercise name, progress line and time left
- The timer has the same size and place for every set of a workout: the name gets a fixed three-line block (long names use a smaller font), and room is kept only for the round ring, weight and reps controls the workout actually uses
- Names too long for three lines show a ⌄ button: tap the name to read it in full in a card over the timer, tap again to close
- **Progress line with cycles**: every set is a segment, cycles sit on their own band and rounds are spaced apart; the current set is white. In big complexes the block you are in keeps per-set detail and takes at least half the line, while other cycles shrink to one segment per round (or per cycle)
- In a cycle: a **round ring** around the timer with one segment per round (the current round is white) and a *2/3* badge in the header
- **New round** animation: the screen edges glow lime, the next segment draws in with a glow, *Round 2 of 3* appears on the timer, the badge flashes, and a distinct triple beep and buzz play
- Reps sets show the target count and a small stopwatch; tap the big **Done** button when you finish and the next rest starts
- Rest screen with the next exercise shown large and the two after it (with reps and weight)
- Pause, skip, previous and +10 s
- **Breathing reminders** (Settings, off by default): during exercises the timer gets a soft blue glow and *WORK* turns into *BREATHE* for two seconds, with a soft sound and a short buzz, every N seconds of exercise time (10–180 s; rests don't count)
- Beeps in the last 3 seconds and at every switch, vibration on each switch (both can be turned off)
- Keeps running with the screen locked; pause, skip, Done (for reps sets) and end from the notification
- The screen stays on during a workout

**Weights**
- Mark an exercise as **Uses weight** in its settings; choose kg or lb in Settings
- Set the weight in the complex: one weight, or in a cycle a weight **per round**
- Per round: **Fill all rounds** from a start weight and a step per round (e.g. 10 kg, +2 kg); the button shows the result before you tap it. Then change any round by hand; edited rounds are marked
- A cycle can mix weighted and bodyweight exercises; each weighted exercise has its own Same / Per round setting, and per-round weights fold into one line until you tap Edit
- Tap any weight to type it (decimals allowed); − / + change it by 1 kg or 2.5 lb
- During the workout the planned weight is shown with the set, in the Next card, on the rest screen and in the notification; it is not changed there

**Reminders**
- Settings › Reminders: any number of reminders, each with a time and repeat days (every day, weekdays, chosen days, or once)
- Remind about a specific workout (start it right from the reminder) or just "time to train" with your own message
- **Notification:** a banner with Start (or Choose workout) and Snooze
- **Alarm:** rings full screen over the lock screen, even in silent mode, with your chosen sound; Start workout, Snooze (5–30 min) or Dismiss. If nobody reacts in 5 minutes it snoozes itself
- **Repeat** mode: remind every X (15 min – 6 h) between two times, e.g. every 2 h from 09:00 to 18:00 on weekdays. Rings sit on a grid from *From* (e.g. 23:00, 23:15, 23:30 …), also when you switch it on in the middle of the window; nothing rings at or after *Until*. Start or Snooze on a repeating reminder restarts the step when the workout ends, and the finish screen shows when the next one comes
- Repeating reminders have **Show times**: the next ring and how many are left today; open it to see today's times (done ones struck through, the next one highlighted) and the next day's
- One-time reminders switch themselves off after they ring; everything is set again after a reboot, an app update or a clock change
- Complex cards on the home screen show their reminder, for example *Mon, Wed, Fri · 07:30*

**Import and export**
- Import exercises and complexes from a JSON file; existing names are skipped and a summary lists what was added, skipped or not readable
- Exercises a complex needs but your library doesn't have are created with a "?" icon
- Names longer than 60 characters are shortened
- Export all exercises, one complex, all complexes, or a full backup

## Screenshots

| Complexes | Exercise library | New exercise |
|---|---|---|
| <img src="docs/screenshots/01-complexes.png" width="240"> | <img src="docs/screenshots/02-exercises.png" width="240"> | <img src="docs/screenshots/03-new-exercise.png" width="240"> |

| Pick exercises | Complex builder | Add a cycle |
|---|---|---|
| <img src="docs/screenshots/04-pick-exercises.png" width="240"> | <img src="docs/screenshots/05-complex-builder.png" width="240"> | <img src="docs/screenshots/06-add-cycle.png" width="240"> |

| Exercise menu | Workout: timed set | Workout: reps set |
|---|---|---|
| <img src="docs/screenshots/14-time-or-reps.png" width="240"> | <img src="docs/screenshots/07-workout.png" width="240"> | <img src="docs/screenshots/15-reps-set.png" width="240"> |

| Many rounds | Rest and next exercise | Import / export |
|---|---|---|
| <img src="docs/screenshots/25-many-rounds.png" width="240"> | <img src="docs/screenshots/08-rest.png" width="240"> | <img src="docs/screenshots/09-import-export.png" width="240"> |

| Uses weight | Weight per round | Mixed cycle |
|---|---|---|
| <img src="docs/screenshots/21-weight-exercise.png" width="240"> | <img src="docs/screenshots/22-weight-per-round.png" width="240"> | <img src="docs/screenshots/23-weight-mixed-cycle.png" width="240"> |

| Weight in the workout | Settings | Import result |
|---|---|---|
| <img src="docs/screenshots/24-weight-workout.png" width="240"> | <img src="docs/screenshots/16-settings.png" width="240"> | <img src="docs/screenshots/10-import-result.png" width="240"> |

| Categories | Exercise categories | Manage categories |
|---|---|---|
| <img src="docs/screenshots/11-categories.png" width="240"> | <img src="docs/screenshots/12-exercise-categories.png" width="240"> | <img src="docs/screenshots/13-manage-categories.png" width="240"> |

| Reminders | New reminder | Notification |
|---|---|---|
| <img src="docs/screenshots/17-reminders.png" width="240"> | <img src="docs/screenshots/18-new-reminder.png" width="240"> | <img src="docs/screenshots/19-reminder-notification.png" width="240"> |

| Alarm |
|---|
| <img src="docs/screenshots/20-alarm.png" width="240"> |

## Install

Every push to `main` is built by GitHub Actions to check it compiles. New versions are published on the [Releases](../../releases) page when a release is started by hand (Actions › Build APK › Run workflow).

1. Open **Releases** on your Android phone and download the latest `TrainingTimer-*.apk`.
2. Open the file. The first time, Android asks you to allow installing apps from your browser or file manager.
3. Installing a newer version over the old one keeps all your data.

Requires Android 8.0 or newer.

### Permissions

| Permission | Why |
|---|---|
| Notifications | Workout controls on the lock screen and reminders. Asked when you start a workout or save a reminder |
| Exact alarms | Reminders fire on the minute. Granted automatically (Android 13+: `USE_EXACT_ALARM`) |
| Full-screen alarms | An alarm opens over the lock screen. On Android 14+ you may need to allow it; the Reminders screen shows an **Allow** button if so |
| Run at startup | Sets reminders again after the phone restarts |
| Foreground service, wake lock, vibration | Keep the timer and an alarm running with the screen off |

The app has no internet permission.

## Built-in content

On first launch the app adds:

- **Categories:** Мобильность (mobility), Силовые (strength), Растяжка (stretching)
- **Exercises:** joint warm-up, shoulder mobility, bodyweight strength, stretching and wrist exercises, each with its own icon
- **Climbing:** fingerboard hangs (open and half-crimp grip, 7/3 repeaters), lock-offs, Frenchies, offset pull-ups, toes to bar, hollow body, Y-T-W, finger extensions, band pull-aparts and external rotations, frog stretch — in a *Скалолазание* category, with a *Фингерборд* equipment item
- **Classics:** burpees, jumping jacks, high knees, mountain climbers, jump rope, planks and side planks, crunches, bicycle, Russian twist, flutter kicks, superman, dead bug, bird-dog, lunges, sumo squats, wall sit, calf raises, step-ups, push-up variations, bench dips, dumbbell and kettlebell basics (weighted) — with a *Кардио* category
- **Растяжка** complex: 51 stretches in order (≈32 min, 30 s each, 5 s rest), with new ones — child's pose, thread the needle, happy baby, cross-body arm, triceps, shin and toe stretches, ankle circles, legs up the wall
- **Pull-ups:** wide, close, chin-up, neutral grip, inverted (Australian), negative, band-assisted, dead hang, scapular, L-sit, archer and weighted
- **Complexes:** five base workouts that show every feature — single exercises, several cycles, repeats and per-cycle rest:

| Complex | Sets | Duration | Highlights |
|---|---|---|---|
| Утренняя зарядка (morning warm-up) | 14 | ~7 min | joint circles, a ×2 cycle |
| Силовая круговая (strength circuit) | 19 | ~12 min + reps | two cycles (×3 and ×2) with their own rests, push-ups and pull-ups in reps |
| Плечи: мобильность (shoulder mobility) | 16 | ~13 min | ×3 band cycle, left/right pairs |
| Растяжка всего тела (full-body stretch) | 22 | ~14 min | neck cycle with 3 s rest, side-by-side stretches |
| Запястья и предплечья (wrists and forearms) | 15 | ~10 min | ×3 and ×2 cycles |

The built-in programs live in [`app/src/main/assets`](app/src/main/assets) and use the same format as import files.

## Import file format

```json
{
  "version": 1,
  "categories": [
    { "name": "Растяжка", "color": "#C49BFF" }
  ],
  "exercises": [
    { "name": "Burpees", "icon": "jump", "categories": ["Силовые"] },
    { "name": "Side plank", "icon": "plank" },
    { "name": "Goblet squat", "icon": "squat", "usesWeight": true }
  ],
  "complexes": [
    {
      "name": "Morning HIIT",
      "rest": 15,
      "items": [
        { "exercise": "Burpees", "time": 30 },
        { "exercise": "Push-ups", "reps": 12 },
        { "exercise": "Goblet squat", "reps": 12, "weight": 16 },
        { "cycle": { "repeats": 3, "rest": 10, "items": [
          { "exercise": "Side plank", "time": 45 },
          { "exercise": "Goblet squat", "reps": 10, "weights": [12, 14, 16] }
        ] } }
      ]
    }
  ]
}
```

- All sections are optional; a plain list of exercise names is also accepted.
- `rest` and `time` are in seconds. A cycle's `rest` is optional; without it the complex's rest is used.
- Use `reps` instead of `time` for a set counted in repetitions; it ends when you tap Done.
- `weight` (a number, in the app's unit) sets the weight of a set; in a cycle `weights` gives one per round, e.g. `[10, 12, 14]`. Exercises can have `"usesWeight": true`.
- Categories and equipment are matched by name and created if they don't exist. Exercises can have `"equipment": ["Гантели"]`; a top-level `"equipment": [{ "name": "Гантели", "icon": "dumbbell" }]` sets icons.
- An unknown or missing `icon` becomes the "?" icon.
- Exercises and complexes whose names already exist are skipped.
- Reminders are not part of export files: they belong to the phone they were set on.

Icon names: `plank pushup squat jump jacks run lunge stretch dumbbell kettlebell rope abs bike timer heart wallslide facepull dislocate handslock chestwall chestwallhigh thoracic pullup dips pistol legraise wristcurl wristext bartilt sidetwist kneeschest hamstring pigeon butterfly forearm neck shoulderroll armcircles elbowcircles wristcircles hipcircles kneecircles catcow glutebridge pike abwheel sidelunge forwardfold scapula cobra legswing legreach sidebend calf quad lyingquad neckside neckforward scm bartiltfwd bartiltback barrotate fingerroll pullupwide pullupclose chinup pullupneutral invertedrow pullupneg pullupband deadhang scappull lsitpull archerpull pullupweight unknown`

## Project structure

Kotlin and Jetpack Compose, no third-party libraries beyond AndroidX.

```
app/src/main/java/com/trainingtimer/app/
├── data/       models, JSON storage (Repo), import/export, icon definitions
├── ui/         screens: home, exercise and complex editors (incl. weights), workout, settings, categories, reminders
├── workout/    workout engine (timing, beeps, vibration) and the foreground service
└── reminders/  scheduling (AlarmManager), notification, alarm service and full-screen alarm screen
app/src/main/assets/    built-in exercises and complexes
docs/screenshots/       images used in this README
```

All data is stored in one JSON file in the app's private storage. Icons are SVG-style path strings drawn on a 24×24 grid, so new icons are a single line of code.

## Build

Open the project in Android Studio, or run:

```
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/`. CI builds with JDK 17 and Gradle 8.9 (see [`.github/workflows/build.yml`](.github/workflows/build.yml)).
