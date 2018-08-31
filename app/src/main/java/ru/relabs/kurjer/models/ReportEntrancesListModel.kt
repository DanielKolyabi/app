package ru.relabs.kurjer.models

/**
 * Created by ProOrange on 30.08.2018.
 */

sealed class ReportEntrancesListModel{
    data class Entrance(val taskItem: TaskItemModel, val entranceNumber: Int): ReportEntrancesListModel()
}