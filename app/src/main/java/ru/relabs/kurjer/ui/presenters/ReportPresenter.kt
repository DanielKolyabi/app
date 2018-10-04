package ru.relabs.kurjer.ui.presenters

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
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
import ru.relabs.kurjer.models.*
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
    var currentTask = 0

    fun changeCurrentTask(taskNumber: Int) {
        fragment.setTaskListActiveTask(currentTask, false)
        fragment.setTaskListActiveTask(taskNumber, true)
        currentTask = taskNumber
        fragment.showHintText(fragment.taskItems[currentTask].notes)
        launch {
            val db = (fragment.activity!!.application as MyApplication).database
            fillEntrancesAdapterData(db)
            fillPhotosAdapterData(db)
            fillDescriptionData(db)
            withContext(UI) {
                fragment.loading.visibility = View.GONE
            }
        }

        fragment.close_button.isEnabled = !(Date() < fragment.tasks[currentTask].startTime ||
                Date() > Date(fragment.tasks[currentTask].endTime.time + 3 * 24 * 60 * 60 * 1000))

        fragment.close_button.isEnabled = fragment.close_button.isEnabled && fragment.taskItems[currentTask].state != TaskItemModel.CLOSED
        fragment.user_explanation_input.isEnabled = fragment.taskItems[currentTask].state != TaskItemModel.CLOSED

        (fragment.context as? MainActivity)?.changeTitle(fragment.taskItems[currentTask].address.name)
    }

    private fun fillDescriptionData(db: AppDatabase) {
        db.taskItemResultsDao().getByTaskItemId(fragment.taskItems[currentTask].id)?.let {
            launch(UI) {
                fragment.user_explanation_input.setText(it.description)
            }
        }
    }

    fun fillTasksAdapterData() {
        fragment.tasksListAdapter.data.addAll(fragment.tasks.mapIndexed { i, it ->
            ReportTasksListModel.TaskButton(it, i, i == 0)
        })
        fragment.tasksListAdapter.notifyDataSetChanged()
    }


    fun fillEntrancesAdapterData(db: AppDatabase) {
        val sharedPref = fragment.activity()?.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        val shouldLoadSameAddress = sharedPref?.getBoolean("remember_report_states", false) ?: false

        val savedEntrancesAtSameAddress = if (shouldLoadSameAddress)
            findEntrancesByAddress(db)
        else
            listOf()

        //create entrance models with loading state from saved address
        val entrances = fragment.taskItems[currentTask].entrances.map {
            val savedEntrance = savedEntrancesAtSameAddress.firstOrNull { saved ->
                it == saved.entrance
            }
            val savedState = if (shouldLoadSameAddress)
                savedEntrance?.state ?: 0
            else
                0

            ReportEntrancesListModel.Entrance(fragment.taskItems[currentTask], it, savedState)
        }
        //find saved state directly for this entrances
        db.taskItemResultsDao().getByTaskItemId(fragment.taskItems[currentTask].id)?.let {
            val savedEntrances = db.entrancesDao().getByTaskItemResultId(it.id)
            entrances.map { ent ->
                ent.selected = savedEntrances.first { it.entrance == ent.entranceNumber }.state
            }
        }

        launch(UI) {
            fragment.entrancesListAdapter.data.clear()
            if (fragment.tasks.size > 1) {
                fragment.setTaskListVisible(true)
            }
            fragment.entrancesListAdapter.data.addAll(entrances)
            fragment.entrancesListAdapter.notifyDataSetChanged()
        }
    }

    private fun findEntrancesByAddress(db: AppDatabase): List<TaskItemResultEntranceModel> {
        val result = mutableListOf<TaskItemResultEntranceModel>()
        db.taskItemDao().getByAddressId(fragment.taskItems[currentTask].address.id).filter {
            it.taskId in fragment.tasks.map { it.id }
        }.forEach {
            db.taskItemResultsDao().getByTaskItemId(it.id)?.let {
                db.entrancesDao().getByTaskItemResultId(it.id).filter {
                    it.state != 0
                }.forEach {
                    result.add(it.toTaskItemResultEntranceModel())
                }
            }
        }
        return result
    }

    fun fillPhotosAdapterData(db: AppDatabase) {
        fragment.photosListAdapter.data.clear()
        val taskPhotos = db.photosDao().getByTaskItemId(fragment.taskItems[currentTask].id).map {
            it.toTaskItemPhotoModel(db)
        }

        launch(UI) {
            taskPhotos.forEach {
                fragment.photosListAdapter.data.add(
                        ReportPhotosListModel.TaskItemPhoto(it, it.getPhotoURI())
                )
            }
            fragment.photosListAdapter.data.add(ReportPhotosListModel.BlankPhoto)
            fragment.photosListAdapter.notifyDataSetChanged()
        }
    }

    fun onEntranceSelected(type: Int, holder: RecyclerView.ViewHolder) {
        val data = (fragment.entrancesListAdapter.data[holder.adapterPosition] as ReportEntrancesListModel.Entrance)
        data.selected = data.selected xor type
        launch(CommonPool) {
            createOrUpdateTaskResult()
        }
        fragment.entrancesListAdapter.notifyItemChanged(holder.adapterPosition)
    }

    fun onDescriptionChanged() {
        launch(CommonPool) {
            try {
                createOrUpdateTaskResult()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun createOrUpdateTaskResult(gps: GPSCoordinatesModel? = null) {
        val db = (fragment.activity!!.application as MyApplication).database
        val taskItemEntity = db.taskItemDao().getById(fragment.taskItems[currentTask].id)
        val taskItemResult = db.taskItemResultsDao().getByTaskItemId(taskItemEntity.id)
        if (taskItemResult == null) {
            createTaskItemResult(db)
        } else {
            updateTaskItemResult(db, gps)
        }
    }

    private fun createTaskItemResult(db: AppDatabase) {
        val newId = db.taskItemResultsDao().insert(
                TaskItemResultEntity(
                        0,
                        fragment.taskItems[currentTask].id,
                        GPSCoordinatesModel(0.0, 0.0, Date()),
                        null,
                        fragment.user_explanation_input.text.toString()
                )
        )
        db.entrancesDao().insertAll(
                fragment.entrancesListAdapter.data.filter { it is ReportEntrancesListModel.Entrance }.map {
                    TaskItemResultEntranceEntity(
                            0,
                            newId.toInt(),
                            (it as ReportEntrancesListModel.Entrance).entranceNumber,
                            it.selected
                    )
                }
        )
    }

    private suspend fun updateTaskItemResult(db: AppDatabase, gps: GPSCoordinatesModel?) {
        val result = db.taskItemResultsDao().getByTaskItemId(fragment.taskItems[currentTask].id)
        val entrances = db.entrancesDao().getByTaskItemResultId(result!!.id)
        result.description = withContext(UI) {fragment.user_explanation_input.text.toString()}
        entrances.map {
            it.state = (fragment.entrancesListAdapter.data.filter { it is ReportEntrancesListModel.Entrance }.first { entData ->
                (entData as ReportEntrancesListModel.Entrance).entranceNumber == it.entrance
            } as ReportEntrancesListModel.Entrance).selected
        }
        gps?.let {
            result.gps = gps
        }
        db.taskItemResultsDao().update(result)
        entrances.forEach { db.entrancesDao().update(it) }
    }


    fun onBlankPhotoClicked() {
//        if (ContextCompat.checkSelfPermission(fragment.context!!, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            fragment.activity()?.showError("Необходимо разрешить приложению доступ к записи файлов.", object : ErrorButtonsListener {
//                override fun positiveListener() {
//                    fragment.requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 2)
//                }
//
//                override fun negativeListener() {}
//            }, "Ок", "")
//        } else {
        requestPhoto()
//        }
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

    private fun requestPhoto() {
        photoUUID = UUID.randomUUID()
        val photoFile = getTaskItemPhotoFile(fragment.taskItems[currentTask], photoUUID)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))

        if (intent.resolveActivity(fragment.context?.packageManager) != null) {
            fragment.startActivityForResult(intent, REQUEST_PHOTO)
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
                saveNewPhoto(BitmapFactory.decodeFile(photoFile.absolutePath))
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

    private fun saveNewPhoto(bmp: Bitmap?): File? {
        val photoFile = getTaskItemPhotoFile(fragment.taskItems[currentTask], photoUUID)
        if (bmp != null) {
            val photo = ImageUtils.resizeBitmap(bmp, 1280f, 768f)
            bmp.recycle()

            try {
                ImageUtils.saveImage(photo, photoFile, fragment.context?.contentResolver)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

            photo.recycle()
        }
        launch(UI) {
            val db = (fragment.activity!!.application as MyApplication).database
            var currentGPS = GPSCoordinatesModel(0.toDouble(), 0.toDouble(), Date())
            fragment.application()?.let {
                currentGPS = it.currentLocation
            }
            val photoEntity = TaskItemPhotoEntity(0, photoUUID.toString(), currentGPS, fragment.taskItems[currentTask].id)
            val photoModel = withContext(CommonPool) {
                val id = db.photosDao().insert(photoEntity)
                db.photosDao().getById(id.toInt()).toTaskItemPhotoModel(db)
            }

            fragment.photosListAdapter.data.add(fragment.photosListAdapter.data.size - 1,
                    ReportPhotosListModel.TaskItemPhoto(
                            photoModel,
                            Uri.fromFile(photoFile)
                    )
            )
            fragment.photosListAdapter.notifyItemRangeChanged(fragment.photosListAdapter.data.size - 1, 2)

            requestPhoto()
        }

        return photoFile
    }

    fun onRemovePhotoClicked(holder: RecyclerView.ViewHolder) {
        val status = File((fragment.photosListAdapter.data[holder.adapterPosition] as ReportPhotosListModel.TaskItemPhoto).photoURI.path).delete()
        if (!status) {
            (fragment.context as MainActivity).showError("Не возможно удалить фото из памяти.")
        }
        val taskItemPhotoId = (fragment.photosListAdapter.data[holder.adapterPosition] as ReportPhotosListModel.TaskItemPhoto).taskItem.id
        launch {
            val db = (fragment.activity!!.application as MyApplication).database
            val photoEntity = db.photosDao().getById(taskItemPhotoId)
            db.photosDao().delete(photoEntity)
        }

        fragment.photosListAdapter.data.removeAt(holder.adapterPosition)
        fragment.photosListAdapter.notifyItemRemoved(holder.adapterPosition)
    }

    fun onCloseClicked() {
        val nothingSelected = fragment.entrancesListAdapter.data.filter { it is ReportEntrancesListModel.Entrance }.any {
            (it as ReportEntrancesListModel.Entrance).selected == 0
        }
        //nothingSelected = nothingSelected or fragment.user_explanation_input.text.isBlank()

        if (nothingSelected) {
            (fragment.context as MainActivity).showError("Вы уверен что хотите закрыть адрес?", object : ErrorButtonsListener {
                override fun positiveListener() {
                    closeTaskItem()
                }

                override fun negativeListener() {}
            }, "Да", "Нет")
            return
        }
        closeTaskItem()
    }

    private fun closeTaskItem() {
        launch(UI) {
            val db = (fragment.activity!!.application as MyApplication).database
            val userToken = (fragment.application()!!.user as UserModel.Authorized).token
            withContext(CommonPool) {
                val location = fragment.application()?.currentLocation
                createOrUpdateTaskResult(location)
                db.taskItemDao().update(
                        db.taskItemDao().getById(fragment.taskItems[currentTask].id).let {
                            it.state = 1
                            it
                        }
                )
                db.taskDao().update(
                        db.taskDao().getById(fragment.tasks[currentTask].id)!!.let {
                            if (it.state and TaskModel.EXAMINED != 0) {
                                it.state = TaskModel.STARTED
                                db.sendQueryDao().insert(
                                        SendQueryItemEntity(0,
                                                BuildConfig.API_URL + "/api/v1/tasks/${it.id}/accepted?token=" + (fragment.application()!!.user as UserModel.Authorized).token,
                                                ""
                                        )
                                )
                            }
                            it
                        }
                )

                val reportItem = ReportQueryItemEntity(
                        0, fragment.taskItems[currentTask].id, fragment.tasks[currentTask].id, fragment.taskItems[currentTask].address.id, location,
                        Date(), fragment.user_explanation_input.text.toString(),
                        fragment.entrancesListAdapter.data.filter { it is ReportEntrancesListModel.Entrance }.map {
                            Pair((it as ReportEntrancesListModel.Entrance).entranceNumber, it.selected)
                        },
                        userToken
                )

                db.reportQueryDao().insert(reportItem)
            }


            (fragment.context as MainActivity).onBackPressed()
        }
    }
}
