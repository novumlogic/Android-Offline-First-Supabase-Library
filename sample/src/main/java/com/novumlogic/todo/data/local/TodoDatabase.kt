package com.novumlogic.todo.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Database(
    entities = [Task::class, Category::class, Priority::class, ],
    version = 1, exportSchema = false
)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun priorityDao(): PriorityDao
//    abstract fun lastTimestampDao(): LastTimestampDao

    companion object {
        @Volatile
        private var instance: TodoDatabase? = null

        fun getDatabase(context: Context) = instance ?: synchronized(this) {

            Room.databaseBuilder(
                context.applicationContext,
                TodoDatabase::class.java,
                "tasks_database"
            ).addCallback(DatabaseCallback()).build()
                .also { instance = it }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val priorityDao = instance?.priorityDao()
                GlobalScope.launch(Dispatchers.IO) {
                    priorityDao?.insert(
                        Priority(1,0),
                        Priority(2,1),
                        Priority(3,2)
                    )
                }
            }
        }

    }
}

/*
@RenameTable("offline_timestamp","last_timestamp")
@RenameColumn.Entries(
    RenameColumn(
        tableName = "offline_timestamp",
        fromColumnName = "went_offline_at",
        toColumnName = "last_timestamp"
    )
)
class RenameTimestampTable: AutoMigrationSpec
*/
