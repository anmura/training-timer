package com.trainingtimer.app.data

/** Built-in exercise icons, drawn from SVG-style path data on a 24x24 grid. */
data class IconDef(val key: String, val label: String, val path: String)

object ExerciseIcons {
    const val DEFAULT = "dumbbell"

    /** Question mark, used for exercises created automatically on import. */
    const val UNKNOWN = "unknown"

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
        IconDef("wallslide", "Wall slide", "M21 2v20M9.5 5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M11 7v8M11 15l-2 6M11 15l2 6M11 10l-4-1-1-5M11 10l4-1 1-5M18 12V7M16.5 8.5L18 7l1.5 1.5"),
        IconDef("facepull", "Face pull", "M4.5 5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M6 7v8M6 15l-2 6M6 15l2 6M6 9l4 1 1-3M11 7h8M20 4v6"),
        IconDef("dislocate", "Band dislocates", "M10.5 9a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M12 11v5M12 16l-2 5M12 16l2 5M12 12L6 7M12 12l6-5M6 7Q12 1 18 7"),
        IconDef("handslock", "Hands behind back", "M11.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M13 6v8M13 14l-1 7M13 14l2 7M13 8l-4 4 3 2M9.5 12.5h.01"),
        IconDef("chestwall", "Chest stretch, wall", "M3 2v20M12.5 5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M14 7v8M14 15l-2 6M14 15l2 6M14 9H6V5"),
        IconDef("chestwallhigh", "Chest stretch, high", "M3 2v20M12.5 6a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M14 8v7M14 15l-2 6M14 15l2 6M14 9L7 6V2"),
        IconDef("thoracic", "Thoracic extension", "M2 21h20M9.5 17.5a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0M2.0 13a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M5 13q7-3 14 2l2 5M8 13l-3 4"),
        IconDef("pullup", "Pull-up", "M3 3h18M8 3l2 6M16 3l-2 6M10.5 7a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M10 9h4M12 9v7M12 16l-2 5M12 16l2 5"),
        IconDef("dips", "Dips", "M3 13h6M15 13h6M10.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M12 6v7M12 7l-3 3-1 3M12 7l3 3 1 3M12 13l-2 4 2 4"),
        IconDef("pistol", "Pistol squat", "M6.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M8 6l2 7M8.5 8.5H17M10 13l4 1-2 6M10 13l11-1M9 21h6"),
        IconDef("legraise", "Leg raise", "M3 21h18M2.5 17a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M6 18h7l4-9M8 18l-1-3"),
        IconDef("wristcurl", "Wrist curl", "M2 17h10M14 13v8M12.5 14v6M15.5 14v6M18 10a5 5 0 0 0-8-4M10 3.5V6h2.5"),
        IconDef("wristext", "Wrist extension", "M2 7h10M14 3v8M12.5 4v6M15.5 4v6M18 14a5 5 0 0 1-8 4M10 20.5V18h2.5"),
        IconDef("bartilt", "Bar tilts", "M5 19L18 6M15 3l6 6M14.5 5.5l4 4M7.5 15.5l2 2M3 10a8 8 0 0 1 4-5M5.5 3.5L7 5l-1.5 1.5"),
        IconDef("sidetwist", "Side twist", "M2 21h20M2.5 16a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M6 17h7l3-4 3 4M8 17l2-7"),
        IconDef("kneeschest", "Knees to chest", "M2 21h20M2.5 17a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M6 18h6l3-6-4-1M8 17l4-4"),
        IconDef("hamstring", "Hamstring stretch", "M3 20h18M13.5 9a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M5 20l8-8M5 20h15M11 14l8 4"),
        IconDef("pigeon", "Pigeon", "M2 21h20M8.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M10 6v8M10 14l-6 3 5 2M10 14l11 5M10 9l-2 4M10 9l3 4"),
        IconDef("butterfly", "Butterfly", "M10.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M12 6v9M12 15L5 18l7 2 7-2z M12 9l-3 8M12 9l3 8"),
        IconDef("forearm", "Forearm stretch", "M2 14h12l3-6M16 4l-1 5M18 4l-1 5M13 20l3-7"),
        IconDef("neck", "Head tilts & turns", "M9 8a3 3 0 1 0 6 0a3 3 0 1 0 -6 0M12 11v2M5 15q7-3 14 0M4.5 9a7.5 7.5 0 0 1 2.5-5.5M5 3.5h2v2M19.5 9a7.5 7.5 0 0 0-2.5-5.5M19 3.5h-2v2"),
        IconDef("shoulderroll", "Shoulder circles", "M10.5 5a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0M12 7v8M12 15l-2 6M12 15l2 6M8 9h8M8 9l-1 6M16 9l1 6M4.5 7.5a2.5 2.5 0 1 0 5.0 0a2.5 2.5 0 1 0 -5.0 0M14.5 7.5a2.5 2.5 0 1 0 5.0 0a2.5 2.5 0 1 0 -5.0 0"),
        IconDef("armcircles", "Arm circles", "M10.5 6a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0M12 8v6M12 14l-2 7M12 14l2 7M12 10H6M12 10h6M1.5 10a2.5 2.5 0 1 0 5.0 0a2.5 2.5 0 1 0 -5.0 0M17.5 10a2.5 2.5 0 1 0 5.0 0a2.5 2.5 0 1 0 -5.0 0"),
        IconDef("elbowcircles", "Elbow circles", "M10.5 5a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0M12 7v8M12 15l-2 6M12 15l2 6M12 9l-5 3 3-5M12 9l5 3-3-5M4.3 12a2.2 2.2 0 1 0 4.4 0a2.2 2.2 0 1 0 -4.4 0M15.3 12a2.2 2.2 0 1 0 4.4 0a2.2 2.2 0 1 0 -4.4 0"),
        IconDef("wristcircles", "Wrist circles", "M10 22v-8M14 22v-8M10 14a2 2 0 0 1 4 0M4.5 9a7.5 7.5 0 0 1 15 0M19.5 9l-2.5-1M19.5 9l1-2.5"),
        IconDef("hipcircles", "Hip circles", "M10.5 4a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0M12 6v7M12 13l-3 8M12 13l3 8M12 8l-4 4M12 8l4 4M4 13a8 2.5 0 1 0 16 0a8 2.5 0 1 0 -16 0"),
        IconDef("kneecircles", "Knee circles", "M10.5 4a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0M12 6l1 6M13 12l-4 4 1 5M13 12l2 4-1 5M12 8l-3 8M12 8l3 8M5 16a7 2 0 1 0 14 0a7 2 0 1 0 -14 0"),
        IconDef("catcow", "Cat–cow", "M2 21h20M5 21v-6M18 21v-6M5 15q6.5-7 13 0M1.5 13a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0"),
        IconDef("glutebridge", "Glute bridge", "M2 21h20M2.5 19a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0M6 20l7-6h4l1 7M7 19.5l-1 1.5"),
        IconDef("pike", "Pike push-up", "M2 21h20M4 21l7-11M11 10l8 11M5.0 14.5a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0M8 14.5l-2 6.5"),
        IconDef("sidelunge", "Side lunge", "M10.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M12 6v6M12 12l-5 3v6M12 12l8 9M12 8l-4 6M2 21h20"),
        IconDef("forwardfold", "Seated forward fold", "M2 21h20M4 19h16M4 19l8-6M12.5 11.5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M9 15.5l10 3"),
        IconDef("scapula", "Cross-body arm", "M10.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M12 6v8M12 14l-2 7M12 14l2 7M14.5 8.5H5M12 9.5l-3 2.5-1-3"),
        IconDef("cobra", "Cobra", "M2 21h20M12 20h9M12 20l-6-7M3.5 10.5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M7 13.5v6.5"),
        IconDef("legswing", "Leg swing", "M8.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M10 6v7M10 13v8M10 13l6 5M10 8l-4 3M10 8l4 2M14 21q5-1 6-6M20 15l-2.5 0.5M20 15l0.5 2.5"),
        IconDef("legreach", "Reach to leg", "M2 21h20M12 19.5L3 20.5M12 19.5l9 1M12 19.5l4-7M15.5 10.5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M15 14l5 5"),
        IconDef("sidebend", "Side bend", "M6 5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M12 13q0-4-3.5-6.5M9 7q3-4 8-2M12 13l-2 8M12 13l2 8M10 10l3 1.5"),
        IconDef("calf", "Calf stretch", "M21 2v20M2 21h18M5 21l7-8 3-6M14.5 5a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M14 9h6M12 13l4 3v5"),
        IconDef("quad", "Quad stretch", "M10.5 4a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M12 6v7M12 13v8M12 13l1.5 5 3-5M12 8l4 5M12 8l-4 3"),
        IconDef("lyingquad", "Lying quad stretch", "M2 21h20M2.5 17a1.5 1.5 0 1 0 3 0a1.5 1.5 0 1 0 -3 0M6 18l7 1h6l-3-6M7 18l9-5"),
        IconDef("neckside", "Neck side tilt", "M7 13h10M12 13v8M12 13l1.6-3.6M12.6 7a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M17 13l2-5-2.5-3"),
        IconDef("neckforward", "Neck forward", "M10 11v10M10 11l2-1M12 9a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M10 13l5-1 .5-5"),
        IconDef("scm", "Neck front stretch", "M6 14h12M12 14l1-4M12 7.5a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M9 14l2-2M16.5 3.5a4 4 0 0 1 2.5 3.5M19 7l1.5-1.5M19 7l-1.8-.8"),
        IconDef("bartiltfwd", "Bar tilt forward", "M2 15h9M11 13h3v4h-3zM12.5 3v18M16 5a8 8 0 0 1 3.5 6M19.5 11l-.2-3M19.5 11l-2.8-1"),
        IconDef("bartiltback", "Bar tilt back (hammer)", "M2 15h9M11 13h3v4h-3zM12.5 3v18M9 5a8 8 0 0 0-3.5 6M5.5 11l.2-3M5.5 11l2.8-1"),
        IconDef("barrotate", "Bar rotation", "M3 12h18M10 10h4v4h-4zM8 7a5 5 0 0 1 8 0M16 7V4M16 7h-3M16 17a5 5 0 0 1-8 0M8 17v3M8 17h3"),
        IconDef("fingerroll", "Finger roll", "M2 17h8q4 0 5.5-3M15 11.5a2 2 0 1 0 4 0a2 2 0 1 0 -4 0M10 17q1 3 4 3M11 8a4 4 0 0 1 4-2M15 6l-2-1M15 6l-1 2"),
        IconDef("unknown", "Unknown", "M8.5 8.5a3.5 3.5 0 1 1 5 3.2c-1 .5-1.5 1.2-1.5 2.3v1M12 19h.01"),
        IconDef("abwheel", "Ab wheel", "M2 21h20M5 21l2-6 7-2 4 3M14.0 10.5a1.5 1.5 0 1 0 3.0 0a1.5 1.5 0 1 0 -3.0 0M17.0 18.5a2.5 2.5 0 1 0 5.0 0a2.5 2.5 0 1 0 -5.0 0"),
    )

    private val byKey = all.associateBy { it.key }

    fun has(key: String): Boolean = byKey.containsKey(key)

    fun path(key: String): String = (byKey[key] ?: byKey.getValue(UNKNOWN)).path
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
    const val GRIP = "M9 6h.01M9 12h.01M9 18h.01M15 6h.01M15 12h.01M15 18h.01"
    const val LIST = "M4 5h16M4 12h16M4 19h10"
    const val DUMBBELL = "M6 7v10M3 9v6M18 7v10M21 9v6M6 12h12"
    const val SKIPPED = "M5 5l14 14M12 22a10 10 0 1 0 0-20a10 10 0 1 0 0 20"
    const val WARNING = "M12 9v4M12 17h.01M10.3 3.9L2 18a2 2 0 0 0 1.7 3h16.6a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"
}
