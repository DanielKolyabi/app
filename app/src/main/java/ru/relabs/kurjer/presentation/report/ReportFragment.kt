package ru.relabs.kurjer.presentation.report

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_report.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemId
import ru.relabs.kurjer.presentation.base.TextChangeListener
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.debugCollector
import ru.relabs.kurjer.presentation.base.tea.defaultController
import ru.relabs.kurjer.presentation.base.tea.rendersCollector
import ru.relabs.kurjer.presentation.base.tea.sendMessage
import ru.relabs.kurjer.utils.debug
import ru.relabs.kurjer.utils.extensions.hideKeyboard
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
            { uiScope.sendMessage(controller, ReportMessages.msgPhotoClicked(it, true)) }
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
            //TODO: Show error
            return
        }

        controller.start(ReportMessages.msgInit(itemIds, selectedItemId))
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

        view.rv_tasks.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
        view.rv_tasks.adapter = tasksAdapter

        view.rv_entrances.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        view.rv_entrances.adapter = entrancesAdapter

        view.rv_photos.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
        view.rv_photos.adapter = photosAdapter

        bindControls(view)

        renderJob = uiScope.launch {
            val renders = listOf(
                ReportRenders.renderLoading(view.loading),
                ReportRenders.renderTasks(tasksAdapter, view.rv_tasks),
                ReportRenders.renderPhotos(photosAdapter),
                ReportRenders.renderEntrances(entrancesAdapter),
                ReportRenders.renderTitle(view.tv_title),
                ReportRenders.renderDescription(view.et_description, descriptionTextWatcher)
            )
            launch { controller.stateFlow().collect(rendersCollector(renders)) }
            launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
        }
        controller.context.errorContext.attach(view)
        controller.context.requestPhoto = ::requestPhoto
        controller.context.hideKeyboard = ::hideKeyboard
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
            nextPhotoData = ReportPhotoData(entrance, multiplePhoto, targetFile, uuid)
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
        if (photoData == null) {
            uiScope.sendMessage(controller, ReportMessages.msgPhotoError(3))
            return
        }
        //Find photo in target file
        if (photoData.targetFile.exists()) {
            uiScope.sendMessage(
                controller,
                ReportMessages.msgPhotoCaptured(photoData.entrance, photoData.multiplePhoto, photoData.targetFile, photoData.uuid)
            )
        }
        //Find photo in result data
        if (data != null) {
            (data.extras?.get("data") as? Bitmap)?.let {
                uiScope.sendMessage(
                    controller,
                    ReportMessages.msgPhotoCaptured(photoData.entrance, photoData.multiplePhoto, it, photoData.targetFile, photoData.uuid)
                )
                return
            }
        }
        uiScope.sendMessage(controller, ReportMessages.msgPhotoError(4))
    }

    private fun bindControls(view: View) {
        view.et_description.addTextChangedListener(descriptionTextWatcher)

        view.iv_menu.setOnClickListener {
            uiScope.sendMessage(controller, ReportMessages.msgBackClicked())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderJob?.cancel()
        controller.context.hideKeyboard = {}
        controller.context.requestPhoto = { _, _, _, _ -> }
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

    private data class ReportPhotoData(val entrance: Int, val multiplePhoto: Boolean, val targetFile: File, val uuid: UUID)
}