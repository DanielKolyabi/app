package ru.relabs.kurjer.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import org.koin.android.ext.koin.androidApplication
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.DeliveryApp
import ru.relabs.kurjer.data.api.ApiProvider
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.migrations.Migrations
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.providers.*
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.RadiusRepository
import ru.relabs.kurjer.domain.storage.AppPreferences
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.domain.storage.CurrentUserStorage
import ru.relabs.kurjer.domain.useCases.AppUpdateUseCase
import ru.relabs.kurjer.domain.useCases.LoginUseCase
import ru.relabs.kurjer.domain.useCases.ReportUseCase
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.Router
import java.io.File

object Modules {
    val DELIVERY_URL = named("DeliveryUrl")
    val SHARED_PREFERENCES_NAME = named("SharedPrefName")
    val FILES_DIR = named("FilesDir")
    val CACHE_DIR = named("CacheDir")
    val APP_CONTEXT = named("AppContext")
}

val constModule = module {
    single<Context>(Modules.APP_CONTEXT) { androidApplication().applicationContext }
    single<String>(Modules.DELIVERY_URL) { BuildConfig.API_URL }
    single<String>(Modules.SHARED_PREFERENCES_NAME) { BuildConfig.APPLICATION_ID }
}

val navigationModule = module {
    single<Router> { (androidApplication() as DeliveryApp).router }
    single<NavigatorHolder> { (androidApplication() as DeliveryApp).navigatorHolder }
}

val fileSystemModule = module {
    single<File>(Modules.FILES_DIR) { androidApplication().filesDir }
    single<File>(Modules.CACHE_DIR) { androidApplication().cacheDir }
}

val storagesModule = module {
    single<SharedPreferences> { androidApplication().getSharedPreferences(get(Modules.SHARED_PREFERENCES_NAME), Context.MODE_PRIVATE) }
    single<AppPreferences> { AppPreferences(get<SharedPreferences>()) }
    single<AuthTokenStorage> { AuthTokenStorage(get<AppPreferences>()) }
    single<CurrentUserStorage> { CurrentUserStorage(get<AppPreferences>()) }

    single<DeviceUUIDProvider> {
        DeviceUUIDProvider(
            get<AppPreferences>()
        )
    }

    single<DeviceUniqueIdProvider> {
        DeviceUniqueIdProvider(androidApplication())
    }

    single<FirebaseTokenProvider> {
        FirebaseTokenProvider(get<AppPreferences>())
    }

    single<LocationProvider> {
        getLocationProvider(androidApplication())
    }

    single<AppDatabase> {
        Room.databaseBuilder(androidApplication(), AppDatabase::class.java, "deliveryman")
            .addMigrations(*Migrations.getMigrations())
            .fallbackToDestructiveMigration()
            .build()
    }

    single<ApiProvider> {
        ApiProvider(get<String>(Modules.DELIVERY_URL))
    }
}


val repositoryModule = module {

    single<DatabaseRepository> {
        DatabaseRepository(
            get<AppDatabase>(),
            get<AuthTokenStorage>(),
            get<String>(Modules.DELIVERY_URL)
        )
    }
    single<DeliveryRepository> {
        DeliveryRepository(
            get<ApiProvider>().practisApi,
            get<AuthTokenStorage>(),
            get<DeviceUUIDProvider>(),
            get<DeviceUniqueIdProvider>(),
            get<FirebaseTokenProvider>(),
            get<AppDatabase>()
        )
    }
    single<RadiusRepository> {
        RadiusRepository(
            get<DeliveryRepository>(),
            get<SharedPreferences>()
        )
    }
    single<PauseRepository> {
        PauseRepository(
            get<DeliveryRepository>(),
            get<SharedPreferences>(),
            get<DatabaseRepository>()
        )
    }
}
val useCasesModule = module {
    single<LoginUseCase> {
        LoginUseCase(
            get<DeliveryRepository>(),
            get<CurrentUserStorage>(),
            get<DatabaseRepository>(),
            get<RadiusRepository>(),
            get<AuthTokenStorage>(),
            get<PauseRepository>(),
            get<AppPreferences>()
        )
    }

    single<AppUpdateUseCase> {
        AppUpdateUseCase(
            get<DeliveryRepository>()
        )
    }

    single<ReportUseCase> {
        ReportUseCase(
            get<DatabaseRepository>(),
            get<PauseRepository>(),
            get<AuthTokenStorage>(),
            get<RadiusRepository>()
        )
    }
}

val eventControllers = module {
    single<TaskEventController> {
        TaskEventController()
    }
}

