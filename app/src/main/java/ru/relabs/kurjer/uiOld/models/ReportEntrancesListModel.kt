package ru.relabs.kurjer.uiOld.models

import ru.relabs.kurjer.models.TaskItemModel

/**
 * Created by ProOrange on 30.08.2018.
 */

sealed class ReportEntrancesListModel {
    data class Entrance(
            val taskItem: TaskItemModel,
            val entranceNumber: Int,
            var selected: Int,
            var coupleEnabled: Boolean,
            val hasPhoto: Boolean
    ) : ReportEntrancesListModel()
}