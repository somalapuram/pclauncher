package com.somalapuram.pclauncher.di

import com.somalapuram.pclauncher.DesktopEnvironmentSource
import com.somalapuram.pclauncher.SafeModeApps
import com.somalapuram.pclauncher.core.apps.AppInventoryRepository
import com.somalapuram.pclauncher.core.apps.IconCache
import com.somalapuram.pclauncher.core.data.layout.DesktopLayoutStore
import com.somalapuram.pclauncher.core.data.pins.PinStore
import com.somalapuram.pclauncher.core.data.prompts.PromptStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets [com.somalapuram.pclauncher.HomeActivity] pull from the graph inside a guarded block. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeEntryPoint {
    fun desktopEnvironmentSource(): DesktopEnvironmentSource

    fun safeModeApps(): SafeModeApps

    fun appInventoryRepository(): AppInventoryRepository

    fun iconCache(): IconCache

    fun pinStore(): PinStore

    fun desktopLayoutStore(): DesktopLayoutStore

    fun promptStore(): PromptStore
}
