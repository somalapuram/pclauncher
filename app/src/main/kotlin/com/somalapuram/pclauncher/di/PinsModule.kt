package com.somalapuram.pclauncher.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.somalapuram.pclauncher.core.data.pins.DataStorePinStore
import com.somalapuram.pclauncher.core.data.pins.PinStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.pinsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pins")

@Module
@InstallIn(SingletonComponent::class)
object PinsModule {

    @Provides
    @Singleton
    fun pinStore(@ApplicationContext context: Context): PinStore =
        DataStorePinStore(context.pinsDataStore)
}
