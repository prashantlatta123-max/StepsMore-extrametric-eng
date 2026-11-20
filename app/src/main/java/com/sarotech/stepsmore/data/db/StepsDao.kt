package com.sarotech.stepsmore.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StepsDao {

    @Query("SELECT steps FROM daily_steps WHERE date = :date")
    fun observeStepsForDate(date: String): Flow<Int?>

    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeDailyRange(startDate: String, endDate: String): Flow<List<DailyStepsEntity>>

    @Query("SELECT * FROM daily_steps ORDER BY date ASC")
    fun observeAllDaily(): Flow<List<DailyStepsEntity>>

    @Query("SELECT * FROM daily_steps ORDER BY date ASC")
    suspend fun getAllDaily(): List<DailyStepsEntity>

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getDaily(date: String): DailyStepsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDaily(entity: DailyStepsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyList(entities: List<DailyStepsEntity>)

    // Hourly breakdown queries
    @Query("SELECT * FROM hourly_steps WHERE date = :date ORDER BY hour ASC")
    fun observeHourlyForDate(date: String): Flow<List<HourlyStepsEntity>>

    @Query("SELECT * FROM hourly_steps WHERE date = :date AND hour = :hour")
    suspend fun getHourly(date: String, hour: Int): HourlyStepsEntity?

    @Query("SELECT * FROM hourly_steps ORDER BY date ASC, hour ASC")
    suspend fun getAllHourly(): List<HourlyStepsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHourly(entity: HourlyStepsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHourlyList(entities: List<HourlyStepsEntity>)

    @Query("DELETE FROM daily_steps")
    suspend fun clearDaily()

    @Query("DELETE FROM hourly_steps")
    suspend fun clearHourly()
}
