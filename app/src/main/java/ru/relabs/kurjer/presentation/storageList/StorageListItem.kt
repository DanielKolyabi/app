package ru.relabs.kurjer.presentation.storageList

import ru.relabs.kurjer.domain.models.Task

sealed class StorageListItem {
    data class StorageAddress(val storage: Task.Storage, val tasks: List<StorageListState.TaskWrapper>) :
        StorageListItem()

    object Loader : StorageListItem()

}