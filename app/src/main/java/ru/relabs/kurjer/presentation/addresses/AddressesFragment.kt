package ru.relabs.kurjer.presentation.addresses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.presentation.base.compose.common.themes.DeliveryTheme
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.defaultController
import ru.relabs.kurjer.presentation.base.tea.msgEmpty
import ru.relabs.kurjer.presentation.base.tea.sendMessage
import ru.relabs.kurjer.utils.extensions.showDialog
import ru.relabs.kurjer.utils.extensions.showSnackbar


/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

class AddressesFragment : BaseFragment() {

    private val controller = defaultController(AddressesState(), AddressesContext())
//    private var renderJob: Job? = null

//    private val addressesAdapter = DelegateAdapter(
//        AddressesAdapter.commonTaskItemDelegate(
//            { item, task ->
//                uiScope.sendMessage(controller, AddressesMessages.msgTaskItemClicked(item, task))
//            },
//            {
//                uiScope.sendMessage(controller, AddressesMessages.msgTaskItemMapClicked(it))
//            }
//        ),
//        AddressesAdapter.firmTaskItemDelegate(
//            { item, task ->
//                uiScope.sendMessage(controller, AddressesMessages.msgTaskItemClicked(item, task))
//            },
//            {
//                uiScope.sendMessage(controller, AddressesMessages.msgTaskItemMapClicked(it))
//            }
//        ),
//        AddressesAdapter.addressDelegate {
//            uiScope.sendMessage(controller, AddressesMessages.msgAddressMapClicked(it))
//        },
//        AddressesAdapter.sortingAdapter {
//            uiScope.sendMessage(controller, AddressesMessages.msgSortingChanged(it))
//        },
//        AddressesAdapter.loaderAdapter(),
//        AddressesAdapter.blankAdapter(),
//        AddressesAdapter.searchAdapter {
//            uiScope.sendMessage(controller, AddressesMessages.msgSearch(it))
//        },
//        AddressesAdapter.otherAddressesAdapter {
//            uiScope.sendMessage(controller, AddressesMessages.msgSearch(""))
//        },
//        AddressesAdapter.storageAdapter {
//            uiScope.sendMessage(controller, AddressesMessages.msgStorageClicked())
//        }
//
//    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskIds = arguments?.getParcelableArrayList<TaskId>(ARG_TASK_IDS)?.toList()

        if (taskIds != null) {
            controller.start(AddressesMessages.msgInit(taskIds))
        } else {
            controller.start(msgEmpty())
            showDialog(
                getString(R.string.unknown_runtime_error_code, "af:100"),
                R.string.ok to {
                    uiScope.sendMessage(
                        controller,
                        AddressesMessages.msgNavigateBack()
                    )
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DeliveryTheme {
                    AddressesScreen(controller)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.context.showSnackbar = { showSnackbar(getString(it)) }
        controller.context.errorContext.attach(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        controller.context.showSnackbar = {}
        controller.context.showImagePreview = {}
        controller.context.errorContext.detach()
    }

    override fun interceptBackPressed(): Boolean {
        return false
    }

    companion object {
        const val ARG_TASK_IDS = "task_ids"
        fun newInstance(taskIds: List<TaskId>) = AddressesFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(ARG_TASK_IDS, ArrayList(taskIds))
            }
        }
    }
}