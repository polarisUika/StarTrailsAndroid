package com.startrails.core.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StackDao {
    @Query("SELECT * FROM stack_jobs ORDER BY updatedAt DESC")
    abstract fun observeJobs(): Flow<List<StackJobEntity>>

    @Query(
        "SELECT * FROM stack_jobs WHERE status IN ('QUEUED','RUNNING','SAVING','CANCELLING') " +
            "ORDER BY updatedAt DESC LIMIT 1",
    )
    abstract fun observeActiveJob(): Flow<StackJobEntity?>

    @Query("SELECT * FROM stack_jobs WHERE id = :id")
    abstract suspend fun getJob(id: String): StackJobEntity?

    @Query("SELECT * FROM stack_jobs WHERE status IN ('QUEUED','RUNNING','SAVING','CANCELLING')")
    abstract suspend fun getInterruptedJobs(): List<StackJobEntity>

    @Upsert
    abstract suspend fun upsertJob(job: StackJobEntity)

    /** 进度回写只改计数与时间戳，避免把取消中的 CANCELLING 覆盖回 RUNNING。 */
    @Query("UPDATE stack_jobs SET processedCount = :processedCount, updatedAt = :updatedAt WHERE id = :id")
    abstract suspend fun updateProgress(id: String, processedCount: Int, updatedAt: Long)

    /** 只改状态，保留已写入的进度计数。 */
    @Query("UPDATE stack_jobs SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    abstract suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    @Query(
        "UPDATE stack_jobs SET status = :status, errorCode = :errorCode, errorMessage = :errorMessage, " +
            "stagingPath = NULL, updatedAt = :updatedAt WHERE id = :id",
    )
    abstract suspend fun updateFailure(
        id: String,
        status: String,
        errorCode: String?,
        errorMessage: String?,
        updatedAt: Long,
    )

    @Query("DELETE FROM stack_jobs WHERE id = :id")
    abstract suspend fun deleteJob(id: String)

    @Query("SELECT * FROM input_frames WHERE jobId = :jobId ORDER BY frameOrder ASC")
    abstract fun observeFrames(jobId: String): Flow<List<InputFrameEntity>>

    @Query("SELECT * FROM input_frames WHERE jobId = :jobId ORDER BY frameOrder ASC")
    abstract suspend fun getFrames(jobId: String): List<InputFrameEntity>

    @Upsert
    abstract suspend fun upsertFrames(frames: List<InputFrameEntity>)

    @Query("DELETE FROM input_frames WHERE jobId = :jobId")
    abstract suspend fun clearFrames(jobId: String)

    @Query(
        "SELECT COUNT(*) FROM stack_jobs WHERE status IN ('QUEUED','RUNNING','SAVING','CANCELLING') " +
            "AND id != :jobId",
    )
    abstract suspend fun activeJobCountExcept(jobId: String): Int

    @Query("SELECT stagingPath FROM stack_jobs WHERE stagingPath IS NOT NULL")
    abstract suspend fun stagingPaths(): List<String>

    /** 替换帧列表放在同一事务里，监听方不会读到中间的空列表。 */
    @Transaction
    open suspend fun replaceFrames(jobId: String, frames: List<InputFrameEntity>) {
        clearFrames(jobId)
        if (frames.isNotEmpty()) upsertFrames(frames)
    }

    @Transaction
    open suspend fun deleteJobWithFrames(jobId: String) {
        clearFrames(jobId)
        deleteJob(jobId)
    }
}

@Database(entities = [StackJobEntity::class, InputFrameEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stackDao(): StackDao
}
