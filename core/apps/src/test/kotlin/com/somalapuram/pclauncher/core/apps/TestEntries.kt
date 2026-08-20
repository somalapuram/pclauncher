package com.somalapuram.pclauncher.core.apps

import android.content.ComponentName
import android.os.UserHandle

/**
 * `UserHandle` has no public constructor, but `getUserHandleForUid` is public API — uid/100000 is
 * the user id, so this yields real handles for a personal (0) and a work (10) profile.
 */
object TestUsers {
    val personal: UserHandle = UserHandle.getUserHandleForUid(0)
    val work: UserHandle = UserHandle.getUserHandleForUid(10 * 100_000)
}

fun entry(
    label: String,
    packageName: String = "com.example." + label.lowercase().filter { it.isLetterOrDigit() },
    user: UserHandle = TestUsers.personal,
    profile: ProfileKind = ProfileKind.Personal,
    suspended: Boolean = false,
    available: Boolean = true,
    resizeable: Boolean = true,
    versionCode: Long = 1L,
): AppEntry = AppEntry(
    key = AppKey(ComponentName(packageName, "$packageName.Main"), user),
    label = label,
    packageName = packageName,
    profile = profile,
    isSuspended = suspended,
    isAvailable = available,
    isResizeable = resizeable,
    versionCode = versionCode,
)

fun inventoryOf(vararg entries: AppEntry) =
    AppInventory(entries = sortedByLabel(entries.toList()), isComplete = true)

/** An [AppSource] with no framework behind it — what makes every test here device-free. */
class FakeAppSource(
    private val byProfile: Map<UserHandle, List<AppEntry>>,
) : AppSource {
    var observing = false
        private set
    private var emit: ((AppChange) -> Unit)? = null

    override fun profiles(): List<UserHandle> = byProfile.keys.toList()

    override fun entriesFor(user: UserHandle): List<AppEntry> = byProfile[user].orEmpty()

    override fun entriesFor(packageName: String, user: UserHandle): List<AppEntry> =
        entriesFor(user).filter { it.packageName == packageName }

    override fun observeChanges(onChange: (AppChange) -> Unit): AutoCloseable {
        observing = true
        emit = onChange
        return AutoCloseable { observing = false; emit = null }
    }

    fun push(change: AppChange) = emit?.invoke(change)
}
