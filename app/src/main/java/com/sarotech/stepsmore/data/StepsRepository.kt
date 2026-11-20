package com.sarotech.stepsmore.data

import com.sarotech.stepsmore.data.db.DailyStepsEntity
import com.sarotech.stepsmore.data.db.HourlyStepsEntity
import com.sarotech.stepsmore.data.db.StepsDao
import com.sarotech.stepsmore.data.prefs.AppPreferences
import com.sarotech.stepsmore.data.prefs.UserSettings
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class StepsRepository(
    private val dao: StepsDao,
    val prefs: AppPreferences
) {
    fun todayIso(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    fun observeStepsForDate(dateIso: String): Flow<Int?> = dao.observeStepsForDate(dateIso)

    fun observeTodaySteps(): Flow<Int?> = dao.observeStepsForDate(todayIso())

    fun observePast7Days(endDate: LocalDate = LocalDate.now()): Flow<List<DailyStepsEntity>> {
        val start = endDate.minusDays(6)
        return dao.observeDailyRange(
            start.format(DateTimeFormatter.ISO_LOCAL_DATE),
            endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }

    fun observeAllDaily(): Flow<List<DailyStepsEntity>> = dao.observeAllDaily()

    fun observeHourlyForDate(dateIso: String): Flow<List<HourlyStepsEntity>> =
        dao.observeHourlyForDate(dateIso)

    suspend fun recordStepDelta(
        delta: Int,
        eventTimeMillis: Long = System.currentTimeMillis()
    ) {
        if (delta <= 0) return
        val eventTime = Instant.ofEpochMilli(eventTimeMillis).atZone(ZoneId.systemDefault())
        val date = eventTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val hour = eventTime.hour

        val currentDaily = dao.getDaily(date)?.steps ?: 0
        dao.upsertDaily(DailyStepsEntity(date, currentDaily + delta))

        val currentHourly = dao.getHourly(date, hour)?.steps ?: 0
        dao.upsertHourly(HourlyStepsEntity(date, hour, currentHourly + delta))
    }

    suspend fun exportBackupJson(): String {
        val daily = dao.getAllDaily()
        val hourly = dao.getAllHourly()

        val root = JSONObject()
        root.put("app", "StepsMore")
        root.put("company", "SAROTECH")
        root.put("developer", "Prashant Latta")
        root.put("version", 1)

        val dailyArray = JSONArray()
        daily.forEach {
            dailyArray.put(JSONObject().apply {
                put("date", it.date)
                put("steps", it.steps)
            })
        }
        root.put("daily", dailyArray)

        val hourlyArray = JSONArray()
        hourly.forEach {
            hourlyArray.put(JSONObject().apply {
                put("date", it.date)
                put("hour", it.hour)
                put("steps", it.steps)
            })
        }
        root.put("hourly", hourlyArray)
        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Int {
        val root = JSONObject(jsonString)
        val dailyList = mutableListOf<DailyStepsEntity>()
        val hourlyList = mutableListOf<HourlyStepsEntity>()

        val dailyArr = root.optJSONArray("daily")
        if (dailyArr != null) {
            for (i in 0 until dailyArr.length()) {
                val obj = dailyArr.getJSONObject(i)
                dailyList.add(DailyStepsEntity(obj.getString("date"), obj.getInt("steps")))
            }
        }

        val hourlyArr = root.optJSONArray("hourly")
        if (hourlyArr != null) {
            for (i in 0 until hourlyArr.length()) {
                val obj = hourlyArr.getJSONObject(i)
                hourlyList.add(
                    HourlyStepsEntity(
                        obj.getString("date"),
                        obj.getInt("hour"),
                        obj.getInt("steps")
                    )
                )
            }
        }

        if (dailyList.isNotEmpty()) dao.upsertDailyList(dailyList)
        if (hourlyList.isNotEmpty()) dao.upsertHourlyList(hourlyList)
        return dailyList.size
    }

    companion object {
        fun calculateDistanceKm(steps: Int, stepLengthCm: Float): Double {
            return (steps * stepLengthCm) / 100000.0
        }

        fun formatDistance(steps: Int, settings: UserSettings): String {
            val km = calculateDistanceKm(steps, settings.stepLengthCm)
            return if (settings.unitSystem == "Imperial") {
                val miles = km * 0.621371
                String.format(Locale.US, "%.2f mi", miles)
            } else {
                String.format(Locale.US, "%.2f km", km)
            }
        }

        // Biomechanical walking energy expenditure (~0.745 gross kcal per kg per km, ~3.5 METs)
        fun calculateCalories(steps: Int, settings: UserSettings): Int {
            val km = calculateDistanceKm(steps, settings.stepLengthCm)
            return (km * settings.weightKg * 0.745).toInt()
        }

        fun formatDate(dateIso: String, pattern: String): String {
            return try {
                val parsed = LocalDate.parse(dateIso, DateTimeFormatter.ISO_LOCAL_DATE)
                parsed.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
            } catch (_: Exception) {
                dateIso
            }
        }
    }
}
