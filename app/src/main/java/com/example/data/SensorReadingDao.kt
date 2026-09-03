package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorReadingDao {
    @Query("SELECT * FROM sensor_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int = 50): Flow<List<SensorReadingEntity>>

    @Query("SELECT * FROM sensor_readings ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentReadingsDirect(limit: Int = 50): List<SensorReadingEntity>

    @Query("SELECT * FROM sensor_readings ORDER BY timestamp ASC")
    fun getAllReadingsAsc(): Flow<List<SensorReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: SensorReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<SensorReadingEntity>)

    @Query("DELETE FROM sensor_readings")
    suspend fun clearAllReadings()
}
