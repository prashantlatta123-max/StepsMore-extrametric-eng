package com.sarotech.stepsmore.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MidnightResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MidnightScheduler.scheduleNextMidnight(context)
        StepsMoreService.start(context)
    }
}
