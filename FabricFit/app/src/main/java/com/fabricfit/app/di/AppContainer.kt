package com.fabricfit.app.di

import android.content.Context
import androidx.room.Room
import com.fabricfit.app.ads.AdProvider
import com.fabricfit.app.ads.DemoAdProvider
import com.fabricfit.app.analytics.AnalyticsLogger
import com.fabricfit.app.analytics.CrashReporter
import com.fabricfit.app.billing.BillingManager
import com.fabricfit.app.billing.DemoBillingManager
import com.fabricfit.app.analytics.DebugAnalyticsLogger
import com.fabricfit.app.data.local.db.FabricFitDatabase
import com.fabricfit.app.data.local.prefs.SettingsRepositoryImpl
import com.fabricfit.app.data.local.prefs.TooltipStore
import com.fabricfit.app.data.remote.AssetCatalogDataSource
import com.fabricfit.app.data.repository.DemoLookGenerator
import com.fabricfit.app.data.repository.DressStyleRepositoryImpl
import com.fabricfit.app.data.repository.EntitlementRepositoryImpl
import com.fabricfit.app.data.repository.FabricAnalysisRepositoryImpl
import com.fabricfit.app.data.repository.FavoritesRepositoryImpl
import com.fabricfit.app.data.repository.HistoryRepositoryImpl
import com.fabricfit.app.data.repository.VirtualTryOnRepositoryImpl
import com.fabricfit.app.domain.repository.FabricAnalysisRepository
import com.fabricfit.app.domain.repository.FavoritesRepository
import com.fabricfit.app.domain.repository.HistoryRepository
import com.fabricfit.app.domain.repository.SettingsRepository
import com.fabricfit.app.domain.repository.VirtualTryOnRepository
import com.fabricfit.app.media.BitmapAnalyzer
import com.fabricfit.app.media.ImageProcessor
import com.fabricfit.app.notifications.FabricFitNotifier

/**
 * Manual dependency container: explicit, testable, zero magic. Tests build the same repositories
 * with fakes.
 */
class AppContainer private constructor(context: Context) {

    private val appContext = context.applicationContext

    val database: FabricFitDatabase by lazy {
        Room.databaseBuilder(appContext, FabricFitDatabase::class.java, "fabricfit.db")
            // Pre-1.0 schema is allowed to evolve destructively; after release ship migrations.
            .fallbackToDestructiveMigration()
            .build()
    }

    val analytics: AnalyticsLogger = DebugAnalyticsLogger()
    val crashReporter = CrashReporter(appContext, analytics)
    val adProvider: AdProvider = DemoAdProvider()
    val notifier = FabricFitNotifier(appContext)

    val imageProcessor = ImageProcessor(appContext)
    val bitmapAnalyzer = BitmapAnalyzer(appContext)

    val settings: SettingsRepository = SettingsRepositoryImpl(appContext)
    val billing: BillingManager = DemoBillingManager(settings)
    val tooltips = TooltipStore(appContext)

    val history: HistoryRepository = HistoryRepositoryImpl(database.historyDao(), settings)

    val catalog = AssetCatalogDataSource(appContext)
    val dressStyles = DressStyleRepositoryImpl(database.dressStyleDao(), catalog)

    val mlClassifier = com.fabricfit.app.data.ml.OnDeviceMlClassifier(appContext, imageProcessor)

    val cloudAnalysis = com.fabricfit.app.data.remote.CloudAnalysisClient()
    val personSegmenter = com.fabricfit.app.data.ml.PersonSegmenter(appContext)

    val fabricAnalysis: FabricAnalysisRepository = FabricAnalysisRepositoryImpl(
        bitmapAnalyzer, database.fabricDao(), history, analytics, mlClassifier, settings, cloudAnalysis,
    )

    val favorites: FavoritesRepository = FavoritesRepositoryImpl(
        database.fabricDao(), database.dressStyleDao(), database.lookDao(),
    )

    val entitlements = EntitlementRepositoryImpl(database.usageDao(), settings)

    val collections: com.fabricfit.app.domain.repository.CollectionsRepository =
        com.fabricfit.app.data.repository.CollectionsRepositoryImpl(database.collectionDao())
    val wardrobe: com.fabricfit.app.domain.repository.WardrobeRepository =
        com.fabricfit.app.data.repository.WardrobeRepositoryImpl(database.wardrobeDao())
    val measurements: com.fabricfit.app.domain.repository.MeasurementsRepository =
        com.fabricfit.app.data.repository.MeasurementsRepositoryImpl(database.measurementDao())
    val feedback: com.fabricfit.app.domain.repository.FeedbackRepository =
        com.fabricfit.app.data.repository.FeedbackRepositoryImpl(database.styleFeedbackDao())
    val tailors: com.fabricfit.app.domain.repository.TailorRepository =
        com.fabricfit.app.data.remote.DemoTailorSource()

    val lookGenerator = DemoLookGenerator(imageProcessor)

    val tryOn: VirtualTryOnRepository = VirtualTryOnRepositoryImpl(
        database.lookDao(),
        database.fabricDao(),
        database.dressStyleDao(),
        lookGenerator,
        history,
        notifier,
        analytics,
        settings,
    )

    // Community features
    val community: com.fabricfit.app.data.repository.CommunityRepository =
        com.fabricfit.app.data.repository.CommunityRepository(
            userDao = database.communityUserDao(),
            galleryDao = database.galleryItemDao(),
            commentDao = database.commentDao(),
            likeDao = database.likeDao(),
            challengeDao = database.styleChallengeDao(),
            entryDao = database.challengeEntryDao(),
        )

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }

        /** Test hook. */
        fun reset() {
            instance = null
        }
    }
}
