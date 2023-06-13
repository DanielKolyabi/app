package ru.relabs.kurjer.presentation.tasks

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.presentation.base.compose.common.themes.DeliveryTheme
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.fragment.FragmentStyleable
import ru.relabs.kurjer.presentation.base.fragment.IFragmentStyleable
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.defaultController
import ru.relabs.kurjer.presentation.base.tea.sendMessage
import ru.relabs.kurjer.presentation.host.HostActivity
import ru.relabs.kurjer.presentation.taskDetails.IExaminedConsumer
import ru.relabs.kurjer.uiOld.fragments.YandexMapFragment
import ru.relabs.kurjer.utils.extensions.showDialog
import ru.relabs.kurjer.utils.extensions.showSnackbar


/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

class TasksFragment : BaseFragment(),
    IFragmentStyleable by FragmentStyleable(false),
    IExaminedConsumer {

    private val controller = defaultController(TasksState(), TasksContext(this))
    private var renderJob: Job? = null
    private var shouldShowUpdateRequiredOnResume: Boolean = false
    private var canSkipUpdate: Boolean = false
    private var taskUpdateRequiredDialogShowed: Boolean = false

    private val tasksAdapter = DelegateAdapter(
        TasksAdapter.loaderAdapter(),
        TasksAdapter.taskAdapter(
            { uiScope.sendMessage(controller, TasksMessages.msgTaskSelectClick(it)) },
            { uiScope.sendMessage(controller, TasksMessages.msgTaskClicked(it)) }
        ),
        TasksAdapter.blankAdapter(),
        TasksAdapter.searchAdapter {
            uiScope.sendMessage(controller, TasksMessages.msgSearch(it))
        }
    )

    override fun onResume() {
        super.onResume()
        YandexMapFragment.savedCameraPosition = null //TODO: Remove after yandex map refactor
        if (shouldShowUpdateRequiredOnResume) {
            showUpdateRequiredOnVisible(canSkipUpdate)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val refreshTasks = arguments?.getBoolean(ARG_REFRESH_TASKS, false) ?: false
        controller.start(TasksMessages.msgInit(refreshTasks))
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DeliveryTheme {
                    TasksScreen(controller) {
                        (activity as? HostActivity)?.changeNavigationDrawerState()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        val binding = FragmentTasksBinding.bind(view)
//
//        val layoutManager =
//            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
//        binding.rvList.layoutManager = layoutManager
//        binding.rvList.adapter = tasksAdapter
//
//        bindControls(binding)
//
//        renderJob = uiScope.launch {
//            val renders = listOf(
//                TasksRenders.renderList(tasksAdapter),
//                TasksRenders.renderLoading(binding.loading),
//                TasksRenders.renderStartButton(binding.btnStart)
//            )
//            launch { controller.stateFlow().collect(rendersCollector(renders)) }
//            launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
//        }
        controller.context.errorContext.attach(view)
        controller.context.showSnackbar = { withContext(Dispatchers.Main) { showSnackbar(resources.getString(it)) } }
        controller.context.showUpdateRequiredOnVisible = ::showUpdateRequiredOnVisible
    }

    override fun onPause() {
        super.onPause()
        taskUpdateRequiredDialogShowed = false
    }

    private fun showUpdateRequiredOnVisible(canSkip: Boolean) {
        if (taskUpdateRequiredDialogShowed) {
            return
        }
        if (isVisible) {
            taskUpdateRequiredDialogShowed = true
            showDialog(
                R.string.task_update_required,
                R.string.ok to {
                    uiScope.sendMessage(controller, TasksMessages.msgRefresh())
                    shouldShowUpdateRequiredOnResume = false
                    canSkipUpdate = false
                    taskUpdateRequiredDialogShowed = false
                },
                (R.string.later to {
                    shouldShowUpdateRequiredOnResume = true
                    canSkipUpdate = canSkip
                    taskUpdateRequiredDialogShowed = false
                }).takeIf { canSkip }
            )
        }
    }

//    private fun bindControls(binding: FragmentTasksBinding) {
//        binding.ivMenu.setOnClickListener {
//            (activity as? HostActivity)?.changeNavigationDrawerState()
//        }
//        binding.btnStart.setOnClickListener {
//            uiScope.sendMessage(controller, TasksMessages.msgStartClicked())
//        }
//        binding.ivUpdate.setOnClickListener {
//            uiScope.sendMessage(controller, TasksMessages.msgRefresh())
//        }
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderJob?.cancel()
        controller.context.errorContext.detach()
        controller.context.showSnackbar = {}
    }

    override fun onExamined(task: Task) {
        uiScope.sendMessage(controller, TasksMessages.msgTaskExamined(task))
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