package ru.relabs.kurjer.presentation.addresses

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.models.Address
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.providers.PathsProvider
import ru.relabs.kurjer.domain.repositories.TaskRepository
import ru.relabs.kurjer.presentation.base.tea.*
import java.io.File

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

data class AddressesState(
    val loaders: Int = 0,
    val tasks: List<Task> = emptyList(),
    val sorting: AddressesSortingMethod = AddressesSortingMethod.STANDARD,
    val searchFilter: String = "",
    val exits: Int = 0,
    val selectedListAddress: Address? = null
)

class AddressesContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

    val taskRepository: TaskRepository by inject()
    val taskEventController: TaskEventController by inject()
    val pathsProvider: PathsProvider by inject()

    var showImagePreview: (File) -> Unit = {}
    var showSnackbar: (msgRes: Int) -> Unit = {}
}

typealias AddressesMessage = ElmMessage<AddressesContext, AddressesState>
typealias AddressesEffect = ElmEffect<AddressesContext, AddressesState>
typealias AddressesRender = ElmRender<AddressesState>

enum class AddressesSortingMethod {
    STANDARD, ALPHABETIC
}