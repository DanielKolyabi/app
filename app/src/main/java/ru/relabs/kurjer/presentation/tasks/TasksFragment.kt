package ru.relabs.kurjer.presentation.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.relabs.kurjer.R
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.fragment.FragmentStyleable
import ru.relabs.kurjer.presentation.base.fragment.IFragmentStyleable
import ru.relabs.kurjer.presentation.base.tea.debugCollector
import ru.relabs.kurjer.presentation.base.tea.defaultController
import ru.relabs.kurjer.presentation.base.tea.rendersCollector
import ru.relabs.kurjer.utils.debug


/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

class TasksFragment : BaseFragment(),
    IFragmentStyleable by FragmentStyleable(false) {

    private val controller = defaultController(TasksState(), TasksContext())
    private var renderJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller.start(TasksMessages.msgInit())
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

        bindControls(view)

        renderJob = uiScope.launch {
            val renders = emptyList<TasksRender>()
            launch { controller.stateFlow().collect(rendersCollector(renders)) }
            launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
        }
        controller.context.errorContext.attach(view)
    }

    private fun bindControls(
        view: View
    ) {

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
        fun newInstance() = TasksFragment()
    }
}