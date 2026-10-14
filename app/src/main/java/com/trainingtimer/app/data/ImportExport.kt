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
)

/**
 * File format (JSON):
 * {
 *   "version": 1,
 *   "exercises": [ { "name": "Burpees", "icon": "jump" }, ... ],
 *   "complexes": [ { "name": "Morning HIIT", "rest": 15, "items": [
 *       { "exercise": "Burpees", "time": 30 },
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

        if (exArr != null) {
            for (i in 0 until exArr.length()) {
                val entry = exArr.opt(i)
                val name: String
                var icon: String? = null
                when (entry) {
                    is String -> name = entry.trim()
                    is JSONObject -> {
                        name = stringOrEmpty(entry, "name")
                        icon = stringOrEmpty(entry, "icon").lowercase().ifEmpty { null }
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
                val ex = Exercise(newId(), name, if (icon != null && ExerciseIcons.has(icon)) icon else ExerciseIcons.DEFAULT)
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
                val items = ArrayList<ComplexItem>()
                val itArr = c.optJSONArray("items") ?: JSONArray()
                for (j in 0 until itArr.length()) {
                    val it = itArr.optJSONObject(j) ?: continue
                    val cycle = it.optJSONObject("cycle")
                    if (cycle != null) {
                        val steps = parseSteps(cycle.optJSONArray("items") ?: JSONArray(), name, byName, errors)
                        if (steps.isNotEmpty()) items += ComplexItem.Cycle(cycle.optInt("repeats", 1).coerceIn(1, 99), steps)
                    } else {
                        val step = parseStep(it, name, byName, errors)
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

        Repo.addImported(added, addedComplexes)
        return ImportResult(
            addedExercises = added.map { it.name },
            skippedExercises = skipped,
            addedComplexes = addedComplexes.map { it.name },
            skippedComplexes = skippedComplexes,
            errors = errors,
        )
    }

    private fun stringOrEmpty(o: JSONObject, key: String): String {
        val v = o.opt(key)
        return if (v is String) v.trim() else ""
    }

    private fun parseSteps(arr: JSONArray, complexName: String, byName: Map<String, Exercise>, errors: MutableList<String>): List<Step> {
        val out = ArrayList<Step>()
        for (k in 0 until arr.length()) {
            val o = arr.optJSONObject(k) ?: continue
            val step = parseStep(o, complexName, byName, errors)
            if (step != null) out += step
        }
        return out
    }

    private fun parseStep(o: JSONObject, complexName: String, byName: Map<String, Exercise>, errors: MutableList<String>): Step? {
        val exName = stringOrEmpty(o, "exercise")
        val ex = byName[exName.lowercase()]
        if (ex == null) {
            errors += "Complex \"$complexName\": unknown exercise \"$exName\""
            return null
        }
        return Step(ex.id, o.optInt("time", 30).coerceIn(1, 3600))
    }

    fun exportExercises(d: AppData): String =
        JSONObject().put("version", 1).put("exercises", exercisesJson(d)).toString(2)

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
                        items.put(JSONObject().put("cycle", JSONObject().put("repeats", item.repeats).put("items", steps)))
                    }
                }
            }
            complexes.put(JSONObject().put("name", c.name).put("rest", c.restSeconds).put("items", items))
        }
        return JSONObject()
            .put("version", 1)
            .put("exercises", exercisesJson(d))
            .put("complexes", complexes)
            .toString(2)
    }

    private fun stepJson(e: Exercise, s: Step): JSONObject = JSONObject().put("exercise", e.name).put("time", s.seconds)

    private fun exercisesJson(d: AppData): JSONArray {
        val arr = JSONArray()
        for (e in d.exercises.sortedBy { it.name.lowercase() }) {
            val o = JSONObject().put("name", e.name)
            if (!e.icon.startsWith("img:")) o.put("icon", e.icon)
            arr.put(o)
        }
        return arr
    }
}
