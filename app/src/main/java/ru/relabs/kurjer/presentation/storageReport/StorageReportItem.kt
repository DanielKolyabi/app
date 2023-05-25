package ru.relabs.kurjer.presentation.storageReport

import android.net.Uri
import ru.relabs.kurjer.domain.models.StorageClosure
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.storage.StorageReportPhoto


sealed class StorageReportItem {
    data class Single(val required: Boolean, val hasPhoto: Boolean) : StorageReportItem()
    data class Photo(val photo: StorageReportPhoto, val photoUri: Uri) : StorageReportItem()


}
