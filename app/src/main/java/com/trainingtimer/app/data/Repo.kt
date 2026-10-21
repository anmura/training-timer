package com.trainingtimer.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/** Keeps all app data in memory and in one JSON file on the phone. */
object Repo {
    private var appContext: Context? = null
    private val _state = MutableStateFlow(AppData())
    val state: StateFlow<AppData> = _state

    private val context: Context get() = appContext ?: error("Repo.init was not called")

    val iconsDir: File
        get() = File(context.filesDir, "icons").apply { mkdirs() }

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val file = dataFile()
        val loaded = if (file.exists()) {
            try {
                Codec.fromJson(JSONObject(file.readText()))
            } catch (e: Exception) {
                null
            }
        } else null
        if (loaded != null) {
            _state.value = loaded
        } else {
            val fresh = AppData()
            _state.value = fresh
            write(fresh)
        }
        installDefaultProgram()
        installAssetProgram("program_zaryadka_v1", "program_zaryadka.json")
        installCategories()
        installAssetProgram("program_stretch2_v1", "program_stretch2.json")
        installAssetProgram("program_basics_v1", "program_basics.json")
        installAssetProgram("program_pullups_v1", "program_pullups.json")
    }

    /**
     * Once per install: create Мобильность / Силовые / Растяжка and sort the known exercises into them.
     * An exercise can land in more than one category; unknown exercises stay uncategorised.
     */
    private fun installCategories() {
        val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
        if (prefs.getBoolean("categories_v1", false)) return
        if (_state.value.categories.isEmpty()) {
            val mobility = Category(newId(), "Мобильность", "#4ADE9B")
            val strength = Category(newId(), "Силовые", "#FF9F43")
            val stretch = Category(newId(), "Растяжка", "#C49BFF")
            val m = mobility.id
            val s = strength.id
            val r = stretch.id
            // Matched by the start of the (lower-case) exercise name.
            val rules = listOf(
                "наклоны и повороты головы" to listOf(m),
                "круги" to listOf(m),
                "кошка" to listOf(m, r),
                "мобильные выпады" to listOf(m),
                "скольжение по стене" to listOf(m),
                "выкруты с резинкой" to listOf(m, r),
                "тяга резинкой к лицу" to listOf(s, m),
                "замок за спиной" to listOf(r, m),
                "грудной отдел на опоре" to listOf(m, r),
                "грудная у стены" to listOf(r),
                "подтягивания" to listOf(s),
                "брусья" to listOf(s),
                "отжимания" to listOf(s),
                "узкие отжимания" to listOf(s),
                "пайк-отжимания" to listOf(s),
                "приседания на одной ноге" to listOf(s),
                "приседание на одной ноге" to listOf(s),
                "приседание на двух ногах" to listOf(s),
                "подъёмы ног" to listOf(s),
                "сгибание запястий" to listOf(s),
                "разгибание запястий" to listOf(s),
                "наклоны грифа" to listOf(s),
                "ягодичный мостик" to listOf(s),
                "выкаты на ролике" to listOf(s),
                "выпад на" to listOf(s),
                "растяжка в выпаде" to listOf(r, m),
                "выпад" to listOf(r),
                "скручивание на" to listOf(r),
                "колени к груди" to listOf(r),
                "задняя поверхность бедра" to listOf(r),
                "голубь" to listOf(r),
                "бабочка" to listOf(r),
                "предплечья" to listOf(r),
                "burpees" to listOf(s),
                "jumping jacks" to listOf(m),
            )
            update { d ->
                d.copy(
                    categories = listOf(mobility, strength, stretch),
                    exercises = d.exercises.map { e ->
                        val n = e.name.trim().lowercase()
                        val match = rules.firstOrNull { n.startsWith(it.first) }?.second
                        if (match == null) e else e.copy(categories = (e.categories + match).distinct())
                    },
                )
            }
        }
        prefs.edit().putBoolean("categories_v1", true).apply()
    }

    // ---------- categories ----------

    fun categoryNameExists(name: String, exceptId: String?): Boolean {
        val n = name.trim().lowercase()
        return _state.value.categories.any { it.id != exceptId && it.name.trim().lowercase() == n }
    }

    fun nextCategoryColor(): String {
        val used = _state.value.categories.map { it.color }.toSet()
        return CategoryColors.palette.firstOrNull { it !in used }
            ?: CategoryColors.palette[_state.value.categories.size % CategoryColors.palette.size]
    }

    fun addCategory(name: String, color: String): Category {
        val c = Category(newId(), name.trim(), color)
        update { it.copy(categories = it.categories + c) }
        return c
    }

    fun renameCategory(id: String, name: String) = update { d ->
        d.copy(categories = d.categories.map { if (it.id == id) it.copy(name = name.trim()) else it })
    }

    fun setCategoryColor(id: String, color: String) = update { d ->
        d.copy(categories = d.categories.map { if (it.id == id) it.copy(color = color) else it })
    }

    fun deleteCategory(id: String) = update { d ->
        d.copy(
            categories = d.categories.filter { it.id != id },
            exercises = d.exercises.map { e -> if (id in e.categories) e.copy(categories = e.categories - id) else e },
        )
    }

    fun moveCategory(from: Int, to: Int) = update { d ->
        if (from !in d.categories.indices || to !in d.categories.indices) {
            d
        } else {
            val list = d.categories.toMutableList()
            list.add(to, list.removeAt(from))
            d.copy(categories = list)
        }
    }

    /**
     * Once per install: swap the old English sample exercises for the built-in Russian program
     * (exercises + complexes from assets/default_program.json). Sample exercises that are used in
     * one of the user's complexes are kept; anything with an existing name is skipped.
     */
    private fun installDefaultProgram() {
        val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
        if (prefs.getBoolean("default_program_v1", false)) return
        val demo = setOf("Burpees", "Jumping jacks", "Lunges", "Mountain climbers", "Plank", "Push-ups", "Sit-ups", "Squats")
        update { d ->
            val used = d.complexes.flatMap { it.exerciseIds() }.toSet()
            d.copy(exercises = d.exercises.filter { !(it.name in demo && it.id !in used) })
        }
        try {
            val text = context.assets.open("default_program.json").bufferedReader().use { it.readText() }
            ImportExport.importJson(text)
        } catch (e: Exception) {
            // Keep going without the built-in program.
        }
        prefs.edit().putBoolean("default_program_v1", true).apply()
    }

    /** Adds a built-in program (exercises + complexes) from assets once per install. */
    private fun installAssetProgram(flag: String, asset: String) {
        val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
        if (prefs.getBoolean(flag, false)) return
        try {
            val text = context.assets.open(asset).bufferedReader().use { it.readText() }
            ImportExport.importJson(text)
        } catch (e: Exception) {
            // Keep going without it.
        }
        prefs.edit().putBoolean(flag, true).apply()
    }

    // ---------- exercises ----------

    fun saveExercise(exercise: Exercise) = update { d ->
        val exists = d.exercises.any { it.id == exercise.id }
        d.copy(exercises = if (exists) d.exercises.map { if (it.id == exercise.id) exercise else it } else d.exercises + exercise)
    }

    fun nameExists(name: String, exceptId: String?): Boolean {
        val n = name.trim().lowercase()
        return _state.value.exercises.any { it.id != exceptId && it.name.trim().lowercase() == n }
    }

    fun usageCount(exerciseId: String): Int =
        _state.value.complexes.count { c -> c.exerciseIds().contains(exerciseId) }

    fun deleteExercise(id: String) {
        val removed = _state.value.exercises.find { it.id == id }
        update { d ->
            val complexes = d.complexes.map { c ->
                val items = c.items.mapNotNull { item ->
                    when (item) {
                        is ComplexItem.Single -> if (item.step.exerciseId == id) null else item
                        is ComplexItem.Cycle -> {
                            val steps = item.steps.filter { it.exerciseId != id }
                            if (steps.isEmpty()) null else item.copy(steps = steps)
                        }
                    }
                }
                c.copy(items = items)
            }
            d.copy(exercises = d.exercises.filter { it.id != id }, complexes = complexes)
        }
        if (removed != null && removed.icon.startsWith("img:")) {
            File(iconsDir, removed.icon.removePrefix("img:")).delete()
        }
    }

    /** Copies a picture from the gallery into app storage as a 256x256 square; returns the icon value or null. */
    fun saveIconFromUri(uri: Uri): String? {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 256 && bounds.outHeight / (sample * 2) >= 256) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
            val side = minOf(bitmap.width, bitmap.height)
            val square = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
            val scaled = Bitmap.createScaledBitmap(square, 256, 256, true)
            val fileName = newId() + ".jpg"
            FileOutputStream(File(iconsDir, fileName)).use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            return "img:$fileName"
        } catch (e: Exception) {
            return null
        }
    }

    // ---------- complexes ----------

    fun saveComplex(complex: Complex) = update { d ->
        val exists = d.complexes.any { it.id == complex.id }
        d.copy(complexes = if (exists) d.complexes.map { if (it.id == complex.id) complex else it } else d.complexes + complex)
    }

    fun deleteComplex(id: String) {
        update { d ->
            // Reminders for this workout become plain "time to train" nudges.
            d.copy(
                complexes = d.complexes.filter { it.id != id },
                reminders = d.reminders.map { r ->
                    if (r.complexId == id) {
                        r.copy(complexId = null, message = d.complexes.find { it.id == id }?.name?.let { "Time for $it" } ?: "")
                    } else r
                },
            )
        }
    }

    // ---------- reminders ----------

    fun saveReminder(reminder: Reminder) {
        update { d ->
            val exists = d.reminders.any { it.id == reminder.id }
            d.copy(reminders = if (exists) d.reminders.map { if (it.id == reminder.id) reminder else it } else d.reminders + reminder)
        }
        com.trainingtimer.app.reminders.ReminderScheduler.schedule(context, reminder)
    }

    fun deleteReminder(id: String) {
        com.trainingtimer.app.reminders.ReminderScheduler.cancel(context, id)
        update { d -> d.copy(reminders = d.reminders.filter { it.id != id }) }
    }

    fun setReminderEnabled(id: String, enabled: Boolean) {
        val r = _state.value.reminders.find { it.id == id } ?: return
        saveReminder(r.copy(enabled = enabled))
    }

    fun moveComplex(from: Int, to: Int) = update { d ->
        if (from !in d.complexes.indices || to !in d.complexes.indices) {
            d
        } else {
            val list = d.complexes.toMutableList()
            list.add(to, list.removeAt(from))
            d.copy(complexes = list)
        }
    }

    // ---------- settings / import ----------

    fun updateSettings(settings: Settings) = update { it.copy(settings = settings) }

    fun addImported(exercises: List<Exercise>, complexes: List<Complex>, categories: List<Category> = emptyList()) {
        if (exercises.isEmpty() && complexes.isEmpty() && categories.isEmpty()) return
        update {
            it.copy(
                exercises = it.exercises + exercises,
                complexes = it.complexes + complexes,
                categories = it.categories + categories,
            )
        }
    }

    // ---------- storage ----------

    private fun update(transform: (AppData) -> AppData) {
        val next = transform(_state.value)
        _state.value = next
        write(next)
    }

    private fun dataFile() = File(context.filesDir, "data.json")

    private fun write(data: AppData) {
        try {
            val file = dataFile()
            val tmp = File(file.parentFile, "data.json.tmp")
            tmp.writeText(Codec.toJson(data).toString())
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        } catch (e: Exception) {
            // Keep running with in-memory data if the disk write fails.
        }
    }
}

