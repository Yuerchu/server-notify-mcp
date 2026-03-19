package com.yuerchu.remoteask.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yuerchu.remoteask.data.model.OptionsConverter
import com.yuerchu.remoteask.data.model.QuestionEntity

@Database(entities = [QuestionEntity::class], version = 1, exportSchema = false)
@TypeConverters(OptionsConverter::class)
abstract class QuestionDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao

    companion object {
        @Volatile
        private var INSTANCE: QuestionDatabase? = null

        fun getInstance(context: Context): QuestionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuestionDatabase::class.java,
                    "questions.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
