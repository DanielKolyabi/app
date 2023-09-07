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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import ru.relabs.kurjer.data.backup.DataBackupController
import ru.relabs.kurjer.di.backupModule
import ru.relabs.kurjer.di.constModule
import ru.relabs.kurjer.di.eventControllers
import ru.relabs.kurjer.di.fileSystemModule
import ru.relabs.kurjer.di.navigationModule
import ru.relabs.kurjer.di.repositoryModule
import ru.relabs.kurjer.di.storagesModule
import ru.relabs.kurjer.di.useCasesModule
import ru.relabs.kurjer.utils.CustomLog
import timber.log.Timber


/**
 * Created by ProOrange on 30.08.2018.
 */

class DeliveryApp : Application(), KoinComponent {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val dataBackupController: DataBackupController by inject()

    private lateinit var cicerone: Cicerone<Router>

    val router: Router
        get() = cicerone.router
    val navigatorHolder: NavigatorHolder
        get() = cicerone.getNavigatorHolder()

    override fun onCreate() {
        super.onCreate()

        appContext = this
        Timber.plant(Timber.DebugTree())
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
        ioScope.launch {
            dataBackupController.startBackup()
        }


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