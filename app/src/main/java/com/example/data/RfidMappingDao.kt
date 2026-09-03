package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RfidMappingDao {

    @Query("SELECT * FROM rfid_mappings ORDER BY lastUpdated DESC")
    fun getAllMappings(): Flow<List<RfidMappingEntity>>

    @Query("SELECT * FROM rfid_mappings ORDER BY lastUpdated DESC")
    suspend fun getAllMappingsDirect(): List<RfidMappingEntity>

    @Query("SELECT * FROM rfid_mappings WHERE tagId = :tagId LIMIT 1")
    fun getMappingForTag(tagId: String): Flow<RfidMappingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: RfidMappingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<RfidMappingEntity>)

    @Query("DELETE FROM rfid_mappings WHERE tagId = :tagId")
    suspend fun deleteMapping(tagId: String)

    @Query("DELETE FROM rfid_mappings")
    suspend fun clearAllMappings()
}
