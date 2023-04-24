package ru.relabs.kurjer.presentation.taskDetails

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_task_details.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.presentation.base.compose.common.themes.DeliveryTheme
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.defaultController
import ru.relabs.kurjer.presentation.base.tea.sendMessage
import ru.relabs.kurjer.utils.IntentUtils
import ru.relabs.kurjer.utils.extensions.showDialog
import ru.relabs.kurjer.utils.extensions.showSnackbar


/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

class TaskDetailsFragment : BaseFragment() {

    private val controller = defaultController(TaskDetailsState(), TaskDetailsContext())
    private var renderJob: Job? = null

    private val taskDetailsAdapter = DelegateAdapter(
        TaskDetailsAdapter.pageHeaderAdapter(),
        TaskDetailsAdapter.listHeaderAdapter(),
        TaskDetailsAdapter.listItemAdapter {
            uiScope.sendMessage(controller, TaskDetailsMessages.msgInfoClicked(it))
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val task = arguments?.getParcelable<Task>(ARG_TASK)
        if (task == null) {
            showFatalError("tdf:101")
        }
        controller.start(TaskDetailsMessages.msgInit(task))
        controller.context.onExamine = { (targetFragment as? IExaminedConsumer)?.onExamined(it) }
    }

    fun showFatalError(errCode: String) {
        showDialog(
            resources.getString(R.string.fatal_error_title, errCode),
            R.string.ok to { uiScope.sendMessage(controller, TaskDetailsMessages.msgNavigateBack()) }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.context.onExamine = {}
        controller.stop()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DeliveryTheme {
                    TaskDetailsScreen(controller)
                }
            }
        }
//        return inflater.inflate(R.layout.fragment_task_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val layoutManager =
//            LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
//        view.rv_list.layoutManager = layoutManager
//        view.rv_list.adapter = taskDetailsAdapter
//
//        bindControls(view)

//        renderJob = uiScope.launch {
//            val renders = listOf(
//                TaskDetailsRenders.renderLoading(view.loading),
//                TaskDetailsRenders.renderList(taskDetailsAdapter),
//                TaskDetailsRenders.renderExamine(view.btn_examined)
//            )
//            launch { controller.stateFlow().collect(rendersCollector(renders)) }
//            launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
//        }
        controller.context.errorContext.attach(view)
        controller.context.showSnackbar = { withContext(Dispatchers.Main) { showSnackbar(resources.getString(it)) } }
        controller.context.showImagePreview = {
            withContext(Dispatchers.Main) {
                ContextCompat.startActivity(requireContext(), IntentUtils.getImageViewIntent(it, requireContext()), null)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        controller.context.showFatalError = { withContext(Dispatchers.Main) { showFatalError(it) } }
    }

    override fun onDetach() {
        super.onDetach()
        controller.context.showFatalError = {}
    }

    private fun bindControls(view: View) {
        view.iv_menu.setOnClickListener {
            uiScope.sendMessage(controller, TaskDetailsMessages.msgNavigateBack())
        }

        view.btn_examined.setOnClickListener {
            uiScope.sendMessage(controller, TaskDetailsMessages.msgExamineClicked())
        }

        view.btn_map.setOnClickListener {
            uiScope.sendMessage(controller, TaskDetailsMessages.msgOpenMap())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderJob?.cancel()
        controller.context.showSnackbar = {}
    }

    override fun interceptBackPressed(): Boolean {
        uiScope.sendMessage(controller, TaskDetailsMessages.msgNavigateBack())
        return true
    }

    companion object {
        const val ARG_TASK = "task"
        fun <T> newInstance(task: Task, parent: T) where T : Fragment, T : IExaminedConsumer = TaskDetailsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_TASK, task)
            }
            setTargetFragment(parent, 0)
        }
    }
}