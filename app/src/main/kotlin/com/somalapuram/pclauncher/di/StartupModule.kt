package com.somalapuram.pclauncher.di

import com.somalapuram.pclauncher.DesktopEnvironment
import com.somalapuram.pclauncher.DesktopEnvironmentSource
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
}
