package com.sarotech.stepsmore.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "stepsmore_prefs")

data class UserSettings(
    val dailyGoal: Int = 10000,
    val notifyWhenReached: Boolean = false,
    val showProgressInNotification: Boolean = true,
    val encouragingNotifications: Boolean = true,
    val drawTargetLine: Boolean = true,
    val heightCm: Int = 167,
    val stepLengthCm: Float = 69.14f,
    val weightKg: Int = 79,
    val unitSystem: String = "Metric", // Metric or Imperial
    val dateFormat: String = "dd/MM/yyyy",
    val firstDayOfWeek: String = "Monday", // Monday or Sunday
    val sensorMode: String = "Auto (Hardware)", // "Auto (Hardware)" or "Accelerometer (Instant)"
    val themeMode: String = "Black",
    val accentColorHex: String = "#7C83FF",
    val isPaused: Boolean = false,
    val pauseUntilMillis: Long = 0L
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val NOTIFY_REACHED = booleanPreferencesKey("notify_reached")
        val SHOW_LIVE_NOTIF = booleanPreferencesKey("show_live_notif")
        val ENCOURAGING_NOTIF = booleanPreferencesKey("encouraging_notif")
        val DRAW_TARGET_LINE = booleanPreferencesKey("draw_target_line")
        val HEIGHT_CM = intPreferencesKey("height_cm")
        val STEP_LENGTH_CM = floatPreferencesKey("step_length_cm")
        val WEIGHT_KG = intPreferencesKey("weight_kg")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
        val SENSOR_MODE = stringPreferencesKey("sensor_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR_HEX = stringPreferencesKey("accent_color_hex")
        val IS_PAUSED = booleanPreferencesKey("is_paused")
        val PAUSE_UNTIL = longPreferencesKey("pause_until")
        val LAST_SENSOR_TOTAL = floatPreferencesKey("last_sensor_total")
        val LAST_BOOT_COUNT = intPreferencesKey("last_boot_count")
        val GOAL_NOTIFIED_DATE = stringPreferencesKey("goal_notified_date")
        val HALF_GOAL_NOTIFIED_DATE = stringPreferencesKey("half_goal_notified_date")
        val ALMOST_GOAL_NOTIFIED_DATE = stringPreferencesKey("almost_goal_notified_date")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { p ->
        UserSettings(
            dailyGoal = p[Keys.DAILY_GOAL] ?: 10000,
            notifyWhenReached = p[Keys.NOTIFY_REACHED] ?: false,
            showProgressInNotification = p[Keys.SHOW_LIVE_NOTIF] ?: true,
            encouragingNotifications = p[Keys.ENCOURAGING_NOTIF] ?: true,
            drawTargetLine = p[Keys.DRAW_TARGET_LINE] ?: true,
            heightCm = p[Keys.HEIGHT_CM] ?: 167,
            stepLengthCm = p[Keys.STEP_LENGTH_CM] ?: 69.14f,
            weightKg = p[Keys.WEIGHT_KG] ?: 79,
            unitSystem = p[Keys.UNIT_SYSTEM] ?: "Metric",
            dateFormat = p[Keys.DATE_FORMAT] ?: "dd/MM/yyyy",
            firstDayOfWeek = p[Keys.FIRST_DAY_OF_WEEK] ?: "Monday",
            sensorMode = p[Keys.SENSOR_MODE] ?: "Auto (Hardware)",
            themeMode = p[Keys.THEME_MODE] ?: "Black",
            accentColorHex = p[Keys.ACCENT_COLOR_HEX] ?: "#7C83FF",
            isPaused = p[Keys.IS_PAUSED] ?: false,
            pauseUntilMillis = p[Keys.PAUSE_UNTIL] ?: 0L
        )
    }

    suspend fun getSettings(): UserSettings = settingsFlow.first()

    suspend fun setDailyGoal(goal: Int) {
        context.dataStore.edit { it[Keys.DAILY_GOAL] = goal.coerceIn(500, 200000) }
    }

    suspend fun setNotifyWhenReached(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_REACHED] = enabled }
    }

    suspend fun setShowProgressInNotification(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_LIVE_NOTIF] = enabled }
    }

    suspend fun setEncouragingNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENCOURAGING_NOTIF] = enabled }
    }

    suspend fun setDrawTargetLine(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DRAW_TARGET_LINE] = enabled }
    }

    suspend fun setHeightCm(height: Int) {
        val safeHeight = height.coerceIn(100, 250)
        context.dataStore.edit {
            it[Keys.HEIGHT_CM] = safeHeight
            // Standard walking biomechanics: step length ≈ 0.414 * height
            it[Keys.STEP_LENGTH_CM] = safeHeight * 0.414f
        }
    }

    suspend fun setStepLengthCm(stepLength: Float) {
        context.dataStore.edit { it[Keys.STEP_LENGTH_CM] = stepLength.coerceIn(30f, 120f) }
    }

    suspend fun setWeightKg(weight: Int) {
        context.dataStore.edit { it[Keys.WEIGHT_KG] = weight.coerceIn(25, 250) }
    }

    suspend fun setUnitSystem(unit: String) {
        context.dataStore.edit { it[Keys.UNIT_SYSTEM] = unit }
    }

    suspend fun setDateFormat(format: String) {
        context.dataStore.edit { it[Keys.DATE_FORMAT] = format }
    }

    suspend fun setFirstDayOfWeek(day: String) {
        context.dataStore.edit { it[Keys.FIRST_DAY_OF_WEEK] = day }
    }

    suspend fun setSensorMode(mode: String) {
        context.dataStore.edit { it[Keys.SENSOR_MODE] = mode }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setAccentColorHex(hex: String) {
        val normalized = if (hex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) hex.uppercase() else "#7C83FF"
        context.dataStore.edit { it[Keys.ACCENT_COLOR_HEX] = normalized }
    }

    suspend fun setPaused(paused: Boolean, pauseUntilMillis: Long = 0L) {
        context.dataStore.edit {
            it[Keys.IS_PAUSED] = paused
            it[Keys.PAUSE_UNTIL] = if (paused) pauseUntilMillis else 0L
        }
    }

    suspend fun getLastSensorTotal(): Float {
        return context.dataStore.data.first()[Keys.LAST_SENSOR_TOTAL] ?: -1f
    }

    suspend fun setLastSensorTotal(value: Float) {
        context.dataStore.edit { it[Keys.LAST_SENSOR_TOTAL] = value }
    }

    suspend fun getLastBootCount(): Int {
        return context.dataStore.data.first()[Keys.LAST_BOOT_COUNT] ?: -1
    }

    suspend fun setLastBootCount(value: Int) {
        if (value >= 0) {
            context.dataStore.edit { it[Keys.LAST_BOOT_COUNT] = value }
        }
    }

    suspend fun getGoalNotifiedDate(): String {
        return context.dataStore.data.first()[Keys.GOAL_NOTIFIED_DATE] ?: ""
    }

    suspend fun setGoalNotifiedDate(date: String) {
        context.dataStore.edit { it[Keys.GOAL_NOTIFIED_DATE] = date }
    }

    suspend fun getHalfGoalNotifiedDate(): String {
        return context.dataStore.data.first()[Keys.HALF_GOAL_NOTIFIED_DATE] ?: ""
    }

    suspend fun setHalfGoalNotifiedDate(date: String) {
        context.dataStore.edit { it[Keys.HALF_GOAL_NOTIFIED_DATE] = date }
    }

    suspend fun getAlmostGoalNotifiedDate(): String {
        return context.dataStore.data.first()[Keys.ALMOST_GOAL_NOTIFIED_DATE] ?: ""
    }

    suspend fun setAlmostGoalNotifiedDate(date: String) {
        context.dataStore.edit { it[Keys.ALMOST_GOAL_NOTIFIED_DATE] = date }
    }
}
