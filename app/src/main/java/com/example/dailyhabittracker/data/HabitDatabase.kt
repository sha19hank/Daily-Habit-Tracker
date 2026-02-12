package com.example.dailyhabittracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class, TokenEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        fun build(context: Context): HabitDatabase {
            return Room.databaseBuilder(
                context,
                HabitDatabase::class.java,
                "habit_tracker.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        }
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habits ADD COLUMN category TEXT")
        database.execSQL("ALTER TABLE habits ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE habits ADD COLUMN scheduledDays TEXT")
        database.execSQL("ALTER TABLE habits ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE habits ADD COLUMN reminderTime TEXT")
        database.execSQL("ALTER TABLE habits ADD COLUMN paused INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habits ADD COLUMN stepEnabled INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE habits ADD COLUMN stepGoal INTEGER")
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS tokens (id INTEGER NOT NULL PRIMARY KEY, count INTEGER NOT NULL, lastEarnedDate TEXT)"
        )
        database.execSQL("INSERT OR IGNORE INTO tokens (id, count, lastEarnedDate) VALUES (1, 0, NULL)")
    }
}
