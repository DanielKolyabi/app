package ru.relabs.kurjer.presentation.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_tasks.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.R
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.fragment.FragmentStyleable
import ru.relabs.kurjer.presentation.base.fragment.IFragmentStyleable
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.debugCollector
import ru.relabs.kurjer.presentation.base.tea.defaultController
import ru.relabs.kurjer.presentation.base.tea.rendersCollector
import ru.relabs.kurjer.presentation.base.tea.sendMessage
import ru.relabs.kurjer.presentation.host.HostActivity
import ru.relabs.kurjer.utils.debug
import ru.relabs.kurjer.utils.extensions.showSnackbar


/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

class TasksFragment : BaseFragment(),
    IFragmentStyleable by FragmentStyleable(false) {

    private val controller = defaultController(TasksState(), TasksContext())
    private var renderJob: Job? = null

    private val tasksAdapter = DelegateAdapter(
        TasksAdapter.loaderAdapter(),
        TasksAdapter.taskAdapter(
            { uiScope.sendMessage(controller, TasksMessages.msgTaskSelectClick(it)) },
            { uiScope.sendMessage(controller, TasksMessages.msgTaskClicked(it)) }
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val refreshTasks = arguments?.getBoolean(ARG_REFRESH_TASKS, false) ?: false
        controller.start(TasksMessages.msgInit(refreshTasks))
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
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager =
            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        view.rv_list.layoutManager = layoutManager
        view.rv_list.adapter = tasksAdapter

        bindControls(view)

        renderJob = uiScope.launch {
            val renders = listOf(
                TasksRenders.renderList(tasksAdapter),
                TasksRenders.renderLoading(view.loading),
                TasksRenders.renderStartButton(view.btn_start)
            )
            launch { controller.stateFlow().collect(rendersCollector(renders)) }
            launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
        }
        controller.context.errorContext.attach(view)
        controller.context.showSnackbar = { withContext(Dispatchers.Main) { showSnackbar(resources.getString(it)) } }
    }

    private fun bindControls(view: View) {
        view.iv_menu.setOnClickListener {
            (activity as? HostActivity)?.changeNavigationDrawerState()
        }
        view.btn_start.setOnClickListener {
            uiScope.sendMessage(controller, TasksMessages.msgStartClicked())
        }
        view.iv_update.setOnClickListener {
            uiScope.sendMessage(controller, TasksMessages.msgRefresh())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderJob?.cancel()
        controller.context.errorContext.detach()
        controller.context.showSnackbar = {}
    }

    override fun interceptBackPressed(): Boolean {
        return false
    }

    companion object {
        const val ARG_REFRESH_TASKS = "refresh_tasks"
        fun newInstance(refreshTasks: Boolean) = TasksFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_REFRESH_TASKS, refreshTasks)
            }
        }
    }
}