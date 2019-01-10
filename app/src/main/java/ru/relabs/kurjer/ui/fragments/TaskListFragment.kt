package ru.relabs.kurjer.ui.fragments


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import kotlinx.android.synthetic.main.fragment_task_list.*
import kotlinx.android.synthetic.main.include_hint_container.*
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.CustomLog
import ru.relabs.kurjer.R
import ru.relabs.kurjer.activity
import ru.relabs.kurjer.ui.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.ui.delegates.TaskListLoaderDelegate
import ru.relabs.kurjer.ui.delegates.TaskListTaskDelegate
import ru.relabs.kurjer.ui.helpers.HintHelper
import ru.relabs.kurjer.ui.models.TaskListModel
import ru.relabs.kurjer.ui.presenters.TaskListPresenter


class TaskListFragment : Fragment(), SearchableFragment {
    override fun onSearchItems(filter: String): List<String> {
        return adapter.data.asSequence()
                .filter {
                    it is TaskListModel.Task
                }
                .filter {
                    (it as TaskListModel.Task).task.area.toString().contains(filter)
                }
                .map {
                    "${(it as TaskListModel.Task).task.name} №${it.task.edition} ${it.task.area}уч"
                }
                .toList()
    }

    override fun onItemSelected(item: String, searchView: AutoCompleteTextView) {
        val itemIndex = adapter.data.indexOfFirst {
            if(it is TaskListModel.Task) {
                "${it.task.name} №${it.task.edition} ${it.task.area}уч".contains(item)
            }else{
                false
            }
        }
        if (itemIndex < 0) {
            return
        }
        tasks_list.smoothScrollToPosition(itemIndex)
    }

    val presenter = TaskListPresenter(this)
    private lateinit var hintHelper: HintHelper
    val adapter = DelegateAdapter<TaskListModel>()
    private var shouldUpdate: Boolean = false
    private var targetListPos = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            shouldUpdate = it.getBoolean("shouldUpdate", false)
            targetListPos = it.getInt("pos_in_list", 0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_task_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hintHelper = HintHelper(hint_container, this.resources.getString(R.string.task_list_hint_text), false, activity!!.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE))
        start.setOnClickListener {
            presenter.onStartClicked()
        }
        activity()?.findViewById<View>(R.id.refresh_button)?.setOnClickListener {
            showListLoading(true)
            presenter.loadTasks(true)
        }

        adapter.addDelegate(TaskListTaskDelegate(
                { presenter.onTaskSelected(it) },
                { presenter.onTaskClicked(it) }
        ))
        adapter.addDelegate(TaskListLoaderDelegate())


        tasks_list.layoutManager = LinearLayoutManager(context)
        tasks_list.adapter = adapter

        if (adapter.data.isEmpty()) {
            val shouldLoadFromNetwork = shouldUpdate // || (adapter.data.size == 0 || (adapter.data.size == 1 && adapter.data.first() == TaskListModel.Loader))

            adapter.data.clear()

            showListLoading(true)
            presenter.loadTasks(shouldLoadFromNetwork)
        }
        presenter.updateStartButton()
    }

    fun scrollListToTarget() {
        if (targetListPos == 0) {
            return
        }
        try {
            tasks_list.smoothScrollToPosition(targetListPos)
        }catch (e: Throwable){
            e.printStackTrace()
            CustomLog.writeToFile(CustomLog.getStacktraceAsString(e))
        }
    }

    fun setStartButtonActive(active: Boolean) {
        start?.isEnabled = active
    }

    fun showListLoading(isLoading: Boolean) {
        if (isLoading) {
            if (adapter.data.isEmpty() || adapter.data.first() !is TaskListModel.Loader) {
                adapter.data.add(0, TaskListModel.Loader)
                adapter.notifyDataSetChanged()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(shouldUpdate: Boolean, posInList: Int = 0) =
                TaskListFragment().apply {
                    arguments = Bundle().apply {
                        putBoolean("shouldUpdate", shouldUpdate)
                        putInt("pos_in_list", posInList)
                    }
                }
    }
}
