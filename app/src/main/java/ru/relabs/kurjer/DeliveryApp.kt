package ru.relabs.kurjer

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.StrictMode
import com.github.terrakok.cicerone.Cicerone
import com.github.terrakok.cicerone.NavigatorHolder
import com.github.terrakok.cicerone.Router
import com.instacart.library.truetime.TrueTime
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.relabs.kurjer.di.*
import ru.relabs.kurjer.utils.CustomLog
import java.util.*


/**
 * Created by ProOrange on 30.08.2018.
 */

class DeliveryApp : Application() {
    private val scope = CoroutineScope(Dispatchers.Default)

    private lateinit var cicerone: Cicerone<Router>

    val router: Router
        get() = cicerone.router
    val navigatorHolder: NavigatorHolder
        get() = cicerone.getNavigatorHolder()

    override fun onCreate() {
        super.onCreate()

        appContext = this
        MapKitFactory.setApiKey(BuildConfig.YA_KEY)

        cicerone = Cicerone.create()

        launchTrueTimeInit()

        startKoin {
            androidContext(this@DeliveryApp)
            modules(
                listOf(
                    constModule,
                    navigationModule,
                    fileSystemModule,
                    eventControllers,
                    storagesModule,
                    repositoryModule,
                    useCasesModule,
                    backupModule
                )
            )
        }

        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
    }

    private fun launchTrueTimeInit() {
        TrueTime.clearCachedInfo()
        scope.launch(Dispatchers.IO) {
            while (!initTrueTime()) {
                delay(500)
            }
            CustomLog.writeToFile("True Time initialized")
        }
    }

    @SuppressLint("HardwareIds")
    private fun initTrueTime(): Boolean {
        return try {
            TrueTime
                .build()
                .initialize()
            true
        } catch (e: Exception) {
            false
        }
    }
    companion object {
        lateinit var appContext: Context
    }
}