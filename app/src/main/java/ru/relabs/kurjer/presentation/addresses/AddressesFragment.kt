package ru.relabs.kurjer.presentation.addresses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_addresses.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.debugCollector
import ru.relabs.kurjer.presentation.base.tea.defaultController
import ru.relabs.kurjer.presentation.base.tea.rendersCollector
import ru.relabs.kurjer.presentation.base.tea.sendMessage
import ru.relabs.kurjer.presentation.host.HostActivity
import ru.relabs.kurjer.utils.debug


/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

class AddressesFragment : BaseFragment() {

    private val controller = defaultController(AddressesState(), AddressesContext())
    private var renderJob: Job? = null

    private val addressesAdapter = DelegateAdapter(
        AddressesAdapter.taskItemDelegate(
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
        AddressesAdapter.searchAdapter{
            uiScope.sendMessage(controller, AddressesMessages.msgSearch(it))
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskIds = arguments?.getParcelableArrayList<TaskId>(ARG_TASK_IDS)?.toList()
        if(taskIds == null){
            //TODO: Show error
            return
        }
        controller.start(AddressesMessages.msgInit(taskIds))
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

        val layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        view.rv_list.layoutManager = layoutManager
        view.rv_list.adapter = addressesAdapter

        bindControls(view)

        renderJob = uiScope.launch {
            val renders = listOf(
                AddressesRenders.renderLoading(view.loading),
                AddressesRenders.renderList(addressesAdapter)
            )
            launch { controller.stateFlow().collect(rendersCollector(renders)) }
            launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
        }
        controller.context.errorContext.attach(view)
    }

    private fun bindControls(view: View) {
        view.iv_menu.setOnClickListener {
            uiScope.sendMessage(controller, AddressesMessages.msgNavigateBack())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderJob?.cancel()
        controller.context.errorContext.detach()
    }

    override fun interceptBackPressed(): Boolean {
        return false
    }

    companion object {
        const val ARG_TASK_IDS = "task_ids"
        fun newInstance(taskIds: List<TaskId>) = AddressesFragment().apply {
            arguments = Bundle().apply{
                putParcelableArrayList(ARG_TASK_IDS, ArrayList(taskIds))
            }
        }
    }
}