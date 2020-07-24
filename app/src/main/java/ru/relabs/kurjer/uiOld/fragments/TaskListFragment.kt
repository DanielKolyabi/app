package ru.relabs.kurjer.uiOld.fragments


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import kotlinx.android.synthetic.main.fragment_task_list.*
import kotlinx.android.synthetic.main.include_hint_container.*
import org.koin.android.ext.android.get
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.R
import ru.relabs.kurjer.ReportService
import ru.relabs.kurjer.utils.activity
import ru.relabs.kurjer.uiOld.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.uiOld.delegates.TaskListLoaderDelegate
import ru.relabs.kurjer.uiOld.delegates.TaskListTaskDelegate
import ru.relabs.kurjer.uiOld.helpers.HintHelper
import ru.relabs.kurjer.uiOld.models.TaskListModel
import ru.relabs.kurjer.uiOld.presenters.TaskListPresenter


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
            if (it is TaskListModel.Task) {
                "${it.task.name} №${it.task.edition} ${it.task.area}уч".contains(item)
            } else {
                false
            }
        }
        if (itemIndex < 0) {
            return
        }
        tasks_list.smoothScrollToPosition(itemIndex)
    }

    val presenter = TaskListPresenter(this, get(), get(), get(), get())
    private lateinit var hintHelper: HintHelper
    val adapter = DelegateAdapter<TaskListModel>()
    private var shouldUpdate: Boolean = false
    private var shouldCheckTasks: Boolean = false
    private var targetListPos = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            shouldUpdate = it.getBoolean("shouldUpdate", false)
            shouldCheckTasks = it.getBoolean("shouldCheckTasks", false)
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
            showListLoading(true, true)
            presenter.loadTasks(true)
        }
        YandexMapFragment.savedCameraPosition = null

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
        if (shouldCheckTasks) {
            shouldCheckTasks = false
            context?.apply {
                startService(Intent(this, ReportService::class.java).apply { putExtra("force_check_updates", true) })
            }
        }
        presenter.updateStartButton()
    }

    fun scrollListToTarget() {
        if (targetListPos == 0) {
            return
        }
        try {
            tasks_list?.smoothScrollToPosition(targetListPos)
        } catch (e: Throwable) {
            e.printStackTrace()
            CustomLog.writeToFile(CustomLog.getStacktraceAsString(e))
        }
    }

    fun setStartButtonActive(active: Boolean) {
        start?.isEnabled = active
    }

    fun showListLoading(isLoading: Boolean, clearList: Boolean = false) {
        if (isLoading) {
            if (adapter.data.isEmpty() || adapter.data.first() !is TaskListModel.Loader) {
                if (clearList) {
                    adapter.data.clear()
                }

                adapter.data.add(0, TaskListModel.Loader)
                adapter.notifyDataSetChanged()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(shouldUpdate: Boolean, posInList: Int = 0, shouldCheckTasks: Boolean = false) =
                TaskListFragment().apply {
                    arguments = Bundle().apply {
                        putBoolean("shouldUpdate", shouldUpdate)
                        putInt("pos_in_list", posInList)
                        putBoolean("shouldCheckTasks", shouldCheckTasks)
                    }
                }
    }
}