/** Internal storage format (with ids). The import/export format is in [ImportExport]. */
object Codec {
    fun toJson(d: AppData): JSONObject {
        val exercises = JSONArray()
        for (e in d.exercises) {
            val cats = JSONArray()
            for (id in e.categories) cats.put(id)
            exercises.put(JSONObject().put("id", e.id).put("name", e.name).put("icon", e.icon).put("cats", cats))
        }
        val categories = JSONArray()
        for (c in d.categories) {
            categories.put(JSONObject().put("id", c.id).put("name", c.name).put("color", c.color))
        }
        val complexes = JSONArray()
        for (c in d.complexes) {
            val items = JSONArray()
            for (item in c.items) {
                when (item) {
                    is ComplexItem.Single -> items.put(
                        stepToJson(item.step).put("type", "single")
                    )
                    is ComplexItem.Cycle -> {
                        val steps = JSONArray()
                        for (s in item.steps) steps.put(stepToJson(s))
                        val o = JSONObject().put("type", "cycle").put("repeats", item.repeats).put("steps", steps)
                        if (item.rest != null) o.put("rest", item.rest)
                        items.put(o)
                    }
                }
            }
            complexes.put(
                JSONObject().put("id", c.id).put("name", c.name).put("rest", c.restSeconds).put("items", items)
            )
        }
        val settings = JSONObject().put("beeps", d.settings.beeps).put("vibration", d.settings.vibration).put("countdown", d.settings.countdown)
        return JSONObject()
            .put("version", 1)
            .put("exercises", exercises)
            .put("complexes", complexes)
            .put("settings", settings)
            .put("categories", categories)
            .put("reminders", remindersToJson(d.reminders))
    }

    private fun remindersToJson(list: List<Reminder>): JSONArray {
        val arr = JSONArray()
        for (r in list) {
            val days = JSONArray()
            for (day in r.days) days.put(day)
            val o = JSONObject()
                .put("id", r.id).put("hour", r.hour).put("minute", r.minute).put("days", days)
                .put("message", r.message).put("alarm", r.alarm).put("snooze", r.snoozeMinutes).put("enabled", r.enabled)
            if (r.complexId != null) o.put("complex", r.complexId)
            if (r.sound != null) o.put("sound", r.sound)
            arr.put(o)
        }
        return arr
    }

    private fun remindersFromJson(arr: JSONArray?): List<Reminder> {
        if (arr == null) return emptyList()
        val out = ArrayList<Reminder>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val daysArr = o.optJSONArray("days")
            val days = ArrayList<Int>()
            if (daysArr != null) for (k in 0 until daysArr.length()) daysArr.optInt(k).takeIf { it in 1..7 }?.let { days += it }
            out += Reminder(
                id = o.optString("id").ifEmpty { newId() },
                hour = o.optInt("hour", 7).coerceIn(0, 23),
                minute = o.optInt("minute", 30).coerceIn(0, 59),
                days = days.distinct().sorted(),
                complexId = if (o.has("complex")) o.optString("complex") else null,
                message = o.optString("message", ""),
                alarm = o.optBoolean("alarm", false),
                snoozeMinutes = o.optInt("snooze", 10).coerceIn(1, 60),
                sound = if (o.has("sound")) o.optString("sound") else null,
                enabled = o.optBoolean("enabled", true),
            )
        }
        return out
    }

    private fun stepToJson(s: Step): JSONObject {
        val o = JSONObject().put("ex", s.exerciseId).put("sec", s.seconds).put("reps", s.reps)
        if (s.byReps) o.put("byReps", true)
        return o
    }

    private fun stepFromJson(o: JSONObject): Step = Step(
        exerciseId = o.getString("ex"),
        seconds = o.getInt("sec"),
        reps = o.optInt("reps", DEFAULT_REPS).coerceIn(1, MAX_REPS),
        byReps = o.optBoolean("byReps", false),
    )

    fun fromJson(o: JSONObject): AppData {
        val exercises = ArrayList<Exercise>()
        val exArr = o.optJSONArray("exercises")
        if (exArr != null) {
            for (i in 0 until exArr.length()) {
                val e = exArr.getJSONObject(i)
                val catArr = e.optJSONArray("cats")
                val cats = ArrayList<String>()
                if (catArr != null) for (k in 0 until catArr.length()) cats += catArr.getString(k)
                exercises += Exercise(e.getString("id"), e.getString("name"), e.optString("icon", ExerciseIcons.DEFAULT), cats)
            }
        }
        val complexes = ArrayList<Complex>()
        val cxArr = o.optJSONArray("complexes")
        if (cxArr != null) {
            for (i in 0 until cxArr.length()) {
                val c = cxArr.getJSONObject(i)
                val items = ArrayList<ComplexItem>()
                val itArr = c.optJSONArray("items") ?: JSONArray()
                for (j in 0 until itArr.length()) {
                    val it = itArr.getJSONObject(j)
                    if (it.optString("type") == "cycle") {
                        val steps = ArrayList<Step>()
                        val stArr = it.optJSONArray("steps") ?: JSONArray()
                        for (k in 0 until stArr.length()) {
                            val s = stArr.getJSONObject(k)
                            steps += stepFromJson(s)
                        }
                        val rest = if (it.has("rest")) it.optInt("rest", 0) else null
                        items += ComplexItem.Cycle(it.optInt("repeats", 1), steps, rest = rest)
                    } else {
                        items += ComplexItem.Single(stepFromJson(it))
                    }
                }
                complexes += Complex(c.getString("id"), c.getString("name"), c.optInt("rest", 15), items)
            }
        }
        val s = o.optJSONObject("settings")
        val settings = if (s != null) Settings(s.optBoolean("beeps", true), s.optBoolean("vibration", true), s.optInt("countdown", 5).coerceIn(0, 15)) else Settings()
        val categories = ArrayList<Category>()
        val caArr = o.optJSONArray("categories")
        if (caArr != null) {
            for (i in 0 until caArr.length()) {
                val c = caArr.getJSONObject(i)
                categories += Category(c.getString("id"), c.getString("name"), c.optString("color", "#4ADE9B"))
            }
        }
        val known = categories.map { it.id }.toSet()
        val cleaned = exercises.map { e -> e.copy(categories = e.categories.filter { it in known }) }
        return AppData(cleaned, complexes, settings, categories, remindersFromJson(o.optJSONArray("reminders")))
    }
}
