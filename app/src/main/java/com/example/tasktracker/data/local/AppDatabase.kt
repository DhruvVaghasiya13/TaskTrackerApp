package com.example.tasktracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TaskEntity::class, CompletionEntity::class, TaskSelectionEntity::class], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun completionDao(): CompletionDao
    abstract fun selectionDao(): SelectionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** v3 -> v4: tasks get an endDate so deleted / replaced tasks stay in past days' history. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN endDate TEXT NOT NULL DEFAULT ''")
                // tasks deleted before this version lost their history anyway -> keep them hidden everywhere
                db.execSQL("UPDATE tasks SET endDate = due WHERE deleted = 1")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tasktracker.db")
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
