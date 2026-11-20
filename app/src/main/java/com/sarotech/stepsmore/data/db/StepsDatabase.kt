package com.sarotech.stepsmore.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DailyStepsEntity::class, HourlyStepsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StepsDatabase : RoomDatabase() {
    abstract fun stepsDao(): StepsDao

    companion object {
        @Volatile
        private var INSTANCE: StepsDatabase? = null

        fun getInstance(context: Context): StepsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StepsDatabase::class.java,
                    "stepsmore.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
