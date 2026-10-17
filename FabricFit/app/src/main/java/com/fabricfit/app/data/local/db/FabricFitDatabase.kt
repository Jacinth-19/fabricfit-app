package com.fabricfit.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FabricEntity::class,
        DressStyleEntity::class,
        FavoriteStyleEntity::class,
        GeneratedLookEntity::class,
        HistoryEntity::class,
        UsageEntity::class,
        CollectionEntity::class,
        CollectionItemEntity::class,
        WardrobeItemEntity::class,
        OutfitEntity::class,
        MeasurementEntity::class,
        StyleFeedbackEntity::class,
        // Community entities
        CommunityUserEntity::class,
        GalleryItemEntity::class,
        CommentEntity::class,
        LikeEntity::class,
        StyleChallengeEntity::class,
        ChallengeEntryEntity::class,
        RecentSearchEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class FabricFitDatabase : RoomDatabase() {
    abstract fun fabricDao(): FabricDao
    abstract fun dressStyleDao(): DressStyleDao
    abstract fun lookDao(): GeneratedLookDao
    abstract fun historyDao(): HistoryDao
    abstract fun usageDao(): UsageDao
    abstract fun collectionDao(): CollectionDao
    abstract fun wardrobeDao(): WardrobeDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun styleFeedbackDao(): StyleFeedbackDao
    // Community DAOs
    abstract fun communityUserDao(): CommunityUserDao
    abstract fun galleryItemDao(): GalleryItemDao
    abstract fun commentDao(): CommentDao
    abstract fun likeDao(): LikeDao
    abstract fun styleChallengeDao(): StyleChallengeDao
    abstract fun challengeEntryDao(): ChallengeEntryDao
    abstract fun recentSearchDao(): RecentSearchDao
}
