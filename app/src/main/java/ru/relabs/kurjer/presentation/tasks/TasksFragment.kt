package ru.relabs.kurjer.presentation.tasks

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.repositories.PauseType
import ru.relabs.kurjer.presentation.base.compose.common.themes.DeliveryTheme
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.fragment.FragmentStyleable
import ru.relabs.kurjer.presentation.base.fragment.IFragmentStyleable
import ru.relabs.kurjer.presentation.base.tea.defaultController
import ru.relabs.kurjer.presentation.base.tea.sendMessage
import ru.relabs.kurjer.presentation.taskDetails.IExaminedConsumer
import ru.relabs.kurjer.uiOld.fragments.YandexMapFragment
import ru.relabs.kurjer.utils.ClipboardHelper
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.IntentUtils
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import ru.relabs.kurjer.utils.extensions.showDialog
import ru.relabs.kurjer.utils.extensions.showSnackbar
import java.io.FileNotFoundException


/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

class TasksFragment : BaseFragment(),
    IFragmentStyleable by FragmentStyleable(false),
    IExaminedConsumer {

    private val controller = defaultController(TasksState(), TasksContext(this))
    private var renderJob: Job? = null
    private var shouldShowUpdateRequiredOnResume: Boolean = false
    private var canSkipUpdate: Boolean = false
    private var taskUpdateRequiredDialogShowed: Boolean = false

    override fun onResume() {
        super.onResume()
        YandexMapFragment.savedCameraPosition = null //TODO: Remove after yandex map refactor
        if (shouldShowUpdateRequiredOnResume) {
            showUpdateRequiredOnVisible(canSkipUpdate)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val refreshTasks = arguments?.getBoolean(ARG_REFRESH_TASKS, false) ?: false
        controller.start(TasksMessages.msgInit(refreshTasks))
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DeliveryTheme {
                    TasksScreen(controller = controller) {
                        when (val r = CustomLog.share(requireActivity())) {
                            is Left -> when (val e = r.value) {
                                is FileNotFoundException -> showSnackbar(resources.getString(R.string.crash_log_not_found))
                                else -> showSnackbar(resources.getString(R.string.unknown_runtime_error))
                            }

                            is Right -> Unit
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.context.errorContext.attach(view)
        controller.context.showSnackbar = { withContext(Dispatchers.Main) { showSnackbar(resources.getString(it)) } }
        controller.context.showUpdateRequiredOnVisible = ::showUpdateRequiredOnVisible
        controller.context.showErrorDialog = ::showErrorDialog
        controller.context.showPauseDialog = ::showPauseDialog
        controller.context.copyToClipboard = ::copyToClipboard
    }

    private fun showPauseDialog(availablePauseTypes: List<PauseType>) {
        var dialog: AlertDialog? = null

        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.select_dialog_singlechoice).apply {
            availablePauseTypes.map {
                add(
                    getString(
                        when (it) {
                            PauseType.Lunch -> R.string.pause_lunch
                            PauseType.Load -> R.string.pause_load
                        }
                    )
                )
            }
            add(getString(R.string.pause_cancel))
        }

        dialog = AlertDialog.Builder(requireContext())
            .setAdapter(adapter) { _, id ->
                when (adapter.getItem(id)) {
                    getString(R.string.pause_lunch) -> uiScope.sendMessage(controller, TasksMessages.msgPauseStart(PauseType.Lunch))
                    getString(R.string.pause_load) -> uiScope.sendMessage(controller, TasksMessages.msgPauseStart(PauseType.Load))
                    getString(R.string.pause_cancel) -> dialog?.dismiss()
                }
            }.show()
    }

    override fun onPause() {
        super.onPause()
        taskUpdateRequiredDialogShowed = false
    }

    private fun showUpdateRequiredOnVisible(canSkip: Boolean) {
        if (taskUpdateRequiredDialogShowed) {
            return
        }
        if (isVisible) {
            taskUpdateRequiredDialogShowed = true
            showDialog(
                R.string.task_update_required,
                R.string.ok to {
                    uiScope.sendMessage(controller, TasksMessages.msgRefresh())
                    shouldShowUpdateRequiredOnResume = false
                    canSkipUpdate = false
                    taskUpdateRequiredDialogShowed = false
                },
                (R.string.later to {
                    shouldShowUpdateRequiredOnResume = true
                    canSkipUpdate = canSkip
                    taskUpdateRequiredDialogShowed = false
                }).takeIf { canSkip }
            )
        }
    }

    private fun showErrorDialog(stringResource: Int) {
        showDialog(
            stringResource,
            R.string.ok to {}
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderJob?.cancel()
        controller.context.errorContext.detach()
        controller.context.showSnackbar = {}
        controller.context.showErrorDialog = {}
        controller.context.showPauseDialog = {}
    }

    override fun onExamined(task: Task) {
        uiScope.sendMessage(controller, TasksMessages.msgTaskExamined(task))
    }

    override fun interceptBackPressed(): Boolean {
        return false
    }

    private fun sendDeviceUUID(text: String) {
        startActivity(
            Intent.createChooser(
                IntentUtils.getShareTextIntent(getString(R.string.share_device_uuid_subject), text),
                getString(R.string.share_device_uuid_title)
            )
        )
    }

    private fun copyToClipboard(text: String) {
        when (val r = ClipboardHelper.copyToClipboard(requireActivity(), text)) {
            is Right ->
                showSnackbar(
                    resources.getString(R.string.copied_to_clipboard),
                    resources.getString(R.string.send) to { sendDeviceUUID(text) }
                )

            is Left ->
                showSnackbar(resources.getString(R.string.unknown_runtime_error))
        }
    }

    companion object {
        const val ARG_REFRESH_TASKS = "refresh_tasks"
        fun newInstance(refreshTasks: Boolean) = TasksFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_REFRESH_TASKS, refreshTasks)
            }
        }
    }
}