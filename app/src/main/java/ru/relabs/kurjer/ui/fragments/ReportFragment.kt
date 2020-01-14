package ru.relabs.kurjer.ui.fragments


import android.app.AlertDialog
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
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_report.*
import kotlinx.android.synthetic.main.include_hint_container.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.MyApplication
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.repository.PauseType
import ru.relabs.kurjer.ui.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.ui.delegates.*
import ru.relabs.kurjer.ui.helpers.HintHelper
import ru.relabs.kurjer.ui.helpers.setVisible
import ru.relabs.kurjer.ui.models.ReportEntrancesListModel
import ru.relabs.kurjer.ui.models.ReportPhotosListModel
import ru.relabs.kurjer.ui.models.ReportTasksListModel
import ru.relabs.kurjer.ui.presenters.ReportPresenter
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.CustomLog.getStacktraceAsString
import java.io.FileNotFoundException
import java.util.*


class ReportFragment : Fragment(), MainActivity.IBackPressedInterceptor {

    lateinit var tasks: MutableList<TaskModel>
    lateinit var taskItems: MutableList<TaskItemModel>
    private var selectedTaskItemId: Int = 0
    private lateinit var hintHelper: HintHelper
    private var gpsLoaderJob: Job? = null

    val tasksListAdapter = DelegateAdapter<ReportTasksListModel>()
    val entrancesListAdapter = DelegateAdapter<ReportEntrancesListModel>()
    val photosListAdapter = DelegateAdapter<ReportPhotosListModel>()

    private val presenter = ReportPresenter(
            this,
            MyApplication.instance.radiusRepository,
            MyApplication.instance.locationProvider
    )

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            val taskItemId = intent.getIntExtra("task_item_closed", 0)
            if (taskItemId != 0) {
                for (taskItem in taskItems) {
                    if (taskItem.id == taskItemId) {
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        presenter.photoUUID?.let {
            outState.putString("photoUUID", it.toString())
        }
        outState.putInt("selected_task_id", selectedTaskItemId)
    }

    override fun onDestroy() {
        activity?.unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        savedInstanceState?.getString("photoUUID")?.let {
            presenter.photoUUID = UUID.fromString(it)
        }
        savedInstanceState?.getInt("selected_task_id")?.let {
            selectedTaskItemId = it
        }
        return inflater.inflate(R.layout.fragment_report, container, false)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        presenter.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun updateHintHelperMaximumHeight() {
//        hint_container.measure(View.MeasureSpec.makeMeasureSpec(hint_container.width, View.MeasureSpec.EXACTLY),
//                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
//        entrances_list.measure(View.MeasureSpec.makeMeasureSpec(entrances_list.width, View.MeasureSpec.EXACTLY),
//                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))

        hintHelper?.maxHeight = (entrances_list?.height ?: 0) + (hint_container?.height ?: 0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hintHelper = HintHelper(
                hint_container,
                "",
                false,
                activity!!.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        )

        hint_container.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                hint_container?.viewTreeObserver?.removeOnGlobalLayoutListener(this)

                updateHintHelperMaximumHeight()
            }
        });

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
        photosListAdapter.addDelegate(ReportBlankMultiPhotoDelegate { holder ->
            presenter.onBlankMultiPhotoClicked()
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

        updatePauseButtonEnabled()
        pause_button.setOnClickListener {
            showPauseDialog()
        }

        presenter.fillTasksAdapterData()
        var currentTask = 0
        tasks.forEachIndexed { index, taskModel ->
            if (taskModel.id == selectedTaskItemId) {
                currentTask = index
                return@forEachIndexed
            }
        }
        presenter.changeCurrentTask(currentTask)
        hideGPSLoader()
    }

    fun updatePauseButtonEnabled() {
        pause_button?.isEnabled = MyApplication.instance.pauseRepository.isAnyPauseAvailable()
    }

    private fun showPauseDialog() {
        var dialog: AlertDialog? = null

        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.select_dialog_singlechoice).apply {
            if (MyApplication.instance.pauseRepository.isPauseAvailable(PauseType.Lunch)) {
                add("Обед")
            }
            if (MyApplication.instance.pauseRepository.isPauseAvailable(PauseType.Load)) {
                add("Дозагрузка")
            }
            add("Отмена")
        }

        dialog = AlertDialog.Builder(requireContext())
                .setAdapter(adapter) { _, id ->
                    val text = adapter.getItem(id)
                    when (text) {
                        "Обед" -> presenter.startPause(PauseType.Lunch)
                        "Дозагрузка" -> presenter.startPause(PauseType.Load)
                        "Отмена" -> dialog?.dismiss()
                    }
                }.show()
    }

    fun showHintText(notes: List<String>) {
        hint_text.text = Html.fromHtml((3 downTo 1).map {
            notes.getOrElse(it - 1) { "" }
        }.joinToString("<br/>"))
    }

    fun setTaskListVisible(visible: Boolean) {
        try {
            tasks_list.setVisible(visible)
        } catch (e: Throwable) {
            e.printStackTrace()
            CustomLog.writeToFile(CustomLog.getStacktraceAsString(e))
        }
    }

    fun setTaskListActiveTask(taskNumber: Int, isActive: Boolean) {
        if (tasksListAdapter.data.size <= taskNumber) return
        (tasksListAdapter.data[taskNumber] as? ReportTasksListModel.TaskButton)?.active = isActive
        tasksListAdapter.notifyItemChanged(taskNumber)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!presenter.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun showPauseError() {
        if (!isVisible) {
            return
        }
        AlertDialog.Builder(requireContext())
                .setTitle("Ошибка")
                .setMessage("Пауза недоступна")
                .setPositiveButton("Ок") { _, _ -> }
                .show()
    }

    fun showPauseWarning() {
        AlertDialog.Builder(requireContext())
                .setTitle("Пауза")
                .setMessage("Пауза будет прервана")
                .setPositiveButton("Ок") { _, _ -> presenter.onCloseClicked(false) }
                .setNegativeButton("Отмена") { _, _ -> }
                .show()
    }

    fun showPreCloseDialog(message: String, action: (() -> Unit)? = null) {
        AlertDialog.Builder(requireContext())
                .setTitle("Ошибка")
                .setMessage(message)
                .setPositiveButton("Ок") { _, _ ->
                    action?.invoke()
                }
                .show()
    }

    fun showSendCrashReportDialog() {
        AlertDialog.Builder(requireContext())
                .setTitle("Crash Log")
                .setMessage("Отправка crash.log")
                .setPositiveButton("Отправить") { _, _ ->
                    try {
                        CustomLog.share(requireActivity())
                    } catch (e: FileNotFoundException) {
                        Toast.makeText(requireContext(), "crash.log отсутствует", Toast.LENGTH_LONG).show()
                    } catch (e: java.lang.Exception) {
                        CustomLog.writeToFile(getStacktraceAsString(e))
                        Toast.makeText(requireContext(), "Произошла ошибка", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Отмена") { _, _ -> }
                .show()
    }

    fun showGPSLoader() {
        gps_loading.visibility = View.VISIBLE
        pb_gps_loading.max = 40
        pb_gps_loading.progress = 0
        pb_gps_loading.isIndeterminate = false
        gpsLoaderJob?.cancel()
        gpsLoaderJob = launch(UI) {
            var i = 0
            while (i < 41) {
                delay(1000)
                i++
                if (!isActive) {
                    return@launch
                }
                pb_gps_loading.progress = i
            }
        }
    }

    fun hideGPSLoader() {
        gpsLoaderJob?.cancel()
        gps_loading.visibility = View.GONE
    }

    override fun interceptBackPressed(): Boolean {
        if (gps_loading?.visibility == View.VISIBLE) {
            return true
        }
        return false
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
