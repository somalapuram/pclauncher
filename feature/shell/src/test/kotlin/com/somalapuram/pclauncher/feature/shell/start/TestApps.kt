package com.somalapuram.pclauncher.feature.shell.start

import android.content.ComponentName
import android.os.UserHandle
import com.somalapuram.pclauncher.core.apps.AppEntry
import com.somalapuram.pclauncher.core.apps.AppKey
import com.somalapuram.pclauncher.core.apps.ProfileKind

val testUser: UserHandle get() = UserHandle.getUserHandleForUid(0)

fun app(label: String, pkg: String = "com.example." + label.lowercase().filter { it.isLetterOrDigit() }) =
    AppEntry(
        key = AppKey(ComponentName(pkg, "$pkg.Main"), testUser),
        label = label,
        packageName = pkg,
        profile = ProfileKind.Personal,
    )

fun AppEntry.id(): String = key.component.flattenToShortString()
