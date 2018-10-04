package ru.relabs.kurjer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.relabs.kurjer.models.AddressModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.network.DeliveryServerAPI
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.network.models.UpdateInfo
import ru.relabs.kurjer.ui.fragments.*
import ru.relabs.kurjer.ui.helpers.setVisible
import ru.relabs.kurjer.ui.models.AddressListModel
import java.io.File
import java.net.URL
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            if (intent.getBooleanExtra("tasks_changed", false)) {
                showError("Необходимо обновить список заданий.", object : ErrorButtonsListener {
                    override fun positiveListener() {
                        showTaskListScreen(true)
                    }

                    override fun negativeListener() {}
                }, "Ок", "")
            }
        }
    }
    private var intentFilter = IntentFilter("NOW")

    override fun onResume() {
        registerReceiver(broadcastReceiver, intentFilter)

        (application as MyApplication).enableLocationListening()
        super.onResume()
    }

    override fun onPause() {
        unregisterReceiver(broadcastReceiver)
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
                android.Manifest.permission.ACCESS_FINE_LOCATION -> "Доступ к получению местоположения"
                android.Manifest.permission.REQUEST_INSTALL_PACKAGES -> "Разрешать устанавливать приложения"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        back_button.setOnClickListener {
            onBackPressed()
        }
        chain_button.setOnClickListener {
            onChainPressed()
        }

        showLoginScreen()
        Thread.setDefaultUncaughtExceptionHandler(MyExceptionHandler())
        supportFragmentManager.addOnBackStackChangedListener {
            val current = supportFragmentManager.findFragmentByTag("fragment")
            setNavigationRefreshVisible(current is TaskListFragment)
            when (current) {
                is TaskListFragment -> changeTitle("Список заданий")
                is AddressListFragment -> changeTitle("Список адресов")
            }
            if (current !is ReportFragment) {
                setChainButtonVisible(false)
            }
        }
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        setChainButtonIconEnabled(
                getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                        .getBoolean("remember_report_states", false)
        )

        showPermissionsRequest(permissions.toTypedArray(), false)
        loading.setVisible(true)
        checkUpdates()
    }

    fun setChainButtonVisible(visible: Boolean) {
        //TODO: Remove Chain_Button
        chain_button.setVisible(false)//visible)
    }

    private fun onChainPressed() {
        val pref = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        val currentState = pref.getBoolean("remember_report_states", false)
        val nextState = !currentState
        var message = "Включено запоминание выбора для одинаковых адресов"
        if (!nextState) {
            message = "Выключено запоминание выбора для одинаковых адресов"
        }
        setChainButtonIconEnabled(nextState)

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        pref.edit()
                .putBoolean("remember_report_states", nextState)
                .apply()
    }

    private fun setChainButtonIconEnabled(enabled: Boolean){
        if (!enabled) {
            chain_button.setImageResource(R.drawable.ic_chain_disabled)
        } else {
            chain_button.setImageResource(R.drawable.ic_chain_enabled)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1) {
            permissions.indexOfFirst { it == android.Manifest.permission.ACCESS_FINE_LOCATION }.let {
                if (it >= 0 && grantResults[it] == PackageManager.PERMISSION_GRANTED && !(application as MyApplication).enableLocationListening()) {
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
            var file: File? = null
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

    private fun processUpdate(updateInfo: UpdateInfo): Boolean {
        if (updateInfo.version > BuildConfig.VERSION_CODE) {

            showError("Доступно новое обновление.", object : ErrorButtonsListener {
                override fun positiveListener() {
                    try {
                        Log.d("updates", "Try install from ${updateInfo.url}")
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

    fun showTaskListScreen(shouldUpdate: Boolean = false) {
        navigateTo(TaskListFragment.newInstance(shouldUpdate))
        changeTitle("Список заданий")
    }

    fun showLoginScreen() {
        navigateTo(LoginFragment())
        changeTitle("Авторизация")
    }

    fun showYandexMap(address: AddressModel) {
        navigateTo(YandexMapFragment.newInstance(address), true)
        changeTitle(address.name)
    }

    fun showAddressListScreen(tasks: List<TaskModel>) {
        if (tasks.isEmpty()) {
            showError("Вы не выбрали ни одной задачи.")
        }
        navigateTo(AddressListFragment.newInstance(tasks), true)
        changeTitle("Список адресов")
    }

    fun showError(errorMessage: String, listener: ErrorButtonsListener? = null, forcePositiveButtonName: String = "Ок", forceNegativeButtonName: String = "") {
        val builder = AlertDialog.Builder(this)
                .setMessage(errorMessage)

        if (forcePositiveButtonName.isNotBlank()) {
            builder.setPositiveButton(forcePositiveButtonName) { _, _ -> listener?.positiveListener() }
        }
        if (forceNegativeButtonName.isNotBlank()) {
            builder.setNegativeButton(forceNegativeButtonName) { _, _ -> listener?.negativeListener() }
        }
        builder.setCancelable(false)
        builder.show()
    }

    fun showTaskDetailsScreen(task: TaskModel) {
        navigateTo(TaskDetailsFragment.newInstance(task), true)
        changeTitle("Детали задания")
    }

    fun showTaskItemExplanation(item: TaskItemModel) {
        navigateTo(TaskItemExplanationFragment.newInstance(item), true)
        changeTitle("Пояснения к заданию")

    }

    fun showTasksReportScreen(tasks: List<AddressListModel.TaskItem>, selectedTaskId: Int) {

        navigateTo(ReportFragment.newInstance(
                tasks.map {
                    it.parentTask
                },
                tasks.map {
                    it.taskItem
                },
                selectedTaskId
        ), true)
    }

    fun changeTitle(title: String) {
        top_app_bar.title.text = title
    }

    fun navigateTo(fragment: Fragment, isAddToBackStack: Boolean = false) {
        if (!isAddToBackStack) {
            clearBackStack()
        }

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment, "fragment")
            if (isAddToBackStack) {
                addToBackStack(null)
            }
        }.commit()

        val backVisible = when (fragment) {
            is LoginFragment -> false
            is TaskListFragment -> false
            else -> true
        }

        setNavigationBackVisible(backVisible)
        setNavigationRefreshVisible(fragment is TaskListFragment)
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
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }

        setNavigationBackVisible(supportFragmentManager.backStackEntryCount > 1)

    }


}

interface ErrorButtonsListener {
    fun positiveListener()
    fun negativeListener()
}
