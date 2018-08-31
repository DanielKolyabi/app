package ru.relabs.kurjer

import android.app.Application
import android.arch.persistence.room.Room
import kotlinx.coroutines.experimental.launch
import ru.relabs.kurjer.models.AddressModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.persistence.AppDatabase
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
                .build()

//        val streets = listOf("Шевченко", "Ленина", "Арбатская", "Шахтёров", "Московская")
//        val publishers = listOf("Красная Ночь", "Вечерняя Москва", "Тёмная Речка", "Синяя Машина", "Голубой Вертолёт")
//        launch {
//            val tempAddresses = (0..20).map {
//                AddressModel(it, "ул. ${streets[Random().nextInt(streets.size)]}, д. $it")
//            }
//            val tempTasks = (0..20).map {
//                TaskModel(it, publishers[Random().nextInt(publishers.size)], it, 1250, 10, 0, 5, 0,
//                        Date(), Date(System.currentTimeMillis() + 86400000), 1, 13, "Петров Пётр Петрович",
//                        "http://url.ru", 1, listOf<TaskItemModel>(), "Москва", "Адрес Склада", false)
//            }
//            val tempTaskItems = (0..200).map {
//                val addr = tempAddresses[Random().nextInt(tempAddresses.size)]
//                TaskItemModel(addr, addr.id, 0, it, listOf<String>(),
//                        (0..Random().nextInt(16)).map {
//                            Random().nextInt(20)
//                        },
//                        1,
//                        it / 10,
//                        100,
//                        it / 10
//
//                )
//            }
//            if (database.addressDao().all.isEmpty()) {
//                database.addressDao().insertAll(tempAddresses)
//            }
//            if (database.taskDao().all.isEmpty()) {
//                database.taskDao().insertAll(tempTasks)
//            }
//            if (database.taskItemDao().all.isEmpty()) {
//                database.taskItemDao().insertAll(tempTaskItems)
//            }
//        }
    }
}