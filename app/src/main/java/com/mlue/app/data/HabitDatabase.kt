package com.mlue.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HabitEntity::class,
        HabitCompletionEntity::class,
        TokenEntity::class,
        GoalEntity::class,
        JournalEntryEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun goalDao(): GoalDao
    abstract fun journalDao(): JournalDao

    companion object {
        fun build(context: Context): HabitDatabase {
            return Room.databaseBuilder(
                context,
                HabitDatabase::class.java,
                "habit_tracker.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
        }
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        android.util.Log.d("MlueStartup", "HabitDatabase: Running MIGRATION_1_2")
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
        android.util.Log.d("MlueStartup", "HabitDatabase: Running MIGRATION_2_3")
        database.execSQL("ALTER TABLE habits ADD COLUMN stepEnabled INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE habits ADD COLUMN stepGoal INTEGER")
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS tokens (id INTEGER NOT NULL PRIMARY KEY, count INTEGER NOT NULL, lastEarnedDate TEXT)"
        )
        database.execSQL("INSERT OR IGNORE INTO tokens (id, count, lastEarnedDate) VALUES (1, 0, NULL)")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        android.util.Log.d("MlueStartup", "HabitDatabase: Running MIGRATION_3_4")
        database.execSQL("ALTER TABLE habits ADD COLUMN goalId INTEGER")
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS goals (" +
                "goalId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "title TEXT NOT NULL, " +
                "description TEXT, " +
                "startDate TEXT NOT NULL, " +
                "deadline TEXT" +
            ")"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS journal_entries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "title TEXT NOT NULL, " +
                "body TEXT NOT NULL, " +
                "date TEXT NOT NULL, " +
                "mood TEXT" +
            ")"
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_journal_entries_date ON journal_entries(date)"
        )
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        android.util.Log.d("MlueStartup", "HabitDatabase: Running MIGRATION_4_5")
        // Add timestamp column
        database.execSQL("ALTER TABLE journal_entries ADD COLUMN timestamp INTEGER NOT NULL DEFAULT 0")
        
        // SQLite doesn't support DROP INDEX IF EXISTS easily in some versions, but we can drop it.
        database.execSQL("DROP INDEX IF EXISTS index_journal_entries_date")
        
        // Recreate index without UNIQUE constraint
        database.execSQL("CREATE INDEX IF NOT EXISTS index_journal_entries_date ON journal_entries(date)")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        android.util.Log.d("MlueStartup", "HabitDatabase: Running MIGRATION_5_6")
        database.execSQL("ALTER TABLE habits ADD COLUMN highestCelebratedMilestone INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE goals ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE goals ADD COLUMN completedDate TEXT")
    }
}
