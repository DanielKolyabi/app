package ru.relabs.kurjer.ui.presenters

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.support.v7.widget.RecyclerView
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
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.entities.*
import ru.relabs.kurjer.ui.fragments.ReportFragment
import ru.relabs.kurjer.ui.models.ReportEntrancesListModel
import ru.relabs.kurjer.ui.models.ReportPhotosListModel
import ru.relabs.kurjer.ui.models.ReportTasksListModel
import java.io.File
import java.util.*


const val REQUEST_PHOTO = 1

class ReportPresenter(private val fragment: ReportFragment) {
    lateinit var photoUUID: UUID
    var photoMultiMode: Boolean = false
    var currentTask = 0

    fun changeCurrentTask(taskNumber: Int) {
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

            fragment.setTaskListActiveTask(currentTask, false)
            fragment.setTaskListActiveTask(taskNumber, true)
            currentTask = taskNumber
            fragment.showHintText(fragment.taskItems[currentTask].notes)
        }

        fragment.close_button?.isEnabled = fragment.tasks[currentTask].isAvailableByDate(Date())

        fragment.close_button?.isEnabled = fragment.close_button.isEnabled && fragment.taskItems[currentTask].state != TaskItemModel.CLOSED
        fragment.user_explanation_input?.isEnabled = fragment.taskItems[currentTask].state != TaskItemModel.CLOSED

        (fragment.context as? MainActivity)?.changeTitle(fragment.taskItems[currentTask].address.name)
    }

    private fun fillDescriptionData(db: AppDatabase) {
        db.taskItemResultsDao().getByTaskItemId(fragment.taskItems[currentTask].id)?.let {
            launch(UI) {
                fragment.user_explanation_input?.setText(it.description)
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

    private fun getTaskItemEntranceData(taskItem: TaskItemModel, db: AppDatabase): List<ReportEntrancesListModel.Entrance> {
        val entrances = taskItem.entrances.map {
            it.coupleEnabled = it.coupleEnabled
                    && fragment.taskItems.size > 1
                    && taskItem.state == TaskItemModel.CREATED

            ReportEntrancesListModel.Entrance(taskItem, it.num, 0, it.coupleEnabled)
        }
        db.taskItemResultsDao().getByTaskItemId(taskItem.id)?.let {
            val savedEntrances = db.entrancesDao().getByTaskItemResultId(it.id)
            entrances.forEach { ent ->
                ent.selected = savedEntrances.first { it.entrance == ent.entranceNumber }.state
            }
        }

        return entrances
    }

    private fun fillEntrancesAdapterData(db: AppDatabase) {

        val entrances = getTaskItemEntranceData(fragment.taskItems[currentTask], db)

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
        val data = (fragment.entrancesListAdapter.data[holder.adapterPosition] as ReportEntrancesListModel.Entrance)
        data.selected = data.selected xor type
        val entrances = fragment.entrancesListAdapter.data
                .filter { it is ReportEntrancesListModel.Entrance }
                .map { it as ReportEntrancesListModel.Entrance }

        launch(CommonPool) {
            val db = MyApplication.instance.database

            if (data.coupleEnabled) {
                for (taskItem in fragment.taskItems) {
                    if (taskItem == fragment.taskItems[currentTask] || taskItem.state == TaskItemModel.CLOSED) {
                        continue
                    }

                    val taskItemEntrances = getTaskItemEntranceData(taskItem, db)
                    if (taskItemEntrances.size <= data.entranceNumber - 1) {
                        val cur = taskItemEntrances[data.entranceNumber - 1].selected
                        if (cur and type != data.selected and type) {
                            taskItemEntrances[data.entranceNumber - 1].selected = cur xor type
                        }
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
        val photoFile = getTaskItemPhotoFile(fragment.taskItems[currentTask], photoUUID)

        val intent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent?.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))

        val packageManager = fragment.context?.packageManager

        packageManager?.let { packageManager ->
            intent?.let { intent ->
                if (intent.resolveActivity(packageManager) != null) {
                    fragment.startActivityForResult(intent, REQUEST_PHOTO)
                }
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_PHOTO) {
            if (resultCode != RESULT_OK) {
                if (resultCode != RESULT_CANCELED) {
                    (fragment.context as MainActivity).showError("Не удалось сделать фото.")
                }
                return false
            }
            val photoFile = getTaskItemPhotoFile(fragment.taskItems[currentTask], photoUUID)
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
        val photoFile = getTaskItemPhotoFile(fragment.taskItems[currentTask], photoUUID)
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
        } else {
            if(fragment.taskItems[currentTask].needPhoto
                    && fragment.photosListAdapter.data.filter { it is ReportPhotosListModel.TaskItemPhoto }.isEmpty()){
                (fragment?.context as? MainActivity)?.showError("Необходимо сделать фотографии")
                return
            }

            val description = try {
                fragment.user_explanation_input.text.toString()
            } catch (e: Exception) {
                "can't get user explanation"
            }

            (fragment.context as MainActivity).showError("Вы уверен что хотите закрыть адрес?", object : ErrorButtonsListener {
                override fun positiveListener() {
                    val status = closeTaskItem(description)
                    if (!status) {
                        (fragment?.context as? MainActivity)?.showError("Произошла ошибка")
                    }
                }

                override fun negativeListener() {}
            }, "Да", "Нет")
            return
        }
    }

    private fun closeTaskItem(description: String): Boolean {
        val app = application()

        launch(UI) {
            val db = app.database
            val userToken = (app.user as UserModel.Authorized).token
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

                val reportItem = ReportQueryItemEntity(
                        0, fragment.taskItems[currentTask].id, fragment.tasks[currentTask].id, fragment.taskItems[currentTask].address.id, location,
                        Date(), description,
                        fragment.entrancesListAdapter.data.filter { it is ReportEntrancesListModel.Entrance }.map {
                            Pair((it as ReportEntrancesListModel.Entrance).entranceNumber, it.selected)
                        },
                        userToken
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

    fun onCouplingChanged(adapterPosition: Int) {
        val entrance = fragment.entrancesListAdapter.data[adapterPosition] as ReportEntrancesListModel.Entrance
        val currentCoupleState = fragment.taskItems[currentTask].entrances[adapterPosition].coupleEnabled

        entrance.coupleEnabled = !currentCoupleState && fragment.taskItems.size > 1

        for (taskItem in fragment.taskItems) {
            taskItem.entrances[adapterPosition].coupleEnabled = entrance.coupleEnabled && entrance.taskItem.state == TaskItemModel.CREATED
        }

        fragment.entrancesListAdapter.notifyItemChanged(adapterPosition)
    }
}
