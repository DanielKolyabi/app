package ru.relabs.kurjer

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.Channel
import ru.relabs.kurjer.CustomLog.getStacktraceAsString
import ru.relabs.kurjer.models.AddressModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.network.models.UpdateInfo
import ru.relabs.kurjer.persistence.PersistenceHelper
import ru.relabs.kurjer.ui.adapters.SearchInputAdapter
import ru.relabs.kurjer.ui.fragments.*
import ru.relabs.kurjer.ui.helpers.setVisible
import ru.relabs.kurjer.ui.models.AddressListModel
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import kotlin.math.roundToInt

const val REQUEST_LOCATION = 999
const val REQUEST_CODE_ALERT_NOTIFICATION = 998

class MainActivity : AppCompatActivity() {
    private var needRefreshShowed = false
    private var needForceRefresh = false
    private var networkErrorShowed = false
    private var serviceCheckingJob: Job? = null
    private val errorsChannel = Channel<() -> Unit>(Channel.UNLIMITED)
    private var errorsChannelHandlerJob: Job? = null
    private var timeLimitJob: Job? = null
    private var ringNotification: Uri? = null
    private var notificationMediaPlayer: MediaPlayer? = null

    val gpsSwitchStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                if (!NetworkHelper.isGPSEnabled(applicationContext)) {
                    NetworkHelper.displayLocationSettingsRequest(applicationContext, this@MainActivity)
                }
            }
        }
    }

    fun showNetworkDisabledError() {
        if (networkErrorShowed) {
            return
        }
        networkErrorShowed = true

        showError(
                "Небходимо включить передачу данных",
                object : ErrorButtonsListener {
                    override fun positiveListener() {
                        networkErrorShowed = false
                        if (!NetworkHelper.isNetworkEnabled(this@MainActivity)) {
                            showNetworkDisabledError()
                            return
                        }
                    }
//                    override fun negativeListener() {
//                        networkErrorShowed = false
//                        try {
//                            startActivity(Intent(Settings.ACTION_SETTINGS))
//                        } catch (e: Exception) {
//                            CustomLog.writeToFile(getStacktraceAsString(e))
//                            showError("Не удалось открыть настройки", object : ErrorButtonsListener {
//                                override fun positiveListener() {
//                                    showNetworkDisabledError()
//                                }
//                            })
//                        }
//                    }
                },
                "Ок"

        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_LOCATION) {
            if (resultCode == Activity.RESULT_OK) {
                application().enableLocationListening()
            }
        }else if(requestCode == REQUEST_CODE_ALERT_NOTIFICATION){
            if(resultCode == Activity.RESULT_OK){
                disableNotification()
            }else{
                startTaskClosingTimer()
            }
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            if (intent.getBooleanExtra("network_disabled", false)) {
                showNetworkDisabledError()
            }
            if (intent.getBooleanExtra("tasks_changed", false)) {
                if (needRefreshShowed) return

                needRefreshShowed = true
                showTasksRefreshDialog(true)
            }
            if (intent.getIntExtra("task_item_closed", 0) != 0) {
                run {
                    val db = (application as? MyApplication)?.database
                    db ?: return@run
                    launch(CommonPool) {
                        db.taskItemDao().getById(intent.getIntExtra("task_item_closed", 0))?.let {
                            it.state = TaskItemModel.CLOSED
                            db.taskItemDao().update(it)
                        }
                    }
                }
            }
        }
    }

    private fun showTasksRefreshDialog(cancelable: Boolean) {
        val negative = if (cancelable) "Позже" else ""
        showError("Необходимо обновить список заданий.", object : ErrorButtonsListener {
            override fun positiveListener() {
                needRefreshShowed = false
                needForceRefresh = false
                showTaskListScreen(true)
            }

            override fun negativeListener() {
                needForceRefresh = true
            }
        }, "Ок", negative)
    }

    fun enableNotification(){
        val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        v?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(1000L, 250L), 0))
            } else {
                v.vibrate(longArrayOf(1000L, 250L), 0)
            }
        }
        notificationMediaPlayer?.isLooping = true
        notificationMediaPlayer?.start()
    }

    fun disableNotification(){
        (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.cancel()
        notificationMediaPlayer?.stop()
    }

    fun startTaskClosingTimer() {
        if (timeLimitJob != null) {
            return
        }
        timeLimitJob = launch(DefaultDispatcher) {
            delay(15 * 60 * 1000) //TODO: Change to 15 min
            timeLimitJob = null

            if (application().database.taskDao().allOpened.isEmpty()) {
                return@launch
            }

            enableNotification()

            launch(CommonPool) {
                delay(30 * 1000)
                disableNotification()
            }

            startActivityForResult(Intent(applicationContext, AlertNotificationActivity::class.java), REQUEST_CODE_ALERT_NOTIFICATION)
        }
    }




    fun restartTaskClosingTimer() {
        timeLimitJob?.cancel()
        timeLimitJob = null
        startTaskClosingTimer()
    }


    override fun onResume() {
        CustomLog.writeToFile("Lifecycle: MainActivity resume")
        logFragmentBackstack()
        if (!NetworkHelper.isGPSEnabled(applicationContext)) {
            NetworkHelper.displayLocationSettingsRequest(applicationContext, this)
        }
        if (!NetworkHelper.isNetworkEnabled(applicationContext)) {
            showNetworkDisabledError()
        }
        registerReceiver(gpsSwitchStateReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        MyApplication.instance.enableLocationListening()

        serviceCheckingJob?.cancel()
        serviceCheckingJob = launch(DefaultDispatcher) {
            while (true) {
                if (!ReportService.isRunning) {
                    startService(Intent(this@MainActivity, ReportService::class.java))
                }
                delay(15000)
            }
        }
        errorsChannelHandlerJob?.cancel()
        errorsChannelHandlerJob = launch(DefaultDispatcher) {
            for (error in errorsChannel) {
                try {
                    launch(UI) {
                        error.invoke()
                    }
                } catch (e: java.lang.Exception) {
                    e.fillInStackTrace()
                    e.logError()
                }
            }
        }
        isRunning = true

        super.onResume()
    }

    override fun onDestroy() {
        CustomLog.writeToFile("Lifecycle: MainActivity destroy")
        super.onDestroy()
    }

    override fun onPause() {
        serviceCheckingJob?.cancel()
        errorsChannelHandlerJob?.cancel()
        isRunning = false
        CustomLog.writeToFile("Lifecycle: MainActivity paused")
        unregisterReceiver(gpsSwitchStateReceiver)
        (application as? MyApplication)?.disableLocationListening()
        super.onPause()

    }

    fun showPermissionsRequest(permissionsList: Array<String>, canDenied: Boolean) {
        if (permissionsList.isEmpty()) {
            return
        }
        var msg = "Необходимо разрешить приложению:\n"
        permissionsList.forEach {
            msg += when (it) {
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Доступ к записи файлов"
                android.Manifest.permission.READ_EXTERNAL_STORAGE -> "Доступ к чтению файлов"
                android.Manifest.permission.ACCESS_FINE_LOCATION -> "Доступ к получению местоположения"
                android.Manifest.permission.REQUEST_INSTALL_PACKAGES -> "Разрешать устанавливать приложения"
                android.Manifest.permission.READ_PHONE_STATE -> "Доступ к информации о телефоне"
                else -> "Неизвестно"
            } + "\n"
        }

        showError(msg, object : ErrorButtonsListener {
            override fun positiveListener() {
                ActivityCompat.requestPermissions(this@MainActivity, permissionsList, 1)
            }

            override fun negativeListener() {
                finish()
            }
        }, "Ок", if (canDenied) "Отмена" else "")
    }

    private fun logFragmentBackstack() = with(supportFragmentManager) {
        CustomLog.writeToFile("FragmentStack: " + (0..backStackEntryCount - 1).joinToString("\n") {
            "$it: " + getBackStackEntryAt(it).name
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CustomLog.writeToFile("SystemInfo: Free Memory (${PersistenceHelper.getFreeMemorySpace()})")
        CustomLog.writeToFile("Lifecycle: MainActivity created")
        logFragmentBackstack()
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        setContentView(R.layout.activity_main)

        notificationMediaPlayer = MediaPlayer.create(applicationContext, R.raw.notification_sound)

        supportActionBar?.hide()
        back_button.setOnClickListener {
            onBackPressed()
        }
        device_uuid.setOnClickListener {
            val deviceUUID = (application as? MyApplication)?.deviceUUID?.split("-")?.last() ?: ""
            if (deviceUUID == "") {
                showError("Не удалось получить device UUID")
                return@setOnClickListener
            }
            showError("Device UUID part: $deviceUUID", object : ErrorButtonsListener {
                override fun positiveListener() {
                    try {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.primaryClip = ClipData.newPlainText("Device UUID", deviceUUID)
                        Toast.makeText(this@MainActivity, "Скопированно в буфер обмена", Toast.LENGTH_LONG).show()
                    } catch (e: java.lang.Exception) {
                        Toast.makeText(this@MainActivity, "Произошла ошибка", Toast.LENGTH_LONG).show()
                    }
                }

                override fun negativeListener() {
                    try {
                        CustomLog.share(this@MainActivity)
                    } catch (e: FileNotFoundException) {
                        Toast.makeText(this@MainActivity, "crash.log отсутствует", Toast.LENGTH_LONG).show()
                    } catch (e: java.lang.Exception) {
                        CustomLog.writeToFile(getStacktraceAsString(e))
                        Toast.makeText(this@MainActivity, "Произошла ошибка", Toast.LENGTH_LONG).show()
                    }
                }
            }, "Скопировать", "Отправить crash.log", cancelable = true)
        }

        if (supportFragmentManager.backStackEntryCount == 0) {
            showLoginScreen()
        } else {
            restoreApplicationUser()
        }
        Thread.setDefaultUncaughtExceptionHandler(MyExceptionHandler())
        supportFragmentManager.addOnBackStackChangedListener {
            val current = supportFragmentManager.findFragmentByTag("fragment")

            setSearchButtonVisible(current is TaskListFragment || current is AddressListFragment)
            if (search_input.visibility == View.VISIBLE) {
                search_input.setVisible(false)
                (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(search_input.windowToken, 0)
                top_app_bar.title.setVisible(true)
            }

            setNavigationRefreshVisible(current is TaskListFragment)
            when (current) {
                is TaskListFragment ->
                    changeTitle("Список заданий")
                is AddressListFragment ->
                    changeTitle("Список адресов")
            }

            updateSearchInputHint(current)

            setDeviceIdButtonVisible(current is TaskListFragment)
            if (needForceRefresh && current is TaskListFragment) {
                showTasksRefreshDialog(true)
            }
        }
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.READ_PHONE_STATE)
        }

        try {
            registerReceiver(broadcastReceiver, IntentFilter("NOW"))
        } catch (e: java.lang.Exception) {
            e.logError()
        }

        showPermissionsRequest(permissions.toTypedArray(), false)
        loading.setVisible(true)
        checkUpdates()

        search_button.setOnClickListener {
            val current = supportFragmentManager.findFragmentByTag("fragment")
            if (current !is TaskListFragment && current !is AddressListFragment) {
                setSearchButtonVisible(false)
                return@setOnClickListener
            }
            setSearchInputVisible(true)
            (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(search_input, InputMethodManager.SHOW_IMPLICIT)
            search_input.requestFocus()
        }
        val adapter = SearchInputAdapter(this, R.layout.item_search, R.id.text, supportFragmentManager)
        search_input.setAdapter(adapter)

        search_input.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_NEXT ||
                    event != null &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    event.keyCode == KeyEvent.KEYCODE_ENTER) {

                val current = supportFragmentManager.findFragmentByTag("fragment") as? SearchableFragment
                current ?: return@setOnEditorActionListener true

                current.onItemSelected(search_input.text.toString(), search_input)

                (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(search_input.windowToken, 0)

                search_input.setVisible(false)
                top_app_bar.title.setVisible(true)

                if (current is TaskListFragment) {
                    setDeviceIdButtonVisible(true)
                }

                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun restoreApplicationUser() {
        val sharedPref = application().getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", "")
        val login = sharedPref.getString("login", "")
        if (token.isBlank() || login.isBlank()) {
            showLoginScreen()
        } else {
            application().user = UserModel.Authorized(login, token)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1) {
            permissions.indexOfFirst { it == android.Manifest.permission.ACCESS_FINE_LOCATION }.let {
                if (it >= 0 && grantResults[it] == PackageManager.PERMISSION_GRANTED && !MyApplication.instance.enableLocationListening()) {
                    showError("Невозможно включить геолокацию")
                }
            }
            val deniedPermission = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }

            showPermissionsRequest(deniedPermission.toTypedArray(), true)
        } else {
            supportFragmentManager.findFragmentByTag("fragment")?.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    fun installUpdate(url: URL) {
        launch {
            var file: File?
            try {
                progress_bar.isIndeterminate = false
                file = NetworkHelper.loadUpdateFile(url) { current, total ->
                    val percents = current.toFloat() / total.toFloat()
                    launch(UI) {
                        loader_progress_text.setVisible(true)
                        loader_progress_text.setText("Загрузка: ${(percents * 100).roundToInt()}%")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(UI) {
                    loader_progress_text.setVisible(false)
                    loading.setVisible(false)
                    progress_bar.isIndeterminate = true
                    showError("Не удалось загрузить файл обновления.")
                }
                return@launch
            }

            launch(UI) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                progress_bar.isIndeterminate = true
                loader_progress_text.setVisible(false)
                loading.setVisible(false)
                startActivity(intent)
            }
        }
    }

    private fun setDeviceIdButtonVisible(visible: Boolean) {
        device_uuid.setVisible(visible)
    }

    private fun setSearchButtonVisible(visible: Boolean) {
        search_button.setVisible(visible)
        if (!visible) {
            setSearchInputVisible(visible)
        }
    }

    private fun setSearchInputVisible(visible: Boolean) {
        search_input.setVisible(visible)
        search_input.setText("")
        setDeviceIdButtonVisible(!visible)
        top_app_bar.title.setVisible(!visible)
    }


    private fun processUpdate(updateInfo: UpdateInfo): Boolean {
        if (updateInfo.version > BuildConfig.VERSION_CODE) {

            showError("Доступно новое обновление.", object : ErrorButtonsListener {
                override fun positiveListener() {
                    try {
                        Log.d("updates", "Try install from ${updateInfo.url}")
                        loading.setVisible(true)
                        installUpdate(URL(updateInfo.url))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        loading.setVisible(false)
                        showError("Не удалось установить обновление")
                    }
                }

                override fun negativeListener() {
                    loading.setVisible(false)
                }

            }, "Установить", if (updateInfo.isRequired) "" else "Напомнить позже")
            return true
        }
        return false
    }

    fun checkUpdates() {
        if (!NetworkHelper.isNetworkAvailable(this)) {
            loading.setVisible(false)
            showError("Не удалось получить информацию об обновлениях.")
            return
        }

        launch(UI) {
            try {
                val updateInfo = DeliveryServerAPI.api.getUpdateInfo().await()
                application().lastRequiredAppVersion = updateInfo?.last_required?.version ?: 0
                if (updateInfo.last_required.version <= BuildConfig.VERSION_CODE
                        && updateInfo.last_optional.version <= BuildConfig.VERSION_CODE) {
                    loading.setVisible(false)
                    return@launch
                }

                if (processUpdate(updateInfo.last_required)) return@launch
                if (processUpdate(updateInfo.last_optional)) return@launch

            } catch (e: Exception) {
                e.printStackTrace()
                loading.setVisible(false)
                showError("Не удалось получить информацию об обновлениях.")
            }
        }
    }

    fun showTaskListScreen(shouldUpdate: Boolean = false, posInList: Int = 0): TaskListFragment {
        val fragment = TaskListFragment.newInstance(shouldUpdate, posInList)
        navigateTo(fragment)
        changeTitle("Список заданий")
        return fragment
    }

    fun showLoginScreen(): LoginFragment {
        try {
            CustomLog.writeToFile("showLoginScreen stackTrace:\n" + Thread.currentThread().stackTrace.joinToString("\n"))
        } catch (e: Exception) {
            e.logError()
        }
        val fragment = LoginFragment()
        navigateTo(fragment)
        changeTitle("Авторизация")
        return fragment
    }

    fun showYandexMap(taskItems: List<TaskItemModel>, onAddressClicked: (AddressModel) -> Unit): YandexMapFragment {
        val addresses = taskItems.groupBy { it.address.id }
                .mapValues { entry ->
                    entry.value.firstOrNull { it.needPhoto }
                            ?: (entry.value.firstOrNull { it.state == TaskItemModel.CLOSED }
                                    ?: entry.value.firstOrNull())
                }
                .mapNotNull { entry ->
                    val value = entry.value
                    if (value is TaskItemModel) {
                        val color = if (value.state == TaskItemModel.CLOSED) {
                            //Gray
                            Color.GRAY
                        } else if (value.needPhoto) {
                            //Red
                            resources.getColor(R.color.colorAccent)
                        } else {
                            //Orange
                            Color.argb(255, 255, 165, 0)
                        }
                        YandexMapFragment.AddressWithColor(value.address, color)
                    } else {
                        null
                    }
                }
        val fragment = YandexMapFragment.newInstance(addresses)
        fragment.onAddressClicked = onAddressClicked
        navigateTo(fragment, true)
        changeTitle("Карта")
        return fragment
    }

    fun showAddressListScreen(tasks: List<TaskModel>): AddressListFragment {
        if (tasks.isEmpty()) {
            showError("Вы не выбрали ни одной задачи.")
        }
        val fragment = AddressListFragment.newInstance(tasks)
        navigateTo(fragment, true)
        changeTitle("Список адресов")
        return fragment
    }

    private fun showErrorInternal(
            errorMessage: String,
            listener: ErrorButtonsListener? = null,
            forcePositiveButtonName: String = "Ок",
            forceNegativeButtonName: String = "",
            cancelable: Boolean = false,
            style: Int? = null
    ) {
        try {
            val builder = (if (style == null) AlertDialog.Builder(this) else AlertDialog.Builder(this, style))
                    .setMessage(errorMessage)

            if (forcePositiveButtonName.isNotBlank()) {
                builder.setPositiveButton(forcePositiveButtonName) { _, _ -> listener?.positiveListener() }
            }
            if (forceNegativeButtonName.isNotBlank()) {
                builder.setNegativeButton(forceNegativeButtonName) { _, _ -> listener?.negativeListener() }
            }
            builder.setCancelable(cancelable)
            builder.show()
        } catch (e: Throwable) {
            CustomLog.writeToFile(getStacktraceAsString(e))
        }
    }

    fun showError(
            errorMessage: String,
            listener: ErrorButtonsListener? = null,
            forcePositiveButtonName: String = "Ок",
            forceNegativeButtonName: String = "",
            cancelable: Boolean = false,
            style: Int? = null
    ) {
        if (!isRunning) {
            errorsChannel.offer {
                showErrorInternal(errorMessage, listener, forcePositiveButtonName, forceNegativeButtonName, cancelable, style)
            }
            return
        }
        showErrorInternal(errorMessage, listener, forcePositiveButtonName, forceNegativeButtonName, cancelable, style)
    }

    fun showTaskDetailsScreen(task: TaskModel, posInList: Int) {
        navigateTo(TaskDetailsFragment.newInstance(task, posInList), true)
        changeTitle("Детали задания")
    }

    fun showTaskItemExplanation(item: TaskItemModel) {
        navigateTo(TaskItemExplanationFragment.newInstance(item), true)
        changeTitle("Пояснения к заданию")

    }

    fun showTasksReportScreen(tasks: List<AddressListModel.TaskItem>, selectedTaskId: Int): ReportFragment {

        val fragment = ReportFragment.newInstance(
                tasks.map {
                    it.parentTask
                },
                tasks.map {
                    it.taskItem
                },
                selectedTaskId
        )
        navigateTo(fragment, true)
        return fragment
    }

    fun changeTitle(title: String) {
        top_app_bar.title.text = title
    }

    fun navigateTo(fragment: Fragment, isAddToBackStack: Boolean = false) {
        try {
            if (!isAddToBackStack) {
                clearBackStack()
            }

            supportFragmentManager.beginTransaction().apply {
                replace(R.id.fragment_container, fragment, "fragment")
                if (isAddToBackStack) {
                    addToBackStack(fragment.javaClass.name)
                }
            }.commit()

            val backVisible = when (fragment) {
                is LoginFragment -> false
                is TaskListFragment -> false
                else -> true
            }

            updateSearchInputHint(fragment)
            setSearchButtonVisible(fragment is TaskListFragment || fragment is AddressListFragment)
            if (search_input.visibility == View.VISIBLE) {
                search_input.setVisible(false)
                (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(search_input.windowToken, 0)
                top_app_bar.title.setVisible(true)
            }
            setNavigationBackVisible(backVisible)
            setNavigationRefreshVisible(fragment is TaskListFragment)
            setDeviceIdButtonVisible(fragment is TaskListFragment)
        } catch (e: Throwable) {
            CustomLog.writeToFile(getStacktraceAsString(e))
            e.printStackTrace()
        }
    }

    private fun updateSearchInputHint(fragment: Fragment?) {

        when (fragment) {
            is TaskListFragment ->
                search_input?.hint = "Номер участка"
            is AddressListFragment ->
                search_input?.hint = "Пробел - список улиц"
        }
    }

    private fun setNavigationRefreshVisible(visible: Boolean) {
        refresh_button.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setNavigationBackVisible(visible: Boolean) {
        back_button.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun clearBackStack() {

        val backStackEntryCount = this
                .supportFragmentManager
                .backStackEntryCount

        for (i in 0 until backStackEntryCount) {
            this.supportFragmentManager.popBackStackImmediate()
        }
    }

    override fun onBackPressed() {
        try {
            if (search_input.visibility == View.VISIBLE) {
                search_input.setVisible(false)
                (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(search_input.windowToken, 0)
                top_app_bar.title.setVisible(true)
                val current = supportFragmentManager?.findFragmentByTag("fragment")
                if (current is TaskListFragment) {
                    setDeviceIdButtonVisible(true)
                }
                return
            }

            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                super.onBackPressed()
            }

            setNavigationBackVisible(supportFragmentManager.backStackEntryCount > 1)
        } catch (e: Throwable) {
            CustomLog.writeToFile(getStacktraceAsString(e))
            super.onBackPressed()
        }
    }

    companion object {
        var isRunning: Boolean = false
    }
}

interface ErrorButtonsListener {
    fun positiveListener() {}
    fun negativeListener() {}
}
