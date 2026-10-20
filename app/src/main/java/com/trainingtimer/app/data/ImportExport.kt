package com.trainingtimer.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

data class ImportResult(
    val addedExercises: List<String>,
    val skippedExercises: List<String>,
    val addedComplexes: List<String>,
    val skippedComplexes: List<String>,
    val errors: List<String>,
    /** Exercises that were added with the "?" icon and need a real one. */
    val withoutIcon: List<String> = emptyList(),
)

/**
 * File format (JSON):
 * {
 *   "version": 1,
 *   "categories": [ { "name": "Растяжка", "color": "#C49BFF" } ],            (optional)
 *   "exercises": [ { "name": "Burpees", "icon": "jump", "categories": ["Силовые"] }, ... ],
 *   "complexes": [ { "name": "Morning HIIT", "rest": 15, "items": [
 *       { "exercise": "Burpees", "time": 30 },
 *       { "exercise": "Push-ups", "reps": 12 },                              (reps set: ends when the user taps Done)
 *       { "cycle": { "repeats": 3, "items": [ { "exercise": "Plank", "time": 45 } ] } } ] } ]
 * }
 * "complexes" is optional. A plain list of exercises is also accepted.
 */
object ImportExport {

    class ImportException(message: String) : Exception(message)

    fun importJson(rawText: String): ImportResult {
        val text = rawText.removePrefix("﻿").trim()
        if (text.isEmpty()) throw ImportException("The file is empty.")
        val root: Any = try {
            JSONTokener(text).nextValue()
        } catch (e: Exception) {
            throw ImportException("This file is not valid JSON.")
        }
        val exArr: JSONArray?
        val cxArr: JSONArray?
        when (root) {
            is JSONArray -> {
                exArr = root
                cxArr = null
            }
            is JSONObject -> {
                exArr = root.optJSONArray("exercises")
                cxArr = root.optJSONArray("complexes")
            }
            else -> throw ImportException("This file is not valid JSON.")
        }
        if (exArr == null && cxArr == null) {
            throw ImportException("No \"exercises\" list was found in this file.")
        }

        val data = Repo.state.value
        val byName = HashMap<String, Exercise>()
        for (e in data.exercises) byName[e.name.trim().lowercase()] = e

        val added = ArrayList<Exercise>()
        val skipped = ArrayList<String>()
        val errors = ArrayList<String>()

        // Categories: match by name, create the ones that don't exist yet.
        val catByName = HashMap<String, Category>()
        for (c in data.categories) catByName[c.name.trim().lowercase()] = c
        val newCategories = ArrayList<Category>()
        val usedColors = data.categories.map { it.color }.toMutableSet()
        fun categoryFor(name: String, color: String?): Category {
            val key = name.trim().lowercase()
            catByName[key]?.let { return it }
            val chosen = color?.takeIf { it.matches(Regex("#[0-9A-Fa-f]{6}")) }
                ?: CategoryColors.palette.firstOrNull { it !in usedColors }
                ?: CategoryColors.palette[(data.categories.size + newCategories.size) % CategoryColors.palette.size]
            usedColors += chosen
            val c = Category(newId(), name.trim(), chosen)
            catByName[key] = c
            newCategories += c
            return c
        }
        if (root is JSONObject) {
            val caArr = root.optJSONArray("categories")
            if (caArr != null) {
                for (i in 0 until caArr.length()) {
                    val c = caArr.opt(i)
                    when (c) {
                        is String -> if (c.isNotBlank()) categoryFor(c, null)
                        is JSONObject -> {
                            val n = stringOrEmpty(c, "name")
                            if (n.isNotEmpty()) categoryFor(n, stringOrEmpty(c, "color").ifEmpty { null })
                        }
                    }
                }
            }
        }

        if (exArr != null) {
            for (i in 0 until exArr.length()) {
                val entry = exArr.opt(i)
                val name: String
                var icon: String? = null
                var catNames: List<String> = emptyList()
                when (entry) {
                    is String -> name = entry.trim()
                    is JSONObject -> {
                        name = stringOrEmpty(entry, "name")
                        icon = stringOrEmpty(entry, "icon").lowercase().ifEmpty { null }
                        val ca = entry.optJSONArray("categories")
                        if (ca != null) {
                            catNames = (0 until ca.length()).mapNotNull { k -> (ca.opt(k) as? String)?.trim()?.ifEmpty { null } }
                        }
                    }
                    else -> {
                        errors += "Exercise #${i + 1}: not a valid entry"
                        continue
                    }
                }
                if (name.isEmpty()) {
                    errors += "Exercise #${i + 1}: missing name"
                    continue
                }
                val key = name.lowercase()
                if (byName.containsKey(key)) {
                    if (!skipped.any { it.equals(name, ignoreCase = true) }) skipped += name
                    continue
                }
                val catIds = catNames.map { categoryFor(it, null).id }.distinct()
                val ex = Exercise(newId(), name, if (icon != null && ExerciseIcons.has(icon)) icon else ExerciseIcons.UNKNOWN, catIds)
                byName[key] = ex
                added += ex
            }
        }

        val addedComplexes = ArrayList<Complex>()
        val skippedComplexes = ArrayList<String>()
        if (cxArr != null) {
            val complexNames = data.complexes.map { it.name.trim().lowercase() }.toMutableSet()
            for (i in 0 until cxArr.length()) {
                val c = cxArr.optJSONObject(i)
                if (c == null) {
                    errors += "Complex #${i + 1}: not a valid entry"
                    continue
                }
                val name = stringOrEmpty(c, "name")
                if (name.isEmpty()) {
                    errors += "Complex #${i + 1}: missing name"
                    continue
                }
                if (complexNames.contains(name.lowercase())) {
                    skippedComplexes += name
                    continue
                }
                val rest = c.optInt("rest", 15).coerceIn(0, 3600)
                // An exercise the complex needs but the library doesn't have is created with a "?" icon.
                val create: (String) -> Exercise = { exName ->
                    val ex = Exercise(newId(), exName, ExerciseIcons.UNKNOWN)
                    byName[exName.lowercase()] = ex
                    added += ex
                    ex
                }
                val items = ArrayList<ComplexItem>()
                val itArr = c.optJSONArray("items") ?: JSONArray()
                for (j in 0 until itArr.length()) {
                    val it = itArr.optJSONObject(j) ?: continue
                    val cycle = it.optJSONObject("cycle")
                    if (cycle != null) {
                        val steps = parseSteps(cycle.optJSONArray("items") ?: JSONArray(), name, byName, errors, create)
                        val cycleRest = if (cycle.has("rest")) cycle.optInt("rest", 0).coerceIn(0, 3600) else null
                        if (steps.isNotEmpty()) items += ComplexItem.Cycle(cycle.optInt("repeats", 1).coerceIn(1, 99), steps, rest = cycleRest)
                    } else {
                        val step = parseStep(it, name, byName, errors, create)
                        if (step != null) items += ComplexItem.Single(step)
                    }
                }
                if (items.isEmpty()) {
                    errors += "Complex \"$name\": no valid exercises"
                    continue
                }
                complexNames += name.lowercase()
                addedComplexes += Complex(newId(), name, rest, items)
            }
        }

        Repo.addImported(added, addedComplexes, newCategories)
        return ImportResult(
            addedExercises = added.map { it.name },
            skippedExercises = skipped,
            addedComplexes = addedComplexes.map { it.name },
            skippedComplexes = skippedComplexes,
            errors = errors,
            withoutIcon = added.filter { it.icon == ExerciseIcons.UNKNOWN }.map { it.name },
        )
    }

    private fun stringOrEmpty(o: JSONObject, key: String): String {
        val v = o.opt(key)
        return if (v is String) v.trim() else ""
    }

    private fun parseSteps(
        arr: JSONArray,
        complexName: String,
        byName: Map<String, Exercise>,
        errors: MutableList<String>,
        create: (String) -> Exercise,
    ): List<Step> {
        val out = ArrayList<Step>()
        for (k in 0 until arr.length()) {
            val o = arr.optJSONObject(k) ?: continue
            val step = parseStep(o, complexName, byName, errors, create)
            if (step != null) out += step
        }
        return out
    }

    private fun parseStep(
        o: JSONObject,
        complexName: String,
        byName: Map<String, Exercise>,
        errors: MutableList<String>,
        create: (String) -> Exercise,
    ): Step? {
        val exName = stringOrEmpty(o, "exercise")
        if (exName.isEmpty()) {
            errors += "Complex \"$complexName\": an exercise without a name was skipped"
            return null
        }
        val ex = byName[exName.lowercase()] ?: create(exName)
        val seconds = o.optInt("time", 30).coerceIn(1, 3600)
        val reps = o.optInt("reps", 0)
        return if (reps > 0) {
            Step(ex.id, seconds, reps = reps.coerceAtMost(MAX_REPS), byReps = true)
        } else {
            Step(ex.id, seconds)
        }
    }

