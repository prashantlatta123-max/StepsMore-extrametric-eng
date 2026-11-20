package com.sarotech.stepsmore.service

/*
 * Tracking-engine integration notice:
 * This file adapts the sequential sensor-processing architecture used by
 * Stepsy (https://github.com/nvllz/stepsy), distributed under GNU GPL v3.
 * The original project credits Motionmate (https://github.com/0xf4b1/motionmate).
 *
 * Modified for StepsMore by SAROTECH / Prashant Latta:
 * - Android Sensor.TYPE_STEP_COUNTER is the primary source.
 * - Sensor callbacks are delivered on a dedicated HandlerThread.
 * - A single-consumer channel processes samples in order.
 * - Room remains the StepsMore persistence layer.
 * - Boot-count-aware baseline handling prevents counter-reset errors.
 *
 * This file is distributed under GNU GPL v3. See LICENSE and
 * THIRD_PARTY_NOTICES.md.
 */

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sarotech.stepsmore.MainActivity
import com.sarotech.stepsmore.StepsMoreApp
import com.sarotech.stepsmore.data.StepsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

class StepsMoreService : Service() {

    private data class SensorSample(
        val sensorType: Int,
        val value: Float,
        val timestampNanos: Long
    )

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val samples = Channel<SensorSample>(Channel.UNLIMITED)

    private lateinit var sensorManager: SensorManager
    private lateinit var repository: StepsRepository
    private lateinit var sensorThread: HandlerThread
    private lateinit var sensorHandler: Handler

    private var registeredMode: String? = null
    private var sensorRegistered = false
    private var counterInitialized = false
    private var lastCounterValue = -1f
    private var lastBootCount = -1

    // Accelerometer fallback state. Hardware step counter remains the default.
    private var gravityFiltered = 9.81
    private var lastStepTimeMs = 0L
    private var wasAboveThreshold = false

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            when (event.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    if (event.values.isNotEmpty()) {
                        samples.trySend(
                            SensorSample(Sensor.TYPE_STEP_COUNTER, event.values[0], event.timestamp)
                        )
                    }
                }
                Sensor.TYPE_STEP_DETECTOR -> {
                    if (event.values.isNotEmpty() && event.values[0] > 0f) {
                        samples.trySend(
                            SensorSample(Sensor.TYPE_STEP_DETECTOR, 1f, event.timestamp)
                        )
                    }
                }
                Sensor.TYPE_ACCELEROMETER -> processAccelerometer(event)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    override fun onCreate() {
        super.onCreate()
        repository = (application as StepsMoreApp).repository
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        sensorThread = HandlerThread("StepsMore-SensorThread").apply { start() }
        sensorHandler = Handler(sensorThread.looper)

        createNotificationChannels()

        // Single consumer guarantees samples are applied in the same order in which
        // the sensor callback enqueued them. Room/DataStore work is never launched
        // concurrently for individual sensor events.
        serviceScope.launch {
            for (sample in samples) {
                try {
                    processSample(sample)
                } catch (error: Exception) {
                    Log.e(TAG, "Could not process sensor sample", error)
                    _sensorStatus.value = "Sensor data processing error"
                }
            }
        }

        serviceScope.launch {
            repository.prefs.settingsFlow.collectLatest { settings ->
                sensorHandler.post {
                    if (!sensorRegistered || registeredMode != settings.sensorMode) {
                        registerSensor(settings.sensorMode)
                    }
                }
                val steps = repository.observeTodaySteps().first() ?: 0
                updateLiveNotification(
                    steps,
                    settings.dailyGoal,
                    settings.isPaused,
                    settings.showProgressInNotification
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasActivityPermission()) {
            _sensorStatus.value = "Activity Recognition permission denied"
            stopSelf()
            return START_NOT_STICKY
        }

        val initial = buildLiveNotification(0, 10000, false)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, initial, type)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to start foreground tracking service", error)
            _sensorStatus.value = "Foreground service could not start"
            stopSelf()
            return START_NOT_STICKY
        }

        sensorHandler.post {
            if (!sensorRegistered) {
                serviceScope.launch {
                    val settings = repository.prefs.getSettings()
                    sensorHandler.post { registerSensor(settings.sensorMode) }
                }
            } else if (intent?.action == ACTION_FLUSH) {
                runCatching { sensorManager.flush(sensorListener) }
            }
        }

        serviceScope.launch { refreshNotification() }
        return START_STICKY
    }

    private fun hasActivityPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Stepsy-style hardware-first registration. Accelerometer is only a fallback
     * or an explicitly selected mode; it is not used when the hardware counter works.
     */
    private fun registerSensor(mode: String) {
        if (sensorRegistered) {
            sensorManager.unregisterListener(sensorListener)
            sensorRegistered = false
        }

        registeredMode = mode
        counterInitialized = false
        lastCounterValue = -1f
        _sensorStatus.value = "Checking device sensors…"

        val counter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val detector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val candidates = if (mode == "Accelerometer (Instant)") {
            listOfNotNull(accelerometer)
        } else {
            listOfNotNull(counter, detector, accelerometer)
        }

        for (sensor in candidates) {
            val registered = sensorManager.registerListener(
                sensorListener,
                sensor,
                if (sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    SensorManager.SENSOR_DELAY_UI
                } else {
                    SensorManager.SENSOR_DELAY_NORMAL
                },
                0,
                sensorHandler
            )
            if (registered) {
                sensorRegistered = true
                _sensorStatus.value = when (sensor.type) {
                    Sensor.TYPE_STEP_COUNTER ->
                        "Hardware step counter connected • Waiting for movement"
                    Sensor.TYPE_STEP_DETECTOR ->
                        "Hardware step detector connected • Waiting for movement"
                    else ->
                        "Accelerometer fallback connected • Estimated counting"
                }
                Log.i(TAG, "Registered sensor type=${sensor.type}, name=${sensor.name}")
                return
            }
        }

        _sensorStatus.value = if (counter == null && detector == null && accelerometer == null) {
            "No compatible motion sensor found"
        } else {
            "Sensor registration failed • Check Activity Recognition permission"
        }
        Log.e(TAG, _sensorStatus.value)
    }

    private fun processAccelerometer(event: SensorEvent) {
        val x = event.values[0].toDouble()
        val y = event.values[1].toDouble()
        val z = event.values[2].toDouble()
        val magnitude = sqrt(x * x + y * y + z * z)

        gravityFiltered = 0.85 * gravityFiltered + 0.15 * magnitude
        val linearAcceleration = magnitude - gravityFiltered
        val now = SystemClock.elapsedRealtime()

        if (linearAcceleration > 1.8 && !wasAboveThreshold && now - lastStepTimeMs > 320L) {
            wasAboveThreshold = true
            lastStepTimeMs = now
            samples.trySend(
                SensorSample(Sensor.TYPE_STEP_DETECTOR, 1f, event.timestamp)
            )
        } else if (linearAcceleration < 0.8) {
            wasAboveThreshold = false
        }
    }

    private suspend fun processSample(sample: SensorSample) {
        val settings = repository.prefs.getSettings()
        val now = System.currentTimeMillis()
        val paused = settings.isPaused &&
            (settings.pauseUntilMillis <= 0L || now < settings.pauseUntilMillis)

        if (settings.isPaused && settings.pauseUntilMillis > 0L && now >= settings.pauseUntilMillis) {
            repository.prefs.setPaused(false, 0L)
        }

        val delta = when (sample.sensorType) {
            Sensor.TYPE_STEP_COUNTER -> calculateHardwareDelta(sample.value)
            Sensor.TYPE_STEP_DETECTOR -> 1
            else -> 0
        }

        if (sample.sensorType == Sensor.TYPE_STEP_COUNTER) {
            _sensorStatus.value = "Hardware step counter receiving data"
        } else if (sample.sensorType == Sensor.TYPE_STEP_DETECTOR) {
            _sensorStatus.value = if (registeredMode == "Accelerometer (Instant)") {
                "Accelerometer receiving data • Estimated counting"
            } else {
                "Hardware step detector receiving data"
            }
        }

        // Keep the baseline current while paused so paused steps are not added later.
        if (paused || delta <= 0) {
            refreshNotification()
            return
        }

        val eventWallTime = eventWallTimeMillis(sample.timestampNanos)
        saveAndNotify(
            delta = delta,
            eventTimeMillis = eventWallTime,
            dailyGoal = settings.dailyGoal,
            showProgress = settings.showProgressInNotification,
            notifyWhenReached = settings.notifyWhenReached,
            encouraging = settings.encouragingNotifications
        )
    }

    /**
     * Persisted hardware total allows a service/process restart to continue from
     * the previous event. Android BOOT_COUNT detects a reboot even if the new
     * sensor total happens to be numerically higher than the previous total.
     */
    private suspend fun calculateHardwareDelta(currentValue: Float): Int {
        val currentBoot = runCatching {
            Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)
        }.getOrDefault(-1)

        if (!counterInitialized) {
            val storedBoot = repository.prefs.getLastBootCount()
            lastBootCount = currentBoot
            lastCounterValue = repository.prefs.getLastSensorTotal()

            if (currentBoot >= 0 && storedBoot >= 0 && currentBoot != storedBoot) {
                lastCounterValue = -1f
            }
            if (currentBoot >= 0) repository.prefs.setLastBootCount(currentBoot)
            counterInitialized = true
        }

        val delta = if (lastCounterValue < 0f || currentValue < lastCounterValue) {
            0
        } else {
            (currentValue - lastCounterValue).roundToInt().coerceAtLeast(0)
        }

