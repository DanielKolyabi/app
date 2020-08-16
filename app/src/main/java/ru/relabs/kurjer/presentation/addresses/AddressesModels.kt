package ru.relabs.kurjer.presentation.addresses

import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.domain.controllers.TaskEventController
import ru.relabs.kurjer.domain.models.Address
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
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
    val isExited: Boolean = false,
    val selectedListAddress: Address? = null
)

class AddressesContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

    val databaseRepository: DatabaseRepository by inject()
    val taskEventController: TaskEventController by inject()

    var showImagePreview: (File) -> Unit = {}
    var showSnackbar: (msgRes: Int) -> Unit = {}
}

typealias AddressesMessage = ElmMessage<AddressesContext, AddressesState>
typealias AddressesEffect = ElmEffect<AddressesContext, AddressesState>
typealias AddressesRender = ElmRender<AddressesState>

enum class AddressesSortingMethod{
    STANDARD, ALPHABETIC
}