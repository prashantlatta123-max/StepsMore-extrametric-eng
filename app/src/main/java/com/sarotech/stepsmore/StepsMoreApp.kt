package com.sarotech.stepsmore

import android.app.Application
import com.sarotech.stepsmore.data.StepsRepository
import com.sarotech.stepsmore.data.db.StepsDatabase
import com.sarotech.stepsmore.data.prefs.AppPreferences
import com.sarotech.stepsmore.service.MidnightScheduler

class StepsMoreApp : Application() {
    lateinit var database: StepsDatabase
        private set
    lateinit var preferences: AppPreferences
        private set
    lateinit var repository: StepsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = StepsDatabase.getInstance(this)
        preferences = AppPreferences(this)
        repository = StepsRepository(database.stepsDao(), preferences)
        MidnightScheduler.scheduleNextMidnight(this)
    }
}
