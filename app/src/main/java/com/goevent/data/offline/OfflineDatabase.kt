package com.goevent.data.offline

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** A saved copy of a server response. json holds the data, savedAt is epoch millis. */
@Entity(tableName = "cache")
data class CacheEntity(
    @PrimaryKey val cacheKey: String,
    val json: String,
    val savedAt: Long
)

/** One change made while offline, waiting to be sent. type is FAVORITE or NOTIFICATION_READ. */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val userId: String,
    val targetId: String,
    val value: String,
    val createdAt: Long
)

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): CacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: CacheEntity)
}

@Dao
interface PendingDao {
    @Insert
    suspend fun insert(action: PendingActionEntity): Long

    @Query("SELECT * FROM pending_actions ORDER BY id ASC")
    suspend fun all(): List<PendingActionEntity>

    @Query("SELECT COUNT(*) FROM pending_actions")
    fun count(): Flow<Int>

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun delete(id: Long)

    /** Keeps only the latest queued change for the same item. */
    @Query("DELETE FROM pending_actions WHERE type = :type AND userId = :userId AND targetId = :targetId")
    suspend fun deleteFor(type: String, userId: String, targetId: String)
}

@Database(
    entities = [CacheEntity::class, PendingActionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GoEventDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
    abstract fun pendingDao(): PendingDao

    companion object {
        fun create(context: Context): GoEventDatabase =
            Room.databaseBuilder(context, GoEventDatabase::class.java, "goevent_offline.db").build()
    }
}