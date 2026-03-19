package com.yuerchu.remoteask.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yuerchu.remoteask.data.model.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY CASE WHEN status = 'pending' THEN 0 ELSE 1 END, receivedAt DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: String): QuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: QuestionEntity)

    @Update
    suspend fun update(question: QuestionEntity)

    @Query("SELECT * FROM questions WHERE pendingSubmit = 1")
    suspend fun getPendingSubmits(): List<QuestionEntity>

    @Query("DELETE FROM questions")
    suspend fun deleteAll()
}
