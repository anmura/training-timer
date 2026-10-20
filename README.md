# Training Timer

An offline interval timer for Android that turns your own exercises, cycles and rests into a hands-free guided workout.

![Training Timer screens](docs/screenshots/banner.png)

Build a workout ("complex") from your own exercise library, set a time for every exercise, group exercises into cycles that repeat several rounds, and choose how long to rest between them. Press **Start** and the app guides you through the whole session: a big countdown, the next exercise shown during every rest, beeps and vibration at each switch, and controls on the lock screen.

No account, no ads, no internet connection. All data stays on the phone.

> The screenshots in this README are taken from the app's design mockups, so some details and texts differ slightly from the current build.

## Features

**Exercise library**
- Exercises with a name and an icon: 60+ built-in line icons in one style
- Categories with colours; an exercise can belong to several categories or to none
- Filter chips and a grouped list by category, plus search
- Ready-made exercises for mobility, strength, stretching and wrists

**Complex builder**
- Pick exercises with checkboxes, then set the time of each one (−/+ in 5-second steps, or tap the time to type it)
- Switch any exercise between **Time** and **Reps**: a reps set has a target count (for example ×12) instead of a timer
- Cycles: a group of exercises repeated N rounds, with its own optional rest between exercises
- One rest time for the whole complex; no rest is added after the last exercise
- Hold and drag to reorder exercises, cycles and complexes; move exercises into and out of cycles
- The total workout time is calculated live (reps sets are shown separately, since they take as long as you need)

**Workout**
- Optional "get ready" countdown before the first exercise (0–15 s, set in Settings)
- Large timer ring, current round (for example *Round 2/3*), progress for every set and time left
- Reps sets show the target count and a small stopwatch; tap the big **Done** button when you finish and the next rest starts
- Rest screen with the next exercise shown large and the two after it
- Pause, skip, previous and +10 s
- Beeps in the last 3 seconds and at every switch, vibration on each switch (both can be turned off)
- Keeps running with the screen locked; pause, skip, Done (for reps sets) and end from the notification
- The screen stays on during a workout

**Reminders**
- Settings › Reminders: set a time and repeat days (or once)
- Remind about a specific workout (start it right from the reminder) or just "time to train"
- Choose a notification (Start / Snooze) or an alarm that rings full screen, even on silent, with Start, Snooze and Dismiss
- Alarms survive reboots and app updates; complex cards show their reminder

**Import and export**
- Import exercises and complexes from a JSON file; existing names are skipped and a summary lists what was added, skipped or not readable
- Exercises a complex needs but your library doesn't have are created with a "?" icon
- Export all exercises, one complex, all complexes, or a full backup

## Screenshots

| Complexes | Exercise library | New exercise |
|---|---|---|
| <img src="docs/screenshots/01-complexes.png" width="240"> | <img src="docs/screenshots/02-exercises.png" width="240"> | <img src="docs/screenshots/03-new-exercise.png" width="240"> |

| Pick exercises | Complex builder | Add a cycle |
|---|---|---|
| <img src="docs/screenshots/04-pick-exercises.png" width="240"> | <img src="docs/screenshots/05-complex-builder.png" width="240"> | <img src="docs/screenshots/06-add-cycle.png" width="240"> |

| Workout | Rest and next exercise | Import / export |
|---|---|---|
| <img src="docs/screenshots/07-workout.png" width="240"> | <img src="docs/screenshots/08-rest.png" width="240"> | <img src="docs/screenshots/09-import-export.png" width="240"> |

| Import result | Categories | Exercise categories |
|---|---|---|
| <img src="docs/screenshots/10-import-result.png" width="240"> | <img src="docs/screenshots/11-categories.png" width="240"> | <img src="docs/screenshots/12-exercise-categories.png" width="240"> |

| Manage categories |
|---|
| <img src="docs/screenshots/13-manage-categories.png" width="240"> |

## Install

Every push to `main` is built by GitHub Actions and published on the [Releases](../../releases) page.

1. Open **Releases** on your Android phone and download the latest `TrainingTimer-*.apk`.
2. Open the file. The first time, Android asks you to allow installing apps from your browser or file manager.
3. Installing a newer version over the old one keeps all your data.

Requires Android 8.0 or newer.

## Built-in content

On first launch the app adds:

- **Categories:** Мобильность (mobility), Силовые (strength), Растяжка (stretching)
- **Exercises:** joint warm-up, shoulder mobility, bodyweight strength, stretching and wrist exercises, each with its own icon
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
    { "name": "Side plank", "icon": "plank" }
  ],
  "complexes": [
    {
      "name": "Morning HIIT",
      "rest": 15,
      "items": [
        { "exercise": "Burpees", "time": 30 },
        { "exercise": "Push-ups", "reps": 12 },
        { "cycle": { "repeats": 3, "rest": 10, "items": [
          { "exercise": "Side plank", "time": 45 }
        ] } }
      ]
    }
  ]
}
```

- All sections are optional; a plain list of exercise names is also accepted.
- `rest` and `time` are in seconds. A cycle's `rest` is optional; without it the complex's rest is used.
- Use `reps` instead of `time` for a set counted in repetitions; it ends when you tap Done.
- Categories are matched by name and created if they don't exist.
- An unknown or missing `icon` becomes the "?" icon.
- Exercises and complexes whose names already exist are skipped.

Icon names: `plank pushup squat jump jacks run lunge stretch dumbbell kettlebell rope abs bike timer heart wallslide facepull dislocate handslock chestwall chestwallhigh thoracic pullup dips pistol legraise wristcurl wristext bartilt sidetwist kneeschest hamstring pigeon butterfly forearm neck shoulderroll armcircles elbowcircles wristcircles hipcircles kneecircles catcow glutebridge pike abwheel sidelunge forwardfold scapula cobra legswing legreach sidebend calf quad lyingquad neckside neckforward scm bartiltfwd bartiltback barrotate fingerroll unknown`

## Project structure

Kotlin and Jetpack Compose, no third-party libraries beyond AndroidX.

```
app/src/main/java/com/trainingtimer/app/
├── data/       models, JSON storage (Repo), import/export, icon definitions
├── ui/         screens: home, exercise and complex editors, workout, settings, categories
└── workout/    workout engine (timing, beeps, vibration) and the foreground service
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
