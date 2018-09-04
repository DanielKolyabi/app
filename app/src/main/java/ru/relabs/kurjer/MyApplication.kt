package ru.relabs.kurjer

import android.app.Application
import android.arch.persistence.room.Room
import kotlinx.coroutines.experimental.launch
import ru.relabs.kurjer.models.AddressModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
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
    override fun onCreate() {
        super.onCreate()
        database = Room
                .databaseBuilder(applicationContext, ru.relabs.kurjer.persistence.AppDatabase::class.java, "deliveryman")
                .fallbackToDestructiveMigration()
                .build()

        val streets = listOf("Шевченко", "Ленина", "Арбатская", "Шахтёров", "Московская", "Швейная", "Голицкая", "Горняцкая", "Грибоедова")
        val publishers = listOf("Красная Ночь", "Вечерняя Москва", "Тёмная Речка", "Синяя Машина", "Голубой Вертолёт")
        launch {
            val tempAddresses = (0..80).map {
                AddressEntity(it,streets[Random().nextInt(streets.size)], it)
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