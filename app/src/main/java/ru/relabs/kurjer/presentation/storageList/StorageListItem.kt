package ru.relabs.kurjer.presentation.storageList

import ru.relabs.kurjer.domain.models.Task

sealed class StorageListItem {
    data class StorageItem(val task: Task) : StorageListItem()
    object Loader : StorageListItem()

}