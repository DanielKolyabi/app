package ru.relabs.kurjer.presentation.report

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_report.*
import kotlinx.android.synthetic.main.fragment_report.view.*
import kotlinx.android.synthetic.main.include_hint_container.*
import kotlinx.android.synthetic.main.include_hint_container.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.presentation.base.TextChangeListener
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.*
import ru.relabs.kurjer.presentation.dialogs.RejectFirmDialog
import ru.relabs.kurjer.uiOld.helpers.HintHelper
import ru.relabs.kurjer.utils.debug
import ru.relabs.kurjer.utils.extensions.hideKeyboard
import ru.relabs.kurjer.utils.extensions.showDialog
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

class ReportFragment : BaseFragment() {
    private var nextPhotoData: ReportPhotoData? = null

    private val controller = defaultController(ReportState(), ReportContext())
    private var renderJob: Job? = null

    private val descriptionTextWatcher = TextChangeListener {
        uiScope.sendMessage(controller, ReportMessages.msgDescriptionChanged(it))
    }

    private val tasksAdapter = DelegateAdapter(
        ReportAdapter.task {
            uiScope.sendMessage(controller, ReportMessages.msgTaskSelected(it.id))
        }
    )

    private val photosAdapter = DelegateAdapter(
        ReportAdapter.photoSingle {
            uiScope.sendMessage(controller, ReportMessages.msgPhotoClicked(null, false))
        },
        ReportAdapter.photo {
            uiScope.sendMessage(controller, ReportMessages.msgRemovePhotoClicked(it))
        }
    )

    private val entrancesAdapter = DelegateAdapter(
        ReportAdapter.entrance(
            { entrance, button -> uiScope.sendMessage(controller, ReportMessages.msgEntranceSelectClicked(entrance, button)) },
            { uiScope.sendMessage(controller, ReportMessages.msgCoupleClicked(it)) },
            { uiScope.sendMessage(controller, ReportMessages.msgPhotoClicked(it, false)) }
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemIds = arguments?.getParcelableArrayList<ArgItem>(ARG_ITEMS_KEY)?.map {
            Pair(it.task, it.taskItem)
        }
        val selectedItemId = arguments?.getInt(ARG_SELECTED_TASK_ITEM_ID, ARG_SELECTED_TASK_ITEM_EMPTY)
            ?.takeIf { it != ARG_SELECTED_TASK_ITEM_EMPTY }
            ?.let { TaskItemId(it) }

        if (itemIds == null || selectedItemId == null) {
            showDialog(
                getString(R.string.unknown_runtime_error_code, "rf:100"),
                R.string.ok to { uiScope.sendMessage(controller, ReportMessages.msgBackClicked()) }
            )
            controller.start(msgEmpty())
        } else {
            controller.start(ReportMessages.msgInit(itemIds, selectedItemId))
        }

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
        return inflater.inflate(R.layout.fragment_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hintHelper = HintHelper(hint_container, "", false, requireActivity())
        hint_container.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                hint_container?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                hintHelper.maxHeight = (rv_entrances?.height ?: 0) + (hint_container?.height ?: 0)
            }
        })

        val listInterceptor = ListClickInterceptor()

        view.rv_tasks.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
        view.rv_tasks.adapter = tasksAdapter

        view.rv_entrances.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        view.rv_entrances.adapter = entrancesAdapter
        view.rv_entrances.addOnItemTouchListener(listInterceptor)

        view.rv_photos.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
        view.rv_photos.adapter = photosAdapter
        view.rv_photos.addOnItemTouchListener(listInterceptor)

        bindControls(view)

        renderJob = uiScope.launch {
            val renders = listOf(
                ReportRenders.renderLoading(view.loading, view.tv_gps_loading),
                ReportRenders.renderTasks(tasksAdapter, view.rv_tasks),
                ReportRenders.renderPhotos(photosAdapter),
                ReportRenders.renderEntrances(entrancesAdapter, view.rv_entrances),
                ReportRenders.renderTitle(view.tv_title),
                ReportRenders.renderDescription(view.et_description, descriptionTextWatcher),
                ReportRenders.renderNotes(view.hint_text),
                ReportRenders.renderTaskItemAvailability(listInterceptor, view.et_description, view.btn_close, view.btn_reject),
                ReportRenders.renderRejectButton(view.btn_reject)
            )
            launch { controller.stateFlow().collect(rendersCollector(renders)) }
            launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
        }
        controller.context.errorContext.attach(view)
        controller.context.requestPhoto = ::requestPhoto
        controller.context.hideKeyboard = ::hideKeyboard
        controller.context.showCloseError = ::showCloseError
        controller.context.showPausedWarning = ::showPausedWarning
        controller.context.showPhotosWarning = ::showPhotosWarning
        controller.context.showPreCloseDialog = ::showPreCloseDialog
        controller.context.getBatteryLevel = ::getBatteryLevel
        controller.context.showError = ::showFatalError
        controller.context.contentResolver = { requireContext().contentResolver }
        controller.context.showRejectDialog = ::showRejectDialog
    }

    private fun showRejectDialog(reasons: List<String>) {
        RejectFirmDialog(reasons) {
            uiScope.sendMessage(controller, ReportMessages.msgCloseClicked(it))
        }.show(requireFragmentManager(), "dialog_reject")
    }

    private suspend fun showFatalError(code: String, isFatal: Boolean) = withContext(Dispatchers.Main) {
        FirebaseCrashlytics.getInstance().log("fatal error $isFatal $code")
        showDialog(
            getString(R.string.unknown_runtime_error_code, code),
            R.string.ok to {
                if (isFatal) {
                    uiScope.sendMessage(controller, ReportMessages.msgBackClicked())
                }
            }
        )

        Unit
    }

    private fun getBatteryLevel(): Float? {
        val ifilter = IntentFilter("android.intent.action.BATTERY_CHANGED")
        val battery = context?.registerReceiver(null as BroadcastReceiver?, ifilter)
        return battery?.let { it ->
            val level = it.getIntExtra("level", -1)
            val scale = it.getIntExtra("scale", -1)
            level.toFloat() / scale.toFloat()
        }
    }

    private fun showPreCloseDialog(location: Location?, rejectReason: String?) {
        showDialog(
            R.string.report_close_ask,
            R.string.yes to { uiScope.sendMessage(controller, ReportMessages.msgPerformClose(location, rejectReason)) },
            R.string.no to {},
            style = R.style.RedAlertDialog
        )
    }

    private fun showCloseError(msgRes: Int, withPreClose: Boolean, location: Location? = null, rejectReason: String?) {
        showDialog(
            msgRes,
            R.string.ok to {
                if (withPreClose) {
                    showPreCloseDialog(location, rejectReason)
                }
            }
        )
    }

    private fun showPhotosWarning() {
        showDialog(
            R.string.report_close_no_photos,
            R.string.ok to {}
        )
    }

    private fun showPausedWarning() {
        showDialog(
            R.string.report_close_paused_warning,
            R.string.ok to { uiScope.sendMessage(controller, ReportMessages.msgInterruptPause()) },
            R.string.cancel to {}
        )
    }

    private fun requestPhoto(entrance: Int, multiplePhoto: Boolean, targetFile: File, uuid: UUID) {
        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "com.relabs.kurjer.file_provider",
            targetFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            nextPhotoData = ReportPhotoData(entrance, multiplePhoto, photoUri, targetFile, uuid)
            startActivityForResult(intent, REQUEST_PHOTO_CODE)
        } else {
            uiScope.sendMessage(controller, ReportMessages.msgPhotoError(1))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PHOTO_CODE) return
        val photoData = nextPhotoData
        nextPhotoData = null
        if (resultCode != Activity.RESULT_OK && resultCode != Activity.RESULT_CANCELED) {
            uiScope.sendMessage(controller, ReportMessages.msgPhotoError(2))
            return
        }
        if (resultCode == Activity.RESULT_CANCELED) {
            return
        }
        if (photoData == null) {
            uiScope.sendMessage(controller, ReportMessages.msgPhotoError(3))
            return
        }

        val uri = data?.data ?: photoData.photoUri
        if (requireContext().contentResolver.getType(uri) == null) {
            uiScope.sendMessage(controller, ReportMessages.msgPhotoError(4))
            return
        }

        uiScope.sendMessage(
            controller,
            ReportMessages.msgPhotoCaptured(
                photoData.entrance,
                photoData.multiplePhoto,
                photoData.photoUri,
                photoData.targetFile,
                photoData.uuid
            )
        )
    }

    private fun bindControls(view: View) {
        view.et_description.addTextChangedListener(descriptionTextWatcher)

        view.iv_menu.setOnClickListener {
            uiScope.sendMessage(controller, ReportMessages.msgBackClicked())
        }

        view.btn_close.setOnClickListener {
            uiScope.sendMessage(controller, ReportMessages.msgCloseClicked(null))
        }

        view.btn_reject.setOnClickListener {
            uiScope.sendMessage(controller, ReportMessages.msgRejectClicked())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderJob?.cancel()
        controller.context.showError = { _, _ -> }
        controller.context.hideKeyboard = {}
        controller.context.requestPhoto = { _, _, _, _ -> }
        controller.context.showCloseError = { _, _, _, _ -> }
        controller.context.showPausedWarning = {}
        controller.context.showPhotosWarning = {}
        controller.context.showPreCloseDialog = { _, _ -> }
        controller.context.getBatteryLevel = { null }
        controller.context.contentResolver = { null }
        controller.context.errorContext.detach()
    }

    override fun interceptBackPressed(): Boolean {
        return false
    }

    companion object {
        const val ARG_ITEMS_KEY = "items"
        const val ARG_SELECTED_TASK_ITEM_ID = "task_item_id"
        const val ARG_SELECTED_TASK_ITEM_EMPTY = -999
        const val REQUEST_PHOTO_CODE = 501

        fun newInstance(items: List<Pair<Task, TaskItem>>, selectedTaskItem: TaskItem) = ReportFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(ARG_ITEMS_KEY, ArrayList(items.map { ArgItem(it.first.id, it.second.id) }))
                putInt(ARG_SELECTED_TASK_ITEM_ID, selectedTaskItem.id.id)
            }
        }
    }

    @Parcelize
    private data class ArgItem(val task: TaskId, val taskItem: TaskItemId) : Parcelable

    private data class ReportPhotoData(
        val entrance: Int,
        val multiplePhoto: Boolean,
        val photoUri: Uri,
        val targetFile: File,
        val uuid: UUID
    )
}