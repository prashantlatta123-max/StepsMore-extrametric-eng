package com.sarotech.stepsmore.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey val date: String, // ISO format: yyyy-MM-dd
    val steps: Int
)

@Entity(
    tableName = "hourly_steps",
    primaryKeys = ["date", "hour"]
)
data class HourlyStepsEntity(
    val date: String, // ISO format: yyyy-MM-dd
    val hour: Int,    // 0..23
    val steps: Int
)
