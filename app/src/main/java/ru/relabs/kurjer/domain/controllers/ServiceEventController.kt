package ru.relabs.kurjer.domain.controllers

import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItemId

class ServiceEventController: BaseEventController<ServiceEvent>()

sealed class ServiceEvent{
    object Stop: ServiceEvent()
}