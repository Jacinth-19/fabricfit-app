package com.fabricfit.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.repository.DressStyleRepository
import com.fabricfit.app.domain.repository.HistoryRepository
import com.fabricfit.app.domain.repository.SettingsRepository
import com.fabricfit.app.notifications.FabricFitNotifier

/** Keeps the catalogue fresh when a backend is configured; harmless with the asset source. */
class CatalogRefreshWorker(
    context: Context,
    params: WorkerParameters,
    private val styles: DressStyleRepository = AppContainer.get(context).dressStyles,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (styles.refresh()) {
        is com.fabricfit.app.domain.model.Outcome.Success -> Result.success()
        else -> Result.retry()
    }
}

/** Weekly inspiration — runs only when the user has explicitly opted in. */
class InspirationWorker(
    context: Context,
    params: WorkerParameters,
    private val settings: SettingsRepository = AppContainer.get(context).settings,
    private val notifier: FabricFitNotifier = AppContainer.get(context).notifier,
    private val styles: DressStyleRepository = AppContainer.get(context).dressStyles,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = settings.current()
        if (!prefs.notifications.inspiration) return Result.success()
        val pick = styles.all().randomOrNull() ?: return Result.success()
        notifier.recommendation(pick.titleKey, pick.id)
        return Result.success()
    }
}

/** Honours the history-retention preference. */
class HistoryPruneWorker(
    context: Context,
    params: WorkerParameters,
    private val settings: SettingsRepository = AppContainer.get(context).settings,
    private val history: HistoryRepository = AppContainer.get(context).history,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val days = settings.current().keepHistoryDays
        if (days > 0) history.pruneOlderThan(days)
        return Result.success()
    }
}
