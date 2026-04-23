package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TweetDao {
    @Query("SELECT * FROM tweets WHERE isLocal = 1 ORDER BY timestamp DESC")
    fun getMyTweets(): Flow<List<Tweet>>

    @Query("SELECT * FROM tweets WHERE isLocal = 0 AND authorId = :authorId ORDER BY timestamp DESC")
    fun getPeerTweets(authorId: String): Flow<List<Tweet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTweet(tweet: Tweet)

    @Query("DELETE FROM tweets WHERE isLocal = 0 AND authorId = :authorId")
    suspend fun clearPeerTweets(authorId: String)

    @Delete
    suspend fun deleteTweet(tweet: Tweet)
}
