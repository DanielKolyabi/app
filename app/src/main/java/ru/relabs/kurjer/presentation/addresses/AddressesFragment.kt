package ru.relabs.kurjer.presentation.addresses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_addresses.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.*
import ru.relabs.kurjer.utils.IntentUtils
import ru.relabs.kurjer.utils.debug
import ru.relabs.kurjer.utils.extensions.showDialog
import ru.relabs.kurjer.utils.extensions.showSnackbar


/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

class AddressesFragment : BaseFragment() {

    private val controller = defaultController(AddressesState(), AddressesContext())
    private var renderJob: Job? = null

    private val addressesAdapter = DelegateAdapter(
        AddressesAdapter.commonTaskItemDelegate(
            { item, task ->
                uiScope.sendMessage(controller, AddressesMessages.msgTaskItemClicked(item, task))
            },
            {
                uiScope.sendMessage(controller, AddressesMessages.msgTaskItemMapClicked(it))
            }
        ),
        AddressesAdapter.firmTaskItemDelegate(
            { item, task ->
                uiScope.sendMessage(controller, AddressesMessages.msgTaskItemClicked(item, task))
            },
            {
                uiScope.sendMessage(controller, AddressesMessages.msgTaskItemMapClicked(it))
            }
        ),
        AddressesAdapter.addressDelegate {
            uiScope.sendMessage(controller, AddressesMessages.msgAddressMapClicked(it))
        },
        AddressesAdapter.sortingAdapter {
            uiScope.sendMessage(controller, AddressesMessages.msgSortingChanged(it))
        },
        AddressesAdapter.loaderAdapter(),
        AddressesAdapter.blankAdapter(),
        AddressesAdapter.searchAdapter {
            uiScope.sendMessage(controller, AddressesMessages.msgSearch(it))
        },
        AddressesAdapter.otherAddressesAdapter {
            uiScope.sendMessage(controller, AddressesMessages.msgSearch(""))
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskIds = arguments?.getParcelableArrayList<TaskId>(ARG_TASK_IDS)?.toList()

        if (taskIds != null) {
            controller.start(AddressesMessages.msgInit(taskIds))
        } else {
            controller.start(msgEmpty())
            showDialog(
                getString(R.string.unknown_runtime_error_code, "af:100"),
                R.string.ok to { uiScope.sendMessage(controller, AddressesMessages.msgNavigateBack()) }
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
    ): View? {
        return inflater.inflate(R.layout.fragment_addresses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.rv_list.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        view.rv_list.adapter = addressesAdapter

        bindControls(view)

        renderJob = uiScope.launch {
            val renders = listOf(
                AddressesRenders.renderLoading(view.loading),
                AddressesRenders.renderList(addressesAdapter),
                AddressesRenders.renderTargetListAddress(addressesAdapter, view.rv_list)
            )
            launch { controller.stateFlow().collect(rendersCollector(renders)) }
            launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
        }
        controller.context.showImagePreview = {
            ContextCompat.startActivity(requireContext(), IntentUtils.getImageViewIntent(it, requireContext()), null)
        }
        controller.context.showSnackbar = { showSnackbar(getString(it)) }
        controller.context.errorContext.attach(view)
    }

    private fun bindControls(view: View) {
        view.iv_menu.setOnClickListener {
            uiScope.sendMessage(controller, AddressesMessages.msgNavigateBack())
        }

        view.btn_map.setOnClickListener {
            uiScope.sendMessage(controller, AddressesMessages.msgGlobalMapClicked())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        controller.context.showSnackbar = {}
        controller.context.showImagePreview = {}
        renderJob?.cancel()
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