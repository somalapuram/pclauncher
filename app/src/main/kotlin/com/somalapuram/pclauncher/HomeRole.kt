package com.somalapuram.pclauncher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Owning the home role (SRS §14 phase 1, requirement 6).
 *
 * On `pc_x86_64` this is true by construction — pclauncher *is* the product's home app. In Stage A
 * the user has to choose it, so both a proper role request and a settings deep-link exist.
 */
object HomeRole {

    /** Is this app the one the system resolves HOME to? */
    fun isDefault(context: Context): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(home, 0)
        return isDefault(resolved?.activityInfo?.packageName, context.packageName)
    }

    /**
     * The comparison, separated from the lookup so both answers can be tested without standing up
     * a package manager. An unresolved HOME intent means nobody holds the role — which is *not*
     * us, and must not read as "already default" and hide the set-as-home affordance.
     */
    fun isDefault(resolvedHomePackage: String?, ownPackage: String): Boolean =
        resolvedHomePackage != null && resolvedHomePackage == ownPackage

    /**
     * How to ask. Prefers [RoleManager] and falls back to the home-app settings screen — the role
     * is not available on every build, and a dead button is worse than a longer path.
     */
    fun requestIntent(context: Context): Intent {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null &&
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        ) {
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }
}
