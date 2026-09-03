package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ProductEntity::class, ScanHistoryEntity::class, SensorReadingEntity::class, RfidMappingEntity::class], version = 3, exportSchema = false)
abstract class EcoDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun sensorReadingDao(): SensorReadingDao
    abstract fun rfidMappingDao(): RfidMappingDao

    companion object {
        @Volatile
        private var INSTANCE: EcoDatabase? = null

        fun getDatabase(context: Context): EcoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EcoDatabase::class.java,
                    "eco_mind_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                database.productDao().insertProducts(SampleData.initialProducts)
                                RfidMappingRepository(database.rfidMappingDao()).seedSampleMappings()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
