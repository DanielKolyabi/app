package ru.relabs.kurjer.ui.fragments


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_report.*
import kotlinx.android.synthetic.main.include_hint_container.*
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.ui.delegates.ReportBlankPhotoDelegate
import ru.relabs.kurjer.ui.delegates.ReportEntrancesDelegate
import ru.relabs.kurjer.ui.delegates.ReportPhotoDelegate
import ru.relabs.kurjer.ui.delegates.ReportTasksDelegate
import ru.relabs.kurjer.ui.helpers.HintHelper
import ru.relabs.kurjer.ui.helpers.setVisible
import ru.relabs.kurjer.ui.models.ReportEntrancesListModel
import ru.relabs.kurjer.ui.models.ReportPhotosListModel
import ru.relabs.kurjer.ui.models.ReportTasksListModel
import ru.relabs.kurjer.ui.presenters.ReportPresenter
import java.util.*

class ReportFragment : Fragment() {
    lateinit var tasks: MutableList<TaskModel>
    lateinit var taskItems: MutableList<TaskItemModel>
    private var selectedTaskItemId: Int = 0
    private lateinit var hintHelper: HintHelper

    val tasksListAdapter = DelegateAdapter<ReportTasksListModel>()
    val entrancesListAdapter = DelegateAdapter<ReportEntrancesListModel>()
    val photosListAdapter = DelegateAdapter<ReportPhotosListModel>()

    private val presenter = ReportPresenter(this)

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            val taskItemId = intent.getIntExtra("task_item_closed", 0)
            if (taskItemId != 0) {
               for(taskItem in taskItems){
                   if(taskItem.id == taskItemId){
                       taskItem.state = TaskItemModel.CLOSED
                       presenter.changeCurrentTask(presenter.currentTask)
                       break
                   }
               }
            }
        }
    }
    private val intentFilter = IntentFilter("NOW")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tasks = it.getParcelableArrayList("tasks")
            taskItems = it.getParcelableArrayList("task_items")
            selectedTaskItemId = it.getInt("selected_task_id")
        }
        activity?.registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        activity?.unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_report, container, false)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        presenter.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hintHelper = HintHelper(hint_container, "", false, activity!!.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE))

        tasksListAdapter.addDelegate(ReportTasksDelegate {
            presenter.changeCurrentTask(it)
        })
        entrancesListAdapter.addDelegate(
                ReportEntrancesDelegate({ type, holder ->
                    presenter.onEntranceSelected(type, holder)
                }, { adapterPosition ->
                    presenter.onCouplingChanged(adapterPosition)
                })
        )
        photosListAdapter.addDelegate(ReportPhotoDelegate { holder ->
            presenter.onRemovePhotoClicked(holder)
        })
        photosListAdapter.addDelegate(ReportBlankPhotoDelegate { holder ->
            presenter.onBlankPhotoClicked()
        })

        val listClickInterceptor = object : RecyclerView.OnItemTouchListener {
            override fun onTouchEvent(rv: RecyclerView?, e: MotionEvent?) {}

            override fun onInterceptTouchEvent(rv: RecyclerView?, e: MotionEvent?): Boolean =
                    taskItems[presenter.currentTask].state == TaskItemModel.CLOSED || !tasks[presenter.currentTask].isAvailableByDate(Date())

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        }

        close_button.setOnClickListener {
            presenter.onCloseClicked()
        }

        user_explanation_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter.onDescriptionChanged()
            }
        })

        tasks_list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        entrances_list.layoutManager = LinearLayoutManager(context)
        photos_list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        tasks_list.adapter = tasksListAdapter
        entrances_list.adapter = entrancesListAdapter
        entrances_list.addOnItemTouchListener(listClickInterceptor)
        photos_list.adapter = photosListAdapter
        photos_list.addOnItemTouchListener(listClickInterceptor)

        presenter.fillTasksAdapterData()
        var currentTask = 0
        tasks.forEachIndexed { index, taskModel ->
            if (taskModel.id == selectedTaskItemId) {
                currentTask = index
                return@forEachIndexed
            }
        }
        presenter.changeCurrentTask(currentTask)
    }

    fun showHintText(notes: List<String>) {
        hint_text.text = Html.fromHtml((1..3).map {
            "<b><h3>Пр. $it</h3></b>\n" + notes.getOrElse(it - 1) { "" }
        }.joinToString("\n"))
    }

    fun setTaskListVisible(visible: Boolean) {
        tasks_list.setVisible(visible)
    }

    fun setTaskListActiveTask(taskNumber: Int, isActive: Boolean) {
        if(tasksListAdapter.data.size <= taskNumber) return
        (tasksListAdapter.data[taskNumber] as? ReportTasksListModel.TaskButton)?.active = isActive
        tasksListAdapter.notifyItemChanged(taskNumber)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!presenter.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(task: List<TaskModel>, taskItem: List<TaskItemModel>, selectedTaskId: Int) =
                ReportFragment().apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList("tasks", ArrayList(task))
                        putParcelableArrayList("task_items", ArrayList(taskItem))
                        putInt("selected_task_id", selectedTaskId)
                    }
                }
    }
}
