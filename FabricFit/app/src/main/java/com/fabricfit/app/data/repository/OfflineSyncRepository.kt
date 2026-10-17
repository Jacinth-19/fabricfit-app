package com.fabricfit.app.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fabricfit.app.di.AppContainer
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Repository for managing offline data synchronization.
 */
class OfflineSyncRepository(private val context: Context) {
    
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Queue a post for upload when network is available.
     */
    fun queuePostForUpload(
        title: String,
        description: String,
        imageUri: String?,
        videoUri: String?,
        mediaType: String,
    ) {
        val data = Data.Builder()
            .putString("title", title)
            .putString("description", description)
            .putString("imageUri", imageUri)
            .putString("videoUri", videoUri)
            .putString("mediaType", mediaType)
            .putLong("timestamp", System.currentTimeMillis())
            .build()
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val uploadWork = OneTimeWorkRequestBuilder<PostUploadWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .addTag("post_upload")
            .build()
        
        workManager.enqueueUniqueWork(
            "post_upload_${System.currentTimeMillis()}",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            uploadWork
        )
    }
    
    /**
     * Queue a comment for upload.
     */
    fun queueCommentForUpload(postId: String, commentText: String) {
        val data = Data.Builder()
            .putString("postId", postId)
            .putString("commentText", commentText)
            .putLong("timestamp", System.currentTimeMillis())
            .build()
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val uploadWork = OneTimeWorkRequestBuilder<CommentUploadWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .addTag("comment_upload")
            .build()
        
        workManager.enqueueUniqueWork(
            "comment_upload_${System.currentTimeMillis()}",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            uploadWork
        )
    }
    
    /**
     * Queue a like/unlike action for sync.
     */
    fun queueLikeForSync(postId: String, isLiked: Boolean) {
        val data = Data.Builder()
            .putString("postId", postId)
            .putBoolean("isLiked", isLiked)
            .putLong("timestamp", System.currentTimeMillis())
            .build()
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncWork = OneTimeWorkRequestBuilder<LikeSyncWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .addTag("like_sync")
            .build()
        
        workManager.enqueueUniqueWork(
            "like_sync_${System.currentTimeMillis()}",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncWork
        )
    }
    
    /**
     * Cancel all pending uploads.
     */
    fun cancelAllPendingUploads() {
        workManager.cancelAllWorkByTag("post_upload")
        workManager.cancelAllWorkByTag("comment_upload")
        workManager.cancelAllWorkByTag("like_sync")
    }
    
    /**
     * Get pending upload count.
     */
    fun getPendingUploadCount(): Int {
        // In production, query WorkManager for pending work
        return 0
    }
}

/**
 * Worker for uploading posts.
 */
class PostUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val title = inputData.getString("title") ?: return Result.failure()
            val description = inputData.getString("description") ?: return Result.failure()
            val imageUri = inputData.getString("imageUri")
            val videoUri = inputData.getString("videoUri")
            val mediaType = inputData.getString("mediaType") ?: "IMAGE"
            
            // Get app container
            val container = AppContainer.get(applicationContext)
            
            // Simulate network call
            delay(2000)
            
            // Upload post
            container.community.createGalleryItem(
                userId = "current_user_id", // Would get from auth
                title = title,
                description = description,
                imageUrls = if (mediaType == "IMAGE" && imageUri != null) listOf(imageUri) else emptyList(),
                fabricIds = emptyList(),
                styleId = null,
                mediaType = com.fabricfit.app.domain.model.MediaType.valueOf(mediaType),
                videoUrl = if (mediaType == "VIDEO") videoUri else null,
            )
            
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

/**
 * Worker for uploading comments.
 */
class CommentUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val postId = inputData.getString("postId") ?: return Result.failure()
            val commentText = inputData.getString("commentText") ?: return Result.failure()
            
            val container = AppContainer.get(applicationContext)
            
            // Simulate network call
            delay(1000)
            
            // Add comment
            container.community.addComment(
                itemId = postId,
                userId = "current_user_id", // Would get from auth
                text = commentText,
            )
            
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

/**
 * Worker for syncing likes.
 */
class LikeSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val postId = inputData.getString("postId") ?: return Result.failure()
            val isLiked = inputData.getBoolean("isLiked", false)
            
            val container = AppContainer.get(applicationContext)
            
            // Simulate network call
            delay(500)
            
            // Toggle like
            if (isLiked) {
                container.community.likeItem(postId, "current_user_id")
            } else {
                container.community.unlikeItem(postId, "current_user_id")
            }
            
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
