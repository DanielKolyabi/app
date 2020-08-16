package ru.relabs.kurjer.uiOld.fragments


import android.app.AlertDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_report_old.*
import kotlinx.android.synthetic.main.include_hint_container.*
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.R
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.database.entities.TaskItemEntity
import ru.relabs.kurjer.domain.providers.LocationProvider
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.PauseRepository
import ru.relabs.kurjer.domain.repositories.PauseType
import ru.relabs.kurjer.domain.repositories.RadiusRepository
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.uiOld.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.uiOld.delegates.*
import ru.relabs.kurjer.uiOld.dialogs.GPSRequestTimeDialog
import ru.relabs.kurjer.uiOld.helpers.HintHelper
import ru.relabs.kurjer.uiOld.helpers.setVisible
import ru.relabs.kurjer.uiOld.models.ReportEntrancesListModel
import ru.relabs.kurjer.uiOld.models.ReportPhotosListModel
import ru.relabs.kurjer.uiOld.models.ReportTasksListModel
import ru.relabs.kurjer.uiOld.presenters.ReportPresenter
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
    private var loadingDialog: Dialog? = null
    private val radiusRepository: RadiusRepository by inject()
    private val pauseRepository: PauseRepository by inject()
    private val locationProvider: LocationProvider by inject()
    private val authTokenStorage: AuthTokenStorage by inject()
    private val database: AppDatabase by inject()
    private val databaseRepository: DatabaseRepository by inject()

    val tasksListAdapter = DelegateAdapter<ReportTasksListModel>()
    val entrancesListAdapter = DelegateAdapter<ReportEntrancesListModel>()
    val photosListAdapter = DelegateAdapter<ReportPhotosListModel>()

    private val presenter = ReportPresenter(
        this,
        radiusRepository,
        locationProvider,
        database,
        pauseRepository,
        authTokenStorage,
        databaseRepository
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tasks = it.getParcelableArrayList<TaskModel>("tasks")?.toMutableList() ?: mutableListOf()
            taskItems = it.getParcelableArrayList<TaskItemModel>("task_items")?.toMutableList() ?: mutableListOf()
            selectedTaskItemId = it.getInt("selected_task_id")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        presenter.photoUUID?.let {
            outState.putString("photoUUID", it.toString())
        }
        outState.putInt("selected_task_id", selectedTaskItemId)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        savedInstanceState?.getString("photoUUID")?.let {
            presenter.photoUUID = UUID.fromString(it)
        }
        savedInstanceState?.getInt("selected_task_id")?.let {
            selectedTaskItemId = it
        }
        return inflater.inflate(R.layout.fragment_report_old, container, false)
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

        hintHelper = HintHelper(hint_container, "", false, requireActivity())

        hint_container.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            override fun onGlobalLayout() {
                hint_container?.viewTreeObserver?.removeOnGlobalLayoutListener(this)

                updateHintHelperMaximumHeight()
            }
        })

        tasksListAdapter.addDelegate(ReportTasksDelegate {
            presenter.changeCurrentTask(it)
        })
        entrancesListAdapter.addDelegate(
            ReportEntrancesDelegate(
                { type, holder ->
                    presenter.onEntranceSelected(type, holder)
                },
                { adapterPosition ->
                    presenter.onCouplingChanged(adapterPosition)
                },
                { entranceNumber ->
                    presenter.requestPhoto(true, entranceNumber)
                }
            )
        )
        photosListAdapter.addDelegate(ReportPhotoDelegate { holder ->
            presenter.onRemovePhotoClicked(holder)
        })
        photosListAdapter.addDelegate(ReportBlankPhotoDelegate { holder ->
            presenter.requestPhoto(false, -1)
        })
        photosListAdapter.addDelegate(ReportBlankMultiPhotoDelegate { holder ->
            presenter.requestPhoto(true, -1)
        })

        val listClickInterceptor = object : RecyclerView.OnItemTouchListener {
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean =
                taskItems[presenter.currentTask].state == TaskItemEntity.STATE_CLOSED || !tasks[presenter.currentTask].isAvailableByDate(Date())

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
        pause_button?.isEnabled = !pauseRepository.isPaused
    }

    private fun showPauseDialog() {
        var dialog: AlertDialog? = null

        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.select_dialog_singlechoice).apply {
            if (pauseRepository.isPauseAvailable(PauseType.Lunch)) {
                add("Обед")
            }
            if (pauseRepository.isPauseAvailable(PauseType.Load)) {
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
            .show()
    }

    fun showGPSLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = GPSRequestTimeDialog(requireContext()).apply { show() }
    }

    fun hideGPSLoadingDialog() {
        loadingDialog?.dismiss()
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
        gpsLoaderJob = GlobalScope.launch(Dispatchers.Main) {
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
