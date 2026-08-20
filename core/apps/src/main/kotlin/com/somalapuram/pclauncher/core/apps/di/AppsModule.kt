package com.somalapuram.pclauncher.core.apps.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.somalapuram.pclauncher.core.apps.AppSource
import com.somalapuram.pclauncher.core.apps.DataStoreUsageStore
import com.somalapuram.pclauncher.core.apps.LauncherAppsSource
import com.somalapuram.pclauncher.core.apps.LocalUsageSignals
import com.somalapuram.pclauncher.core.apps.SystemUsageSignals
import com.somalapuram.pclauncher.core.apps.AppInventoryRepository
import com.somalapuram.pclauncher.core.apps.UsageSignalSource
import com.somalapuram.pclauncher.core.apps.UsageSignals
import com.somalapuram.pclauncher.core.apps.UsageStore
import com.somalapuram.pclauncher.core.apps.hasUsageAccess
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

private val Context.usageDataStore: DataStore<Preferences> by preferencesDataStore(name = "usage")

@Module
@InstallIn(SingletonComponent::class)
object AppsModule {

    @Provides
    @Singleton
    fun appSource(@ApplicationContext context: Context): AppSource = LauncherAppsSource(context)

    @Provides
    @Singleton
    fun usageStore(@ApplicationContext context: Context): UsageStore =
        DataStoreUsageStore(context.usageDataStore)

    @Provides
    @Singleton
    fun appInventoryRepository(source: AppSource): AppInventoryRepository =
        AppInventoryRepository(source, Dispatchers.IO)

    @Provides
    @Singleton
    fun usageSignals(
        @ApplicationContext context: Context,
        repository: AppInventoryRepository,
        store: UsageStore,
    ): UsageSignalSource = UsageSignals(
        // Evaluated per call, not captured: usage access can be granted or revoked while the
        // shell is running and the answer has to follow.
        hasUsageAccess = { hasUsageAccess(context) },
        system = SystemUsageSignals(context, entries = { repository.inventory.value.entries }),
        local = LocalUsageSignals(store),
    )
}
