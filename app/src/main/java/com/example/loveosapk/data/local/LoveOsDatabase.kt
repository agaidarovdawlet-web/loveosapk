package com.example.loveosapk.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WishEntity::class,
        TaskEntity::class,
        SavingEntity::class,
        NoteEntity::class,
        TimeCapsuleEntity::class,
        ChatMessageEntity::class,
        CycleLogEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LoveOsDatabase : RoomDatabase() {
    abstract fun dao(): LoveOsDao
    abstract fun cycleDao(): CycleLogDao

    companion object {
        @Volatile
        private var INSTANCE: LoveOsDatabase? = null

        fun getDatabase(context: Context): LoveOsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LoveOsDatabase::class.java,
                    "loveos_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE wishes ADD COLUMN author TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE wishes ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE wishes ADD COLUMN priority TEXT NOT NULL DEFAULT 'MEDIUM'")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN reactionsJson TEXT NOT NULL DEFAULT '{}'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN videoUrl TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN videoDuration INTEGER")
            }
        }
    }
}
