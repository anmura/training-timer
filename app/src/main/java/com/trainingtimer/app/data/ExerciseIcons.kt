package com.trainingtimer.app.data

/** Built-in exercise icons, drawn from SVG-style path data on a 24x24 grid. */
data class IconDef(val key: String, val label: String, val path: String)

object ExerciseIcons {
    const val DEFAULT = "dumbbell"

    val all: List<IconDef> = listOf(
        IconDef("plank", "Plank", "M3.5 11a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M7 12l12 3M9 13l-1 4M19 15v2M3 19h18"),
        IconDef("pushup", "Push-up", "M3.5 10a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M7 11l13 3M10 12v5M20 14v3M3 19h18"),
        IconDef("squat", "Squat", "M10.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M12 6v5l-4 2v7M12 11l4 2-1 7M8 8h9"),
        IconDef("jump", "Jump", "M10.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M12 6v8M12 8l-6-4M12 8l6-4M12 14l-5 6M12 14l5 6"),
        IconDef("jacks", "Jumping jacks", "M10.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M12 6v6M12 8l-5 3M12 8l5 3M12 12l-4 8M12 12l4 8"),
        IconDef("run", "Run", "M12.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M13 7l-3 5 4 2-2 6M10 12l-4-1M13 7l3 3 3-1"),
        IconDef("lunge", "Lunge", "M10.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M12 6v6M12 12l-5 3v5M12 12l5 1v7M9 9h6"),
        IconDef("stretch", "Stretch", "M10.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M12 6v7M12 8l-7 2M12 8l7-2M12 13l-5 7M12 13l5 7"),
        IconDef("dumbbell", "Dumbbell", "M6 7v10M3 9v6M18 7v10M21 9v6M6 12h12"),
        IconDef("kettlebell", "Kettlebell", "M9 9V7a3 3 0 0 1 6 0v2M7 9h10l1.5 10h-13z"),
        IconDef("rope", "Jump rope", "M5 3v7M19 3v7M5 10c0 9 14 9 14 0"),
        IconDef("abs", "Abs", "M8 4h8v16H8zM8 9.5h8M8 14.5h8M12 4v16"),
        IconDef("bike", "Bike", "M2.5 17a3 3 0 1 0 6 0a3 3 0 1 0 -6 0M15.5 17a3 3 0 1 0 6 0a3 3 0 1 0 -6 0M5.5 17l4-7h5l4 7M9.5 10l-1-3h-2M14.5 10l1.5-3h2"),
        IconDef("timer", "Timer", "M12 13l3-3M9 2h6M12 22a8 8 0 1 0 0-16a8 8 0 1 0 0 16"),
        IconDef("heart", "Heart", "M12 20s-7-4.5-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.5-7 10-7 10z"),
    )

    private val byKey = all.associateBy { it.key }

    fun has(key: String): Boolean = byKey.containsKey(key)

    fun path(key: String): String = (byKey[key] ?: byKey.getValue(DEFAULT)).path
}

/** Interface icons in the same path format. */
object UiIcons {
    const val PLUS = "M12 5v14M5 12h14"
    const val CLOSE = "M6 6l12 12M18 6L6 18"
    const val BACK = "M15 6l-6 6 6 6"
    const val FORWARD = "M9 6l6 6-6 6"
    const val CHECK = "M5 12l5 5 9-10"
    const val MORE = "M12 5h.01M12 12h.01M12 19h.01"
    const val SEARCH = "M11 18a7 7 0 1 0 0-14a7 7 0 1 0 0 14M20 20l-4-4"
    const val SETTINGS = "M4 6h9M17 6h3M15 4v4M4 12h3M11 12h9M9 10v4M4 18h11M19 18h1M17 16v4"
    const val PLAY = "M7 4.5l13 7.5-13 7.5z"
    const val PAUSE = "M6 4h4.5v16H6zM13.5 4H18v16h-4.5z"
    const val PREV = "M18 5L8 12l10 7zM5 5h2.5v14H5z"
    const val NEXT = "M6 5l10 7-10 7zM16.5 5H19v14h-2.5z"
    const val REPEAT = "M17 2l3 3-3 3M4 11V9a4 4 0 0 1 4-4h12M7 22l-3-3 3-3M20 13v2a4 4 0 0 1-4 4H4"
    const val TIMER = "M12 13l3-3M9 2h6M12 22a8 8 0 1 0 0-16a8 8 0 1 0 0 16"
    const val IMPORT_EXPORT = "M7 4v12M3 12l4 4 4-4M17 20V8M13 12l4-4 4 4"
    const val DOWNLOAD = "M12 3v12M7 10l5 5 5-5M5 21h14"
    const val UPLOAD = "M12 15V3M7 8l5-5 5 5M5 21h14"
    const val BACKUP = "M4 7h16v13H4zM4 7l2-3h12l2 3M9 12h6"
    const val IMAGE = "M4 5h16v14H4zM4 16l5-5 4 4 3-3 4 4M15 9h.01"
    const val LIST = "M4 5h16M4 12h16M4 19h10"
    const val DUMBBELL = "M6 7v10M3 9v6M18 7v10M21 9v6M6 12h12"
    const val SKIPPED = "M5 5l14 14M12 22a10 10 0 1 0 0-20a10 10 0 1 0 0 20"
    const val WARNING = "M12 9v4M12 17h.01M10.3 3.9L2 18a2 2 0 0 0 1.7 3h16.6a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"
}
