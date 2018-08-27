package ru.relabs.kurjer.models

/**
 * Created by ProOrange on 09.08.2018.
 */
sealed class AddressElement {

    data class AddressModel(
            val address: String
    ) : AddressElement()

    data class TaskModel(
            val task: String
    ) : AddressElement()
}