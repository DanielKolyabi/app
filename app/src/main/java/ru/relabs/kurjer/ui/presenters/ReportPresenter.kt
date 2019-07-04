package ru.relabs.kurjer.ui.presenters

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.fragment_report.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.*
import ru.relabs.kurjer.files.ImageUtils
import ru.relabs.kurjer.files.PathHelper.getTaskItemPhotoFile
import ru.relabs.kurjer.models.GPSCoordinatesModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.entities.*
import ru.relabs.kurjer.ui.fragments.ReportFragment
import ru.relabs.kurjer.ui.models.ReportEntrancesListModel
import ru.relabs.kurjer.ui.models.ReportPhotosListModel
import ru.relabs.kurjer.ui.models.ReportTasksListModel
import java.io.File
import java.util.*
import kotlin.math.roundToInt


const val REQUEST_PHOTO = 1

class ReportPresenter(private val fragment: ReportFragment) {
    var photoUUID: UUID? = null
    var photoMultiMode: Boolean = false
    var currentTask = 0

    fun changeCurrentTask(taskNumber: Int) {
        fragment.setTaskListActiveTask(currentTask, false)
        fragment.setTaskListActiveTask(taskNumber, true)
        currentTask = taskNumber

        fragment.showHintText(fragment.taskItems[currentTask].notes)

        val db = (fragment.activity?.application as? MyApplication)?.database
        db?.let {
            launch {
                fillEntrancesAdapterData(db)
                fillPhotosAdapterData(db)
                fillDescriptionData(db)
                withContext(UI) {
                    try {
                        fragment.loading.visibility = View.GONE
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        CustomLog.writeToFile(CustomLog.getStacktraceAsString(e))
                    }
                }
            }
        }

        fragment.close_button?.isEnabled = fragment.tasks[currentTask].isAvailableByDate(Date())

        fragment.close_button?.isEnabled = fragment.close_button.isEnabled && fragment.taskItems[currentTask].state != TaskItemModel.CLOSED
        fragment.user_explanation_input?.isEnabled = fragment.taskItems[currentTask].state != TaskItemModel.CLOSED

        (fragment.context as? MainActivity)?.changeTitle(fragment.taskItems[currentTask].address.name)
    }

    private fun fillDescriptionData(db: AppDatabase) {
        db.taskItemResultsDao().getByTaskItemId(fragment.taskItems[currentTask].id)?.let {
            launch(UI) {
                fragment?.user_explanation_input?.setText(it.description)
            }
        }
    }

    fun fillTasksAdapterData() {
        fragment.setTaskListVisible(true)

        fragment.tasksListAdapter.data.clear()
        fragment.tasksListAdapter.data.addAll(fragment.tasks.mapIndexed { i, it ->
            ReportTasksListModel.TaskButton(it, i, i == currentTask)
        })
        fragment.tasksListAdapter.notifyDataSetChanged()
    }

    private fun getTaskItemEntranceData(taskItem: TaskItemModel, task: TaskModel, db: AppDatabase): List<ReportEntrancesListModel.Entrance> {
        val tasksWithSameCouple = fragment.tasks.filter { it.coupleType == task.coupleType }
        val entrances = taskItem.entrances.map {
            it.coupleEnabled = it.coupleEnabled
                    && tasksWithSameCouple.size > 1
                    && taskItem.state == TaskItemModel.CREATED

            ReportEntrancesListModel.Entrance(taskItem, it.num, 0, it.coupleEnabled)
        }

        val savedResult = db.taskItemResultsDao().getByTaskItemId(taskItem.id)
        if (savedResult != null) {
            val savedEntrances = db.entrancesDao().getByTaskItemResultId(savedResult.id)

            entrances.forEach { ent ->
                val saved = savedEntrances.firstOrNull { it.entrance == ent.entranceNumber }
                if (saved != null) {
                    ent.selected = saved.state
                }
            }
        }

        return entrances
    }

    private fun fillEntrancesAdapterData(db: AppDatabase) {

        val entrances = getTaskItemEntranceData(fragment.taskItems[currentTask], fragment.tasks[currentTask], db)

        launch(UI) {
            fragment.entrancesListAdapter.data.clear()
            fragment.entrancesListAdapter.data.addAll(entrances)
            fragment.entrancesListAdapter.notifyDataSetChanged()
        }
    }

    fun fillPhotosAdapterData(db: AppDatabase) {
        fragment.photosListAdapter.data.clear()
        val taskPhotos = db.photosDao().getByTaskItemId(fragment.taskItems[currentTask].id).map {
            it.toTaskItemPhotoModel(db)
        }.filterNotNull()

        launch(UI) {
            fragment.photosListAdapter.data.add(ReportPhotosListModel.BlankPhoto)
            fragment.photosListAdapter.data.add(ReportPhotosListModel.BlankMultiPhoto)
            taskPhotos.forEach {
                fragment.photosListAdapter.data.add(
                        ReportPhotosListModel.TaskItemPhoto(it, it.getPhotoURI())
                )
            }
            fragment.photosListAdapter.notifyDataSetChanged()
        }
    }

    fun onEntranceSelected(type: Int, holder: RecyclerView.ViewHolder) {
        if (holder.adapterPosition < 0) {
            return
        }
        val data = (fragment.entrancesListAdapter.data[holder.adapterPosition] as ReportEntrancesListModel.Entrance)
        data.selected = data.selected xor type
        val entrances = fragment.entrancesListAdapter.data
                .filter { it is ReportEntrancesListModel.Entrance }
                .map { it as ReportEntrancesListModel.Entrance }

        val isEuroEnabled = data.selected and 0x0001 != 0
        val hasLookout = data.selected and 0x0010 != 0
        val isStacked = data.selected and 0x0100 != 0

        if (!isStacked) {
            if ((type == 0x0001 && isEuroEnabled) || (type == 0x0010 && hasLookout)) {
                data.selected = data.selected xor 0x0100
            }
        } else {
            if (!isEuroEnabled && !hasLookout && (type == 0x0001 || type == 0x0010)) {
                data.selected = data.selected xor 0x0100
            }
        }

        launch(CommonPool) {
            val db = MyApplication.instance.database

            if (data.coupleEnabled) {
                for ((idx, taskItem) in fragment.taskItems.withIndex()) {
                    if (taskItem == fragment.taskItems[currentTask] || taskItem.state == TaskItemModel.CLOSED) {
                        continue
                    }
                    if (fragment.tasks[idx].coupleType != fragment.tasks[currentTask].coupleType) {
                        continue
                    }

                    val taskItemEntrances = getTaskItemEntranceData(taskItem, fragment.tasks[idx], db)
                    if (taskItemEntrances.size > data.entranceNumber - 1) {
                        taskItemEntrances[data.entranceNumber - 1].selected = data.selected
                        try {
                            createOrUpdateTaskResult(taskItem.id, entrances = taskItemEntrances)
                        } catch (e: Throwable) {
                            CustomLog.writeToFile(CustomLog.getStacktraceAsString(e))
                        }
                    }
                }
            }
            try {
                createOrUpdateTaskResult(fragment.taskItems[currentTask].id, entrances = entrances)
            } catch (e: Throwable) {
                CustomLog.writeToFile(CustomLog.getStacktraceAsString(e))
            }
            withContext(UI) { fragment.entrancesListAdapter.notifyItemChanged(holder.adapterPosition) }
        }
    }

    fun onDescriptionChanged() {
        fragment.user_explanation_input ?: return
        val description = fragment.user_explanation_input.text.toString()

        launch(CommonPool) {
            try {
                createOrUpdateTaskResult(fragment.taskItems[currentTask].id, description = description)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun createOrUpdateTaskResult(
            taskItemId: Int,
            gps: GPSCoordinatesModel? = null,
            description: String? = null,
            entrances: List<ReportEntrancesListModel.Entrance>? = null
    ) {
        val db = application().database
        val taskItemEntity = db.taskItemDao().getById(taskItemId)
        val taskItemResult = db.taskItemResultsDao().getByTaskItemId(taskItemEntity!!.id)
        if (taskItemResult == null) {
            createTaskItemResult(taskItemId, db)
        }

        gps?.let {
            updateTaskItemGPSResult(taskItemId, db, it)
        }
        description?.let {
            updateTaskItemDescriptionResult(taskItemId, db, description)
        }
        entrances?.let {
            updateTaskItemEntrancesResult(taskItemId, db, entrances)
        }
    }


    private fun createTaskItemResult(taskItemId: Int, db: AppDatabase) {
        val newId = db.taskItemResultsDao().insert(
                TaskItemResultEntity(
                        0,
                        taskItemId,
                        GPSCoordinatesModel(0.0, 0.0, Date()),
                        null,
                        ""
                )
        )
        db.entrancesDao().insertAll(
                fragment.entrancesListAdapter.data.filter { it is ReportEntrancesListModel.Entrance }.map {
                    TaskItemResultEntranceEntity(
                            0,
                            newId.toInt(),
                            (it as ReportEntrancesListModel.Entrance).entranceNumber,
                            0
                    )
                }
        )
    }

    private suspend fun updateTaskItemGPSResult(taskItemId: Int, db: AppDatabase, gps: GPSCoordinatesModel?) {
        val result = db.taskItemResultsDao().getByTaskItemId(taskItemId)
        gps?.let {
            result?.gps = gps
        }
        result?.let {
            db.taskItemResultsDao().update(it)
        }
    }

    private suspend fun updateTaskItemDescriptionResult(taskItemId: Int, db: AppDatabase, description: String) {
        val result = db.taskItemResultsDao().getByTaskItemId(taskItemId)
        result?.let {
            it.description = description
            db.taskItemResultsDao().update(it)
        }
    }

    private suspend fun updateTaskItemEntrancesResult(taskItemId: Int, db: AppDatabase, entrances: List<ReportEntrancesListModel.Entrance>) {
        val result = db.taskItemResultsDao().getByTaskItemId(taskItemId)
        result ?: return
        val savedEntrances = db.entrancesDao().getByTaskItemResultId(result.id)
        savedEntrances.map {
            val sameEntrance = entrances.firstOrNull { entrance -> it.entrance == entrance.entranceNumber }
            val state = sameEntrance?.selected ?: 0
            it.state = state
        }
        savedEntrances.forEach { db.entrancesDao().update(it) }
    }

    fun onBlankPhotoClicked() {
        requestPhoto(false)
    }

    fun onBlankMultiPhotoClicked() {
        requestPhoto()
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (permissions[0] == android.Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                fragment.activity()?.showError("Необходимо разрешить приложению получать ваше местоположение.", object : ErrorButtonsListener {
                    override fun positiveListener() {
                        fragment.requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                    }

                    override fun negativeListener() {}
                }, "Ок", "Отмена")
            } else {
                requestPhoto()
            }
        }
    }

    private fun requestPhoto(multiPhoto: Boolean = true) {
        photoUUID = UUID.randomUUID()
        photoMultiMode = multiPhoto

        val photoFile = getTaskItemPhotoFile(fragment.taskItems[currentTask], photoUUID ?: UUID.randomUUID())

        val intent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent?.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))

        val packageManager = fragment.context?.packageManager

        CustomLog.writeToFile("RequestPhoto")
        packageManager?.let { packageManager
            CustomLog.writeToFile("PackageManager found")
            intent?.let { intent ->
                CustomLog.writeToFile("Intent found")
                if (intent.resolveActivity(packageManager) != null) {
                    CustomLog.writeToFile("Lifecycle: photo activity started")
                    fragment.startActivityForResult(intent, REQUEST_PHOTO)
                }else{
                    CustomLog.writeToFile("Can't resolve intent")
                }
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_PHOTO) {
            CustomLog.writeToFile("Lifecycle: photo activity received")
            if (resultCode != RESULT_OK) {
                if (resultCode != RESULT_CANCELED) {
                    (fragment.context as MainActivity).showError("Не удалось сделать фото.")
                }
                return false
            }
            val photoFile = getTaskItemPhotoFile(fragment.taskItems[currentTask], photoUUID ?: UUID.randomUUID())
            if (photoFile.exists()) {
                saveNewPhoto(photoFile.absolutePath)
                return true
            }

            if (data == null) {
                (fragment.context as MainActivity).showError("Не удалось сделать фото.")
                return false
            }

            val photo = saveNewPhoto(data.extras!!.get("data") as Bitmap)

            if (photo == null) {
                (fragment.context as MainActivity).showError("Не удалось сделать фото.")
                return false
            }

            return true
        }
        return false
    }

    private fun saveNewPhoto(path: String): File? {
        try {
            val bmp = BitmapFactory.decodeFile(path)
            return saveNewPhoto(bmp)
        } catch (e: Throwable) {
            (fragment.context as MainActivity).showError("Не удалось сохранить фотографию. Недостаточно памяти. Попробуйте сделать снимок еще раз. Если проблема повторится перезагрузите телефон.")
            e.printStackTrace()
        }

        return null
    }

    private fun saveNewPhoto(bmp: Bitmap?): File? {
        val photoFile = getTaskItemPhotoFile(fragment.taskItems[currentTask], photoUUID ?: UUID.randomUUID())
        if (bmp != null) {
            val photo: Bitmap
            try {
                photo = ImageUtils.resizeBitmap(bmp, 1024f, 768f)
                bmp.recycle()
            } catch (e: Throwable) {
                (fragment.context as MainActivity).showError("Не удалось сохранить фотографию. Недостаточно памяти. Попробуйте сделать снимок еще раз. Если проблема повторится перезагрузите телефон.")
                e.printStackTrace()
                bmp.recycle()
                return null
            }

            try {
                ImageUtils.saveImage(photo, photoFile, fragment.context?.contentResolver)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

            photo.recycle()
        }
        launch(UI) {
            val db = MyApplication.instance.database
            var currentGPS = application().currentLocation

            val photoEntity = TaskItemPhotoEntity(0, photoUUID.toString(), currentGPS, fragment.taskItems[currentTask].id)
            val photoModel = withContext(CommonPool) {
                val id = db.photosDao().insert(photoEntity)
                db.photosDao().getById(id.toInt()).toTaskItemPhotoModel(db)
            }

            photoModel?.let {
                fragment.photosListAdapter.data.add(
                        ReportPhotosListModel.TaskItemPhoto(
                                it,
                                Uri.fromFile(photoFile)
                        )
                )
            }
            fragment.photosListAdapter.notifyItemRangeChanged(fragment.photosListAdapter.data.size - 1, 2)

            if (photoMultiMode) {
                requestPhoto()
            }
        }

        return photoFile
    }

    fun onRemovePhotoClicked(holder: RecyclerView.ViewHolder) {
        val position = holder.adapterPosition
        if (position >= fragment.photosListAdapter.data.size ||
                position < 0) {

            (fragment.context as? MainActivity)?.showError("Не возможно удалить фото.")
            return
        }
        val status = File((fragment.photosListAdapter.data[holder.adapterPosition] as ReportPhotosListModel.TaskItemPhoto).photoURI.path).delete()
        if (!status) {
            (fragment.context as MainActivity).showError("Не возможно удалить фото из памяти.")
        }
        val taskItemPhotoId = (fragment.photosListAdapter.data[holder.adapterPosition] as ReportPhotosListModel.TaskItemPhoto).taskItem.id
        launch {
            val db = MyApplication.instance.database
            val photoEntity = db.photosDao().getById(taskItemPhotoId)
            db.photosDao().delete(photoEntity)
        }

        fragment.photosListAdapter.data.removeAt(holder.adapterPosition)
        fragment.photosListAdapter.notifyItemRemoved(holder.adapterPosition)
    }


    fun onCloseClicked() {
        if (!fragment.tasks[currentTask].canShowedByDate(Date())) {
            fragment.activity()?.showError("Задание больше недоступно.", object : ErrorButtonsListener {
                override fun positiveListener() {
                    fragment?.activity()?.showTaskListScreen()
                }

                override fun negativeListener() {
                }
            })
            return
        }

        if (fragment.taskItems[currentTask].needPhoto
                && fragment.photosListAdapter.data.filter { it is ReportPhotosListModel.TaskItemPhoto }.isEmpty()) {
            (fragment?.context as? MainActivity)?.showError("Необходимо сделать фотографии")
            return
        }

        closeClicked()
    }

    private fun isAllEntranceWithDefaults(): Boolean {

        fragment.taskItems[currentTask].entrancesData.forEach { default ->
            fragment.entrancesListAdapter.data
                    .filter { it is ReportEntrancesListModel.Entrance }
                    .map { it as ReportEntrancesListModel.Entrance }
                    .forEach { data ->
                        if (default.isEuroBoxes && (data.selected and 0x0001) == 0) {
                            return false
                        }
                        if (default.hasLookout && (data.selected and 0x0010) == 0) {
                            return false
                        }
                        if (default.isStacked && (data.selected and 0x0100) == 0) {
                            return false
                        }
                        if (default.isRefused && (data.selected and 0x1000) == 0) {
                            return false
                        }
                    }
        }
        return true
    }

    fun closeClicked() {
        val description = try {
            fragment.user_explanation_input.text.toString()
        } catch (e: Exception) {
            "can't get user explanation"
        }

        (fragment.context as MainActivity).showError("КНОПКИ НАЖАЛ?\nОТЧЁТ ОТПРАВЛЯЮ?\n(tugmachalari bosildi? " +
                "hisobot yuboringmi?)", object : ErrorButtonsListener {
            override fun positiveListener() {
                val status = closeTaskItem(description)
                if (!status) {
                    (fragment?.context as? MainActivity)?.showError("Произошла ошибка")
                } else {
                    fragment.activity()?.restartTaskClosingTimer()
                    if (!NetworkHelper.isNetworkEnabled(fragment.context)) {
                        (fragment.activity as? MainActivity)?.showNetworkDisabledError()
                    }
                }
            }
        }, "Да", "Нет", style = R.style.RedAlertDialog)
        return
    }

    private fun closeTaskItem(description: String): Boolean {
        val app = application()

        launch(UI) {
            val db = app.database
            val userToken = (app.user as? UserModel.Authorized)?.token ?: ""
            val entrances = fragment.entrancesListAdapter.data
                    .filter { it is ReportEntrancesListModel.Entrance }
                    .map { it as ReportEntrancesListModel.Entrance }

            withContext(CommonPool) {
                val location = application().currentLocation

                try {
                    createOrUpdateTaskResult(
                            fragment.taskItems[currentTask].id,
                            gps = location,
                            description = description,
                            entrances = entrances
                    )
                } catch (e: Throwable) {
                    CustomLog.writeToFile(CustomLog.getStacktraceAsString(e))
                }

                db.taskItemDao().getById(fragment.taskItems[currentTask].id)?.apply {
                    state = TaskItemModel.CLOSED
                }?.let {
                    db.taskItemDao().update(it)
                }

                db.taskDao().getById(fragment.tasks[currentTask].id)?.let {
                    if (it.state and TaskModel.EXAMINED != 0) {
                        it.state = TaskModel.STARTED
                        val token = (app.user as? UserModel.Authorized)?.token

                        db.sendQueryDao().insert(
                                SendQueryItemEntity(0,
                                        BuildConfig.API_URL + "/api/v1/tasks/${it.id}/accepted?token=" + (token
                                                ?: ""),
                                        ""
                                )
                        )
                    }

                    db.taskDao().update(it)
                }

                Log.d("BatteryLevel", "${((getBatteryLevel(fragment.context)
                        ?: 0f) * 100).roundToInt()}")

                val reportItem = ReportQueryItemEntity(
                        0, fragment.taskItems[currentTask].id, fragment.tasks[currentTask].id, fragment.taskItems[currentTask].address.id, location,
                        Date(), description,
                        fragment.entrancesListAdapter.data.filter { it is ReportEntrancesListModel.Entrance }.map {
                            Pair((it as ReportEntrancesListModel.Entrance).entranceNumber, it.selected)
                        },
                        userToken,
                        ((getBatteryLevel(fragment.context) ?: 0f) * 100).roundToInt()
                )

                db.reportQueryDao().insert(reportItem)
            }

            val int = Intent().apply {
                putExtra("changed_item", fragment.taskItems[currentTask])
                putExtra("changed_task", fragment.tasks[currentTask])
            }
            fragment.targetFragment?.onActivityResult(1, Activity.RESULT_OK, int)

            fragment.taskItems[currentTask].state = TaskItemModel.CLOSED
            val dateNow = Date()
            val openedTasks = fragment.taskItems.filterIndexed { i, it ->
                val parent = fragment.tasks[i]
                it.state == TaskItemModel.CREATED && parent.isAvailableByDate(dateNow)
            }
            if (openedTasks.isNotEmpty()) {
                val openedTaskPos = fragment.taskItems.indexOf(openedTasks.first())
                changeCurrentTask(openedTaskPos)
                fillTasksAdapterData()
            } else {
                (fragment.context as? MainActivity)?.onBackPressed()
            }
        }
        return true
    }

    private fun getBatteryLevel(context: Context?): Float? {
        val ifilter = IntentFilter("android.intent.action.BATTERY_CHANGED")
        val battery = context?.registerReceiver(null as BroadcastReceiver?, ifilter)
        if (battery == null) {
            return null
        } else {
            val level = battery.getIntExtra("level", -1)
            val scale = battery.getIntExtra("scale", -1)
            return level.toFloat() / scale.toFloat()
        }
    }

    fun onCouplingChanged(adapterPosition: Int) {
        val entrance = fragment.entrancesListAdapter.data[adapterPosition] as ReportEntrancesListModel.Entrance
        val currentCoupleState = fragment.taskItems[currentTask].entrances[adapterPosition].coupleEnabled

        val taskItemsWithSameCoupleType = fragment.taskItems.filterIndexed { idx, _ -> fragment.tasks[idx].coupleType == fragment.tasks[currentTask].coupleType }
        entrance.coupleEnabled = !currentCoupleState && taskItemsWithSameCoupleType.size > 1

        for ((idx, taskItem) in fragment.taskItems.withIndex()) {
            if (fragment.tasks[idx].coupleType == fragment.tasks[currentTask].coupleType)
                taskItem.entrances[adapterPosition].coupleEnabled = entrance.coupleEnabled && entrance.taskItem.state == TaskItemModel.CREATED
        }

        fragment.entrancesListAdapter.notifyItemChanged(adapterPosition)
    }
}
