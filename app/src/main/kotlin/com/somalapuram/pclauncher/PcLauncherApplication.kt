package com.somalapuram.pclauncher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/*
 * NOTE (aosp branch): the explicit base class is required here, and is the only
 * source difference between this branch and main.
 *
 * Bare `@HiltAndroidApp` relies on the Hilt Gradle plugin, which rewrites the
 * class at build time to extend the generated Hilt_ base. Soong does not run
 * that plugin, and Hilt says so precisely:
 *
 *     [Hilt] Expected @HiltAndroidApp to have a value.
 *            Did you forget to apply the Gradle Plugin?
 *
 * Naming the base class in the annotation and extending Hilt_PcLauncherApplication
 * by hand is what the plugin would have done, and is the form AOSP itself uses --
 * see packages/modules/Bluetooth/tools/leaudiocompatibilitytool, which writes
 * `@AndroidEntryPoint(ComponentActivity::class) class MainActivity : Hilt_MainActivity()`.
 *
 * This form also works under Gradle, so main could adopt it and make the two
 * branches identical in source. Worth doing: it would leave Android.bp and the
 * manifest package attributes as the whole delta.
 */
@HiltAndroidApp(Application::class)
class PcLauncherApplication : Hilt_PcLauncherApplication()
