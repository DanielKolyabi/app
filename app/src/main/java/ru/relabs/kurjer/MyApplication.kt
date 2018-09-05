package ru.relabs.kurjer

import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import kotlinx.coroutines.experimental.launch
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.entities.AddressEntity
import ru.relabs.kurjer.persistence.entities.TaskEntity
import ru.relabs.kurjer.persistence.entities.TaskItemEntity
import java.util.*

/**
 * Created by ProOrange on 30.08.2018.
 */

class MyApplication : Application() {
    lateinit var database: AppDatabase
    var user: UserModel = UserModel.Unauthorized
    lateinit var deviceUUID: String

    override fun onCreate() {
        super.onCreate()
        deviceUUID = getOrGenerateDeviceUUID()

        database = Room
                .databaseBuilder(applicationContext, ru.relabs.kurjer.persistence.AppDatabase::class.java, "deliveryman")
                .fallbackToDestructiveMigration()
                .build()

        fillDatabase(database)
    }

    fun storeUserCredentials() {
        if(user !is UserModel.Authorized) return
        getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                .edit()
                .putString("login", (user as UserModel.Authorized).login)
                .putString("token", (user as UserModel.Authorized).token)
                .apply()
    }

    fun getUserCredentials(): UserModel.Authorized? {
        val login = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("login", "-unknw")
        val token = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE).getString("token", "-unknw")
        if(token == "-unknw"){
            return null
        }
        return UserModel.Authorized(login = login, token = token)
    }

    fun restoreUserCredentials() {
        getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                .edit()
                .remove("login")
                .remove("token")
                .apply()
    }


    fun getOrGenerateDeviceUUID(): String {
        val sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        var deviceUUID = sharedPreferences.getString(
                "device_uuid", "unknown"
        )

        if (deviceUUID == "unknown") {
            deviceUUID = UUID.randomUUID().toString()
            sharedPreferences.edit()
                    .putString("device_uuid", deviceUUID)
                    .apply()
        }
        return deviceUUID
    }

    fun fillDatabase(database: AppDatabase) {
        val streets = listOf("Шевченко", "Ленина", "Арбатская", "Шахтёров", "Московская", "Швейная", "Голицкая", "Горняцкая", "Грибоедова")
        val publishers = listOf("Красная Ночь", "Вечерняя Москва", "Тёмная Речка", "Синяя Машина", "Голубой Вертолёт")
        launch {
            val tempAddresses = (0..80).map {
                AddressEntity(it, streets[Random().nextInt(streets.size)], it)
            }
            val tempTasks = (0..4).map {
                TaskEntity(it, publishers[Random().nextInt(publishers.size)], it, 1250, 10, 0, 5, 0,
                        Date(), Date(System.currentTimeMillis() + 86400000), 1, 13, "Петров Пётр Петрович",
                        "http://url.ru", 1, "Москва", "Адрес Склада",
                        null, null)
            }
            val tempTaskItems = (0..40).map {
                val freeEntrances = (1..20).toMutableList()
                val addr = tempAddresses[Random().nextInt(tempAddresses.size)]
                TaskItemEntity(addr.id, 0, it, listOf<String>(),
                        (0..Random().nextInt(16)).map {
                            freeEntrances.removeAt(Random().nextInt(freeEntrances.size))
                        },
                        1,
                        it / 10,
                        100,
                        it / 10

                )
            }
            if (database.addressDao().all.isEmpty()) {
                database.addressDao().insertAll(tempAddresses)
            }
            if (database.taskDao().all.isEmpty()) {
                database.taskDao().insertAll(tempTasks)
            }
            if (database.taskItemDao().all.isEmpty()) {
                database.taskItemDao().insertAll(tempTaskItems)
            }
        }
    }
}