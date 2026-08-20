package com.somalapuram.pclauncher.di

import com.somalapuram.pclauncher.DesktopEnvironmentSource
import com.somalapuram.pclauncher.SafeModeApps
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets [com.somalapuram.pclauncher.HomeActivity] pull from the graph inside a guarded block. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeEntryPoint {
    fun desktopEnvironmentSource(): DesktopEnvironmentSource

    fun safeModeApps(): SafeModeApps
}
