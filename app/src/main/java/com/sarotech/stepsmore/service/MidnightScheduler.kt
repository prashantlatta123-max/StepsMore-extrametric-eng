package com.sarotech.stepsmore.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.ZoneId

object MidnightScheduler {
    private const val REQUEST_CODE = 2026

    fun scheduleNextMidnight(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MidnightResetReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextMidnightMillis = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() + 5_000L // 00:00:05 to avoid boundary race

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextMidnightMillis,
            pendingIntent
        )
    }
}