    fun exportExercises(d: AppData): String =
        JSONObject().put("version", 1).put("categories", categoriesJson(d)).put("exercises", exercisesJson(d)).toString(2)

    private fun categoriesJson(d: AppData): JSONArray {
        val arr = JSONArray()
        for (c in d.categories) arr.put(JSONObject().put("name", c.name).put("color", c.color))
        return arr
    }

    /** Only the given complexes, with the exercises and categories they use. */
    fun exportComplexes(d: AppData, selected: List<Complex>): String {
        val used = selected.flatMap { it.exerciseIds() }.toSet()
        val exercises = d.exercises.filter { it.id in used }
        val catIds = exercises.flatMap { it.categories }.toSet()
        val part = d.copy(
            exercises = exercises,
            complexes = selected,
            categories = d.categories.filter { it.id in catIds },
        )
        return exportBackup(part)
    }

    fun exportBackup(d: AppData): String {
        val byId = d.exercises.associateBy { it.id }
        val complexes = JSONArray()
        for (c in d.complexes) {
            val items = JSONArray()
            for (item in c.items) {
                when (item) {
                    is ComplexItem.Single -> byId[item.step.exerciseId]?.let { items.put(stepJson(it, item.step)) }
                    is ComplexItem.Cycle -> {
                        val steps = JSONArray()
                        for (s in item.steps) byId[s.exerciseId]?.let { steps.put(stepJson(it, s)) }
                        val c = JSONObject().put("repeats", item.repeats).put("items", steps)
                        if (item.rest != null) c.put("rest", item.rest)
                        items.put(JSONObject().put("cycle", c))
                    }
                }
            }
            complexes.put(JSONObject().put("name", c.name).put("rest", c.restSeconds).put("items", items))
        }
        return JSONObject()
            .put("version", 1)
            .put("categories", categoriesJson(d))
            .put("exercises", exercisesJson(d))
            .put("complexes", complexes)
            .toString(2)
    }

    private fun stepJson(e: Exercise, s: Step): JSONObject {
        val o = JSONObject().put("exercise", e.name)
        return if (s.byReps) o.put("reps", s.reps) else o.put("time", s.seconds)
    }

    private fun exercisesJson(d: AppData): JSONArray {
        val arr = JSONArray()
        for (e in d.exercises.sortedBy { it.name.lowercase() }) {
            val o = JSONObject().put("name", e.name)
            if (!e.icon.startsWith("img:")) o.put("icon", e.icon)
            val names = e.categories.mapNotNull { id -> d.categories.find { it.id == id }?.name }
            if (names.isNotEmpty()) {
                val ca = JSONArray()
                for (n in names) ca.put(n)
                o.put("categories", ca)
            }
            arr.put(o)
        }
        return arr
    }
}