        lastCounterValue = currentValue
        repository.prefs.setLastSensorTotal(currentValue)
        if (currentBoot >= 0 && currentBoot != lastBootCount) {
            lastBootCount = currentBoot
            repository.prefs.setLastBootCount(currentBoot)
        }
        return delta
    }

    private fun eventWallTimeMillis(timestampNanos: Long): Long {
        val ageMillis = (SystemClock.elapsedRealtimeNanos() - timestampNanos) / 1_000_000L
        return if (ageMillis < 0L || ageMillis > MAX_EVENT_AGE_MS) {
            System.currentTimeMillis()
        } else {
            System.currentTimeMillis() - ageMillis
        }
    }

    private suspend fun saveAndNotify(
        delta: Int,
        eventTimeMillis: Long,
        dailyGoal: Int,
        showProgress: Boolean,
        notifyWhenReached: Boolean,
        encouraging: Boolean
    ) {
        repository.recordStepDelta(delta, eventTimeMillis)
        val todaySteps = repository.observeTodaySteps().first() ?: 0
        updateLiveNotification(todaySteps, dailyGoal, false, showProgress)
        checkGoalAndEncouragingNotifications(todaySteps, dailyGoal, notifyWhenReached, encouraging)
    }

    private suspend fun refreshNotification() {
        val settings = repository.prefs.getSettings()
        val steps = repository.observeTodaySteps().first() ?: 0
        updateLiveNotification(steps, settings.dailyGoal, settings.isPaused, settings.showProgressInNotification)
    }

    private fun updateLiveNotification(steps: Int, goal: Int, paused: Boolean, showProgress: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildLiveNotification(steps, goal, paused, showProgress))
    }

    private fun buildLiveNotification(
        steps: Int,
        goal: Int,
        paused: Boolean,
        showProgress: Boolean = true
    ): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (paused) "StepsMore • Paused" else "$steps steps"
        val text = if (paused) {
            "Step counting is paused"
        } else {
            val percentage = ((steps.toFloat() / goal.coerceAtLeast(1)) * 100)
                .toInt().coerceAtMost(999)
            "Daily goal: $goal ($percentage%)"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_LIVE)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)

        if (showProgress && !paused) {
            builder.setProgress(goal.coerceAtLeast(1), steps.coerceAtMost(goal), false)
        }
        return builder.build()
    }

    private suspend fun checkGoalAndEncouragingNotifications(
        steps: Int,
        goal: Int,
        notifyWhenReached: Boolean,
        encouraging: Boolean
    ) {
        if (goal <= 0) return
        val today = repository.todayIso()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notifyWhenReached && steps >= goal && repository.prefs.getGoalNotifiedDate() != today) {
            repository.prefs.setGoalNotifiedDate(today)
            manager.notify(
                GOAL_NOTIFICATION_ID,
                NotificationCompat.Builder(this, CHANNEL_GOAL)
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .setContentTitle("Daily goal reached!")
                    .setContentText("You reached $goal steps today with StepsMore.")
                    .setAutoCancel(true)
                    .build()
            )
            return
        }

        if (!encouraging) return
        val ratio = steps.toFloat() / goal.toFloat()

        if (ratio >= 0.8f && steps < goal &&
            repository.prefs.getAlmostGoalNotifiedDate() != today
        ) {
            repository.prefs.setAlmostGoalNotifiedDate(today)
            manager.notify(
                ENCOURAGE_NOTIFICATION_ID,
                NotificationCompat.Builder(this, CHANNEL_GOAL)
                    .setSmallIcon(android.R.drawable.ic_menu_directions)
                    .setContentTitle("Almost there!")
                    .setContentText("Only ${goal - steps} steps left to reach your daily goal.")
                    .setAutoCancel(true)
                    .build()
            )
        } else if (ratio >= 0.5f && ratio < 0.8f &&
            repository.prefs.getHalfGoalNotifiedDate() != today
        ) {
            repository.prefs.setHalfGoalNotifiedDate(today)
            manager.notify(
                ENCOURAGE_NOTIFICATION_ID,
                NotificationCompat.Builder(this, CHANNEL_GOAL)
                    .setSmallIcon(android.R.drawable.ic_menu_directions)
                    .setContentTitle("Halfway to your goal!")
                    .setContentText("Great pace — $steps steps logged today.")
                    .setAutoCancel(true)
                    .build()
            )
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_LIVE, "Live Step Counter", NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_GOAL, "Goal & Progress Alerts", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    override fun onDestroy() {
        samples.close()
        if (::sensorManager.isInitialized) sensorManager.unregisterListener(sensorListener)
        if (::sensorHandler.isInitialized) sensorHandler.removeCallbacksAndMessages(null)
        if (::sensorThread.isInitialized) sensorThread.quitSafely()
        _sensorStatus.value = "Stopped"
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "StepsMoreService"
        const val CHANNEL_LIVE = "stepsmore_live"
        const val CHANNEL_GOAL = "stepsmore_goal"
        const val NOTIFICATION_ID = 1001
        const val GOAL_NOTIFICATION_ID = 1002
        const val ENCOURAGE_NOTIFICATION_ID = 1003
        const val ACTION_FLUSH = "com.sarotech.stepsmore.ACTION_FLUSH"
        private const val MAX_EVENT_AGE_MS = 12L * 60L * 60L * 1000L

        private val _sensorStatus = MutableStateFlow("Starting…")
        val sensorStatus: StateFlow<String> = _sensorStatus.asStateFlow()

        fun start(context: Context, flush: Boolean = false) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                _sensorStatus.value = "Activity Recognition permission denied"
                return
            }

            try {
                val intent = Intent(context, StepsMoreService::class.java).apply {
                    if (flush) action = ACTION_FLUSH
                }
                context.startForegroundService(intent)
            } catch (error: Exception) {
                Log.e(TAG, "Could not start step tracking service", error)
                _sensorStatus.value = "Unable to start background tracking"
            }
        }
    }
}
