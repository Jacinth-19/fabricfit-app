package com.fabricfit.app.data.mapper

import com.fabricfit.app.data.local.db.ChallengeEntryEntity
import com.fabricfit.app.data.local.db.CommentEntity
import com.fabricfit.app.data.local.db.CommunityUserEntity
import com.fabricfit.app.data.local.db.GalleryItemEntity
import com.fabricfit.app.data.local.db.LikeEntity
import com.fabricfit.app.data.local.db.RecentSearchEntity
import com.fabricfit.app.data.local.db.ReportEntity
import com.fabricfit.app.data.local.db.StyleChallengeEntity
import com.fabricfit.app.domain.model.ChallengeEntry
import com.fabricfit.app.domain.model.Comment
import com.fabricfit.app.domain.model.CommunityUser
import com.fabricfit.app.domain.model.FabricCategory
import com.fabricfit.app.domain.model.GalleryItem
import com.fabricfit.app.domain.model.Like
import com.fabricfit.app.domain.model.Occasion
import com.fabricfit.app.domain.model.RecentSearch
import com.fabricfit.app.domain.model.Report
import com.fabricfit.app.domain.model.ReportStatus
import com.fabricfit.app.domain.model.ReportType
import com.fabricfit.app.domain.model.ReportedContentType
import com.fabricfit.app.domain.model.StyleChallenge

// CommunityUser mappers
fun CommunityUserEntity.toDomain() = CommunityUser(
    id = id,
    username = username,
    displayName = displayName,
    bio = bio,
    avatarUrl = avatarUrl,
    followerCount = followerCount,
    followingCount = followingCount,
    galleryCount = galleryCount,
    isFollowing = isFollowing,
    createdAt = createdAt,
)

fun CommunityUser.toEntity() = CommunityUserEntity(
    id = id,
    username = username,
    displayName = displayName,
    bio = bio,
    avatarUrl = avatarUrl,
    followerCount = followerCount,
    followingCount = followingCount,
    galleryCount = galleryCount,
    isFollowing = isFollowing,
    createdAt = createdAt,
)

// GalleryItem mappers
fun GalleryItemEntity.toDomain() = GalleryItem(
    id = id,
    userId = userId,
    username = username,
    userAvatar = userAvatar,
    title = title,
    description = description,
    imageUrl = imageUrl,
    mediaType = MediaType.valueOf(mediaType),
    videoUrl = videoUrl,
    videoDuration = videoDuration,
    thumbnailUrl = thumbnailUrl,
    fabricIds = if (fabricIds.isBlank()) emptyList() else fabricIds.split(",").map { it.toLong() },
    styleId = styleId,
    tags = if (tags.isBlank()) emptyList() else tags.split(","),
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = isLiked,
    createdAt = createdAt,
)

fun GalleryItem.toEntity() = GalleryItemEntity(
    id = id,
    userId = userId,
    username = username,
    userAvatar = userAvatar,
    title = title,
    description = description,
    imageUrl = imageUrl,
    mediaType = mediaType.name,
    videoUrl = videoUrl,
    videoDuration = videoDuration,
    thumbnailUrl = thumbnailUrl,
    fabricIds = fabricIds.joinToString(","),
    styleId = styleId,
    tags = tags.joinToString(","),
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = isLiked,
    createdAt = createdAt,
)

// Comment mappers
fun CommentEntity.toDomain() = Comment(
    id = id,
    galleryItemId = galleryItemId,
    userId = userId,
    username = username,
    userAvatar = userAvatar,
    text = text,
    createdAt = createdAt,
)

fun Comment.toEntity() = CommentEntity(
    id = id,
    galleryItemId = galleryItemId,
    userId = userId,
    username = username,
    userAvatar = userAvatar,
    text = text,
    createdAt = createdAt,
)

// Like mappers
fun LikeEntity.toDomain() = Like(
    id = id,
    galleryItemId = galleryItemId,
    userId = userId,
    createdAt = createdAt,
)

fun Like.toEntity() = LikeEntity(
    id = id,
    galleryItemId = galleryItemId,
    userId = userId,
    createdAt = createdAt,
)

// StyleChallenge mappers
fun StyleChallengeEntity.toDomain() = StyleChallenge(
    id = id,
    title = title,
    description = description,
    bannerUrl = bannerUrl,
    startDate = startDate,
    endDate = endDate,
    participantCount = participantCount,
    requiredFabricTypes = if (requiredFabricTypes.isBlank()) {
        emptyList()
    } else {
        requiredFabricTypes.split(",").mapNotNull { 
            runCatching { FabricCategory.valueOf(it) }.getOrNull() 
        }
    },
    requiredOccasions = if (requiredOccasions.isBlank()) {
        emptyList()
    } else {
        requiredOccasions.split(",").mapNotNull { 
            runCatching { Occasion.valueOf(it) }.getOrNull() 
        }
    },
    prizes = if (prizes.isBlank()) emptyList() else prizes.split(","),
    isActive = isActive,
    isParticipating = isParticipating,
)

fun StyleChallenge.toEntity() = StyleChallengeEntity(
    id = id,
    title = title,
    description = description,
    bannerUrl = bannerUrl,
    startDate = startDate,
    endDate = endDate,
    participantCount = participantCount,
    requiredFabricTypes = requiredFabricTypes.joinToString(",") { it.name },
    requiredOccasions = requiredOccasions.joinToString(",") { it.name },
    prizes = prizes.joinToString(","),
    isActive = isActive,
    isParticipating = isParticipating,
)

// ChallengeEntry mappers
fun ChallengeEntryEntity.toDomain() = ChallengeEntry(
    id = id,
    challengeId = challengeId,
    userId = userId,
    username = username,
    userAvatar = userAvatar,
    galleryItemId = galleryItemId,
    submissionDate = submissionDate,
    voteCount = voteCount,
    isVoted = isVoted,
)

fun ChallengeEntry.toEntity() = ChallengeEntryEntity(
    id = id,
    challengeId = challengeId,
    userId = userId,
    username = username,
    userAvatar = userAvatar,
    galleryItemId = galleryItemId,
    submissionDate = submissionDate,
    voteCount = voteCount,
    isVoted = isVoted,
)

// RecentSearch mappers
fun RecentSearchEntity.toDomain() = RecentSearch(
    query = query,
    timestamp = timestamp,
)

fun RecentSearch.toEntity() = RecentSearchEntity(
    query = query,
    timestamp = timestamp,
)

// Report mappers
fun ReportEntity.toDomain() = Report(
    id = id,
    reporterId = reporterId,
    reportedUserId = reportedUserId,
    contentType = ReportedContentType.valueOf(contentType),
    contentId = contentId,
    reportType = ReportType.valueOf(reportType),
    description = description,
    status = ReportStatus.valueOf(status),
    createdAt = createdAt,
    resolvedAt = resolvedAt,
    resolution = resolution,
)

fun Report.toEntity() = ReportEntity(
    id = id,
    reporterId = reporterId,
    reportedUserId = reportedUserId,
    contentType = contentType.name,
    contentId = contentId,
    reportType = reportType.name,
    description = description,
    status = status.name,
    createdAt = createdAt,
    resolvedAt = resolvedAt,
    resolution = resolution,
)
