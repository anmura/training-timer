# Training Timer

An Android interval-training app: build complexes of exercises with per-exercise times, cycles with repeats and rest between exercises, then run them with a big countdown, pause/skip controls and beeps/vibration.

## Install

Every push to `main` is built by GitHub Actions. Open **Releases** on your phone, download the latest `TrainingTimer-*.apk` and open it. Android will ask you to allow installing apps from your browser or file manager the first time.

Requires Android 8.0 or newer.

## Import file format

```json
{
  "version": 1,
  "exercises": [
    { "name": "Burpees", "icon": "jump" },
    { "name": "Side plank", "icon": "plank" }
  ]
}
```

Built-in icon names: plank, pushup, squat, jump, jacks, run, lunge, stretch, dumbbell, kettlebell, rope, abs, bike, timer, heart. Exercises whose names already exist are skipped. A full backup from the app also contains a `complexes` list and can be imported the same way.

## Build locally

Open the folder in Android Studio, or run `./gradlew assembleRelease`.
