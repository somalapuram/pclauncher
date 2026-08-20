package com.somalapuram.pclauncher.di

import android.os.Process
import com.somalapuram.pclauncher.DesktopEnvironment
import com.somalapuram.pclauncher.DesktopEnvironmentSource
import com.somalapuram.pclauncher.SafeModeApps
import com.somalapuram.pclauncher.core.apps.AppSource
import com.somalapuram.pclauncher.platform.privileged.CapabilityDetector
import com.somalapuram.pclauncher.platform.privileged.UndetectedCapabilities
import com.somalapuram.pclauncher.platform.privileged.tierFor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StartupModule {

    @Provides
    @Singleton
    fun capabilityDetector(): CapabilityDetector = UndetectedCapabilities()

    @Provides
    @Singleton
    fun desktopEnvironmentSource(detector: CapabilityDetector): DesktopEnvironmentSource =
        DesktopEnvironmentSource { DesktopEnvironment(tier = tierFor(detector.detect())) }

    /**
     * Safe mode reads straight from the source, bypassing the repository, the icon cache and the
     * usage store — any of which may be what put the user here (GATE 4). A failure to list is
     * absorbed: an empty desktop still beats no desktop.
     */
    @Provides
    @Singleton
    fun safeModeApps(source: AppSource): SafeModeApps = SafeModeApps {
        runCatching { source.entriesFor(Process.myUserHandle()) }.getOrDefault(emptyList())
    }
}
