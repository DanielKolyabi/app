package ru.relabs.kurjer.presentation.host.systemWatchers

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.presentation.host.featureCheckers.MockedLocationChecker
import java.util.concurrent.TimeUnit


class MockedLocationWatcher(
    a: Activity,
    private val mockedLocationChecker: MockedLocationChecker
) : SystemWatcher(a, TimeUnit.SECONDS.toMillis(3)) {
    private val ctx: Context = a

    override suspend fun onWatcherTick() {
        super.onWatcherTick()
        if (!mockedLocationChecker.isFeatureEnabled()) {
            if(getAllMockGPSApps().isNotEmpty()){
                withContext(Dispatchers.Main) {
                    mockedLocationChecker.requestFeature()
                }
            }else{
                mockedLocationChecker.reset()
            }
        }
    }

    private fun getAllMockGPSApps(): List<String> {
        return ctx.packageManager.getInstalledApplications(PackageManager.GET_META_DATA).filter {
            val isSystemPackage = it.flags and ApplicationInfo.FLAG_SYSTEM != 0
            !isSystemPackage && hasAppPermission(ctx, it.packageName, "android.permission.ACCESS_MOCK_LOCATION")
        }.map {
            it.packageName
        }
    }

    fun hasAppPermission(context: Context, app: String, permission: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(app, PackageManager.GET_PERMISSIONS)?.requestedPermissions?.contains(permission) ?: false
        } catch (e: NameNotFoundException) {
            return false
        }
    }
}