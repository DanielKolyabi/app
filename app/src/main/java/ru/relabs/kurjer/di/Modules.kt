package ru.relabs.kurjer.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.terrakok.cicerone.NavigatorHolder
import com.github.terrakok.cicerone.Router
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidApplication
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.DeliveryApp
import ru.relabs.kurjer.data.api.ApiProvider
import ru.relabs.kurjer.data.backup.DataBackupController
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.migrations.Migrations
import ru.relabs.kurjer.domain.controllers.ServiceEventController
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.providers.ConnectivityProvider
import ru.relabs.kurjer.domain.providers.DeviceUUIDProvider
import ru.relabs.kurjer.domain.providers.DeviceUniqueIdProvider
import ru.relabs.kurjer.domain.providers.FirebaseTokenProvider
import ru.relabs.kurjer.domain.providers.LocationProvider
import ru.relabs.kurjer.domain.providers.PathsProvider
import ru.relabs.kurjer.domain.providers.RoomBackupProvider
import ru.relabs.kurjer.domain.providers.getLocationProvider
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.PhotoRepository
import ru.relabs.kurjer.domain.repositories.QueryRepository
import ru.relabs.kurjer.domain.repositories.SettingsRepository
import ru.relabs.kurjer.domain.repositories.StorageRepository
import ru.relabs.kurjer.domain.repositories.TaskRepository
import ru.relabs.kurjer.domain.repositories.TextSizeStorage
import ru.relabs.kurjer.domain.storage.AppInitialStorage
import ru.relabs.kurjer.domain.storage.AppPreferences
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.domain.storage.CurrentUserStorage
import ru.relabs.kurjer.domain.storage.SavedUserStorage
import ru.relabs.kurjer.domain.useCases.AppUpdateUseCase
import ru.relabs.kurjer.domain.useCases.LoginUseCase
import ru.relabs.kurjer.domain.useCases.ReportUseCase
import ru.relabs.kurjer.domain.useCases.StorageReportUseCase
import ru.relabs.kurjer.domain.useCases.TaskUseCase
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
    single<SharedPreferences> {
        androidApplication().getSharedPreferences(
            get(Modules.SHARED_PREFERENCES_NAME),
            Context.MODE_PRIVATE
        )
    }
    single<AppPreferences> { AppPreferences(get<SharedPreferences>()) }
    single<AuthTokenStorage> { AuthTokenStorage(get<AppPreferences>()) }
    single<CurrentUserStorage> { CurrentUserStorage(get<AppPreferences>()) }
    single<SavedUserStorage> { SavedUserStorage(get<AppPreferences>()) }
    single { AppInitialStorage(appPreferences = get()) }

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
        getLocationProvider(androidApplication(), CoroutineScope(Dispatchers.Main))
    }

    single<AppDatabase> {
        Room.databaseBuilder(androidApplication(), AppDatabase::class.java, "deliveryman")
            .addMigrations(*Migrations.getMigrations())
            .fallbackToDestructiveMigration()
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
    }

    single<ApiProvider> {
        ApiProvider(get<String>(Modules.DELIVERY_URL))
    }

    single<PathsProvider> {
        PathsProvider(get<File>(Modules.FILES_DIR))
    }
    single { ConnectivityProvider(context = get()) }
}

val backupModule = module {
    single { RoomBackupProvider() }
    single {
        DataBackupController(
            savedUserStorage = get(),
            provider = get(),
            db = get(),
            pathsProvider = get(),
            authTokenStorage = get(),
            currentUserStorage = get(),
            filesRootDir = get<File>(Modules.FILES_DIR),
            settingsRepository = get(),
            appPreferences = get(),
            pauseRepository = get()
        )
    }
}

val repositoryModule = module {

    single { PhotoRepository(db = get(), pathsProvider = get()) }
    single {
        QueryRepository(
            db = get(),
            authTokenStorage = get(),
            baseUrl = get(Modules.DELIVERY_URL),
            pathsProvider = get()
        )
    }
    single<TaskRepository> {
        TaskRepository(
            get<AppDatabase>(),
            photoRepository = get(),
            queryRepository = get(),
            get<PathsProvider>()
        )
    }
    single<DeliveryRepository> {
        DeliveryRepository(
            get<ApiProvider>().deliveryApi,
            get<AuthTokenStorage>(),
            get<DeviceUUIDProvider>(),
            get<DeviceUniqueIdProvider>(),
            get<FirebaseTokenProvider>(),
            get<AppDatabase>(),
            get<ApiProvider>().httpClient,
            get<PathsProvider>(),
            get<StorageRepository>(),
            queryRepository = get()
        )
    }
    single<SettingsRepository> {
        SettingsRepository(
            get<DeliveryRepository>(),
            get<SharedPreferences>()
        )
    }
    single<PauseRepository> {
        PauseRepository(
            get<DeliveryRepository>(),
            get<SharedPreferences>(),
            queryRepository = get(),
            get<CurrentUserStorage>()
        )
    }
    single { StorageRepository(get<AppDatabase>(), get<PathsProvider>()) }
}

val useCasesModule = module {
    single<LoginUseCase> {
        LoginUseCase(
            get<DeliveryRepository>(),
            get<CurrentUserStorage>(),
            get<TaskRepository>(),
            get<SettingsRepository>(),
            get<AuthTokenStorage>(),
            get<PauseRepository>(),
            get<AppPreferences>(),
            get<SavedUserStorage>()
        )
    }

    single<AppUpdateUseCase> {
        AppUpdateUseCase(
            get<DeliveryRepository>(),
            get<PathsProvider>()
        )
    }

    single<ReportUseCase> {
        ReportUseCase(
            get<TaskRepository>(),
            queryRepository = get(),
            get<AuthTokenStorage>(),
            get<SettingsRepository>(),
            get<TaskEventController>()
        )
    }
    single {
        TaskUseCase(
            get<TaskRepository>()
        )
    }
    single {
        StorageReportUseCase(
            get<StorageRepository>(),
            get<PathsProvider>(),
            get<PauseRepository>(),
            get<LocationProvider>(),
            get<SettingsRepository>(),
            get<AuthTokenStorage>(),
            get<TaskRepository>()
        )
    }
    factory {
        TextSizeStorage(
            preferences = get<SharedPreferences>()
        )
    }
}

val eventControllers = module {
    single<TaskEventController> {
        TaskEventController()
    }
    single<ServiceEventController> {
        ServiceEventController()
    }
}

