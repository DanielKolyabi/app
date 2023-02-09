package ru.relabs.kurjer.presentation.storageReport

import android.app.Activity
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.text.Html
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.include_hint_container.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.R
import ru.relabs.kurjer.databinding.FragmentStorageReportBinding
import ru.relabs.kurjer.domain.models.TaskId
import ru.relabs.kurjer.domain.models.storage.StorageReportId
import ru.relabs.kurjer.presentation.base.TextChangeListener
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.recycler.DelegateAdapter
import ru.relabs.kurjer.presentation.base.tea.*
import ru.relabs.kurjer.presentation.report.ListClickInterceptor
import ru.relabs.kurjer.presentation.report.ReportFragment
import ru.relabs.kurjer.uiOld.helpers.HintHelper
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.debug
import ru.relabs.kurjer.utils.extensions.showDialog
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class StorageReportFragment : BaseFragment() {
    private var newPhotoData: StoragePhotoData? = null

    private val controller = defaultController(StorageReportState(), StorageReportContext())
    private var renderJob: Job? = null
    private lateinit var binding: FragmentStorageReportBinding
    private val closesAdapter = DelegateAdapter(
        StorageReportAdapter.closure()
    )

    private val photosAdapter = DelegateAdapter(
        StorageReportAdapter.photoSingle {
            uiScope.sendMessage(controller, StorageReportMessages.msgPhotoClicked())
        },
        StorageReportAdapter.photo {
            uiScope.sendMessage(controller, StorageReportMessages.msgRemovePhotoClicked(it))
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskIds =
            arguments?.getParcelableArrayList<TaskId>(ARG_TASK_IDS)?.toList()

        if (taskIds != null) {
            controller.start(StorageReportMessages.msgInit(taskIds))
        } else {
            controller.start(msgEmpty())
            showDialog(
                getString(R.string.unknown_runtime_error_code, "srf:100"),
                R.string.ok to {
                    uiScope.sendMessage(
                        controller,
                        StorageReportMessages.msgNavigateBack()
                    )
                }
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStorageReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newPhotoData = savedInstanceState?.getParcelable<StoragePhotoData>(
            PHOTO_DATA_KEY
        )?.also {
            savedInstanceState.remove(PHOTO_DATA_KEY)

            CustomLog.writeToFile("Request Photo: Photo Data Restored ${it}")
        }

        val hintHelper = HintHelper(hint_container, "", true, requireActivity())

        val listInterceptor = ListClickInterceptor()
        binding.rvCloses.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvCloses.adapter = closesAdapter

        binding.rvPhotos.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPhotos.adapter = photosAdapter
        binding.rvPhotos.addOnItemTouchListener(listInterceptor)

        val descriptionTextWatcher = TextChangeListener {
            if (binding.etDescription.hasFocus())
                uiScope.sendMessage(controller, StorageReportMessages.msgDescriptionChanged(it))
        }
        bindControls(binding, descriptionTextWatcher)

        renderJob = uiScope.launch {
            val renders = listOf(
                StorageReportRenders.renderLoading(binding.loading, binding.tvGpsLoading),
                StorageReportRenders.renderHint(hintHelper),
                StorageReportRenders.renderTitle(binding.tvTitle),
                StorageReportRenders.renderClosesList(closesAdapter),
                StorageReportRenders.renderPhotos(photosAdapter),
                StorageReportRenders.renderDescription(binding.etDescription,descriptionTextWatcher)
            )
            launch { controller.stateFlow().collect(rendersCollector(renders)) }
            launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
        }
        controller.context.errorContext.attach(view)
        controller.context.requestPhoto = ::requestPhoto
        controller.context.showCloseError = ::showCloseError
        controller.context.showError = ::showFatalError
        controller.context.contentResolver = { requireContext().contentResolver }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderJob?.cancel()
        controller.context.errorContext.detach()
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
    }

    private fun bindControls(binding: FragmentStorageReportBinding, descriptionTextWatcher: TextWatcher) {
        binding.ivMenu.setOnClickListener {
            uiScope.sendMessage(
                controller,
                StorageReportMessages.msgNavigateBack()
            )
        }
        binding.etDescription.addTextChangedListener(descriptionTextWatcher)
        binding.btnShowMap.setOnClickListener { uiScope.sendMessage(controller, StorageReportMessages.msgMapClicked()) }
        binding.btnClose.setOnClickListener { }
    }

    private suspend fun showFatalError(code: String, isFatal: Boolean) =
        withContext(Dispatchers.Main) {
            FirebaseCrashlytics.getInstance().log("fatal error $isFatal $code")
            showDialog(
                getString(R.string.unknown_runtime_error_code, code),
                R.string.ok to {
                    if (isFatal) {
                        uiScope.sendMessage(controller, StorageReportMessages.msgNavigateBack())
                    }
                }
            ).setOnDismissListener {
            }
            Unit
        }

    private fun showPreCloseDialog(location: Location?, rejectReason: String?) {
        showDialog(
            R.string.report_close_ask,
//            R.string.yes to {
//                uiScope.sendMessage(
//                    controller,
//                   StorageReportMessages.msgPerformClose(location, rejectReason)
//                )
//            },
            R.string.no to {},
            style = R.style.RedAlertDialog
        ).setOnDismissListener {

        }
    }

    private fun showCloseError(
        msgRes: Int,
        withPreClose: Boolean,
        location: Location? = null,
        rejectReason: String?,
        vararg msgFormat: Any
    ) {
        val text = Html.fromHtml(resources.getString(msgRes, *msgFormat))
        showDialog(
            text,
            R.string.ok to {
                if (withPreClose) {
                    showPreCloseDialog(location, rejectReason)
                }
            }
        ).setOnDismissListener {

        }
    }

    private fun requestPhoto(storageReportId: StorageReportId, targetFile: File, uuid: UUID) {
        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "com.relabs.kurjer.file_provider",
            targetFile
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            newPhotoData =
                StoragePhotoData(storageReportId, photoUri, targetFile, uuid)
            CustomLog.writeToFile("Request Photo: Store photo data: ${newPhotoData}")
            startActivityForResult(intent, REQUEST_PHOTO_CODE)
        } else {
            uiScope.sendMessage(controller, StorageReportMessages.msgPhotoError(1))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != ReportFragment.REQUEST_PHOTO_CODE) return
        val photoData = newPhotoData
        newPhotoData = null
        if (resultCode != Activity.RESULT_OK && resultCode != Activity.RESULT_CANCELED) {
            uiScope.sendMessage(controller, StorageReportMessages.msgPhotoError(2))
            return
        }
        if (resultCode == Activity.RESULT_CANCELED) {
            return
        }
        if (photoData == null) {
            CustomLog.writeToFile("Request Photo: Photo data is null")
            uiScope.sendMessage(controller, StorageReportMessages.msgPhotoError(3))
            return
        }

        val uri = data?.data ?: photoData.photoUri
        if (requireContext().contentResolver.getType(uri) == null) {
            uiScope.sendMessage(controller, StorageReportMessages.msgPhotoError(4))
            return
        }

        uiScope.sendMessage(
            controller,
            StorageReportMessages.msgPhotoCaptured(
                photoData.storageReportId,
                photoData.photoUri,
                photoData.targetFile,
                photoData.uuid
            )
        )
    }

    companion object {
        private const val PHOTO_DATA_KEY = "photo_data"
        private const val ARG_TASK_IDS = "task_ids"
        private const val REQUEST_PHOTO_CODE = 501

        fun newInstance(taskId: List<TaskId>) = StorageReportFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(ARG_TASK_IDS, ArrayList(taskId))
            }
        }
    }

    @Parcelize
    private data class StoragePhotoData(
        val storageReportId: StorageReportId,
        val photoUri: Uri,
        val targetFile: File,
        val uuid: UUID,
    ) : Parcelable
}