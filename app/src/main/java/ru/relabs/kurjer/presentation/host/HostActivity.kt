package ru.relabs.kurjer.presentation.host

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE
import com.github.terrakok.cicerone.Command
import com.github.terrakok.cicerone.NavigatorHolder
import com.github.terrakok.cicerone.androidx.AppNavigator
import com.github.terrakok.cicerone.androidx.FragmentScreen
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.holder.DimenHolder
import com.mikepenz.materialize.util.UIUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.R
import ru.relabs.kurjer.databinding.ActivityHostBinding
import ru.relabs.kurjer.databinding.NavHeaderBinding
import ru.relabs.kurjer.domain.models.AppUpdate
import ru.relabs.kurjer.domain.providers.LocationProvider
import ru.relabs.kurjer.presentation.base.fragment.AppBarSettings
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.fragment.IFragmentStyleable
import ru.relabs.kurjer.presentation.base.tea.defaultController
import ru.relabs.kurjer.presentation.base.tea.rendersCollector
import ru.relabs.kurjer.presentation.base.tea.sendMessage
import ru.relabs.kurjer.presentation.customViews.drawables.NavDrawerBackgroundDrawable
import ru.relabs.kurjer.presentation.host.featureCheckers.FeatureCheckersContainer
import ru.relabs.kurjer.presentation.host.systemWatchers.SystemWatchersContainer
import ru.relabs.kurjer.services.ReportService
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.MyExceptionHandler
import ru.relabs.kurjer.utils.extensions.hideKeyboard
import ru.relabs.kurjer.utils.extensions.showDialog
import ru.relabs.kurjer.utils.log
import java.io.File


class HostActivity : AppCompatActivity(), IFragmentHolder {

    private val supervisor = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + supervisor)
    private val controller = defaultController(HostState(), HostContext())

    private val navigationHolder: NavigatorHolder by inject()
    private val locationProvider: LocationProvider by inject()
    private lateinit var navigationDrawer: Drawer

    private val navigator = object : AppNavigator(this, R.id.fragment_container) {
        override fun setupFragmentTransaction(
            screen: FragmentScreen,
            fragmentTransaction: FragmentTransaction,
            currentFragment: Fragment?,
            nextFragment: Fragment
        ) {
            fragmentTransaction.setTransition(TRANSIT_FRAGMENT_FADE)
        }

        override fun applyCommands(commands: Array<out Command>) {
            hideKeyboard()
            super.applyCommands(commands)
        }
    }

    private val featureCheckersContainer = FeatureCheckersContainer(this, locationProvider, uiScope)
    private val systemWatchersContainer = SystemWatchersContainer(
        this,
        featureCheckersContainer.network,
        featureCheckersContainer.gps,
        featureCheckersContainer.mockLocation,
        featureCheckersContainer.sim
    )

    private var taskUpdateRequiredDialogShowed: Boolean = false
    private var isUpdateAppDialogShowed: Boolean = false

    override fun onFragmentAttached(fragment: Fragment) {
        when (fragment) {
            is IFragmentStyleable -> updateAppBar(
                AppBarSettings(
                    navDrawerLocked = fragment.navDrawerLocked,
                    isFullScreen = fragment.isFullScreen
                )
            )

            else -> updateAppBar(AppBarSettings())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        systemWatchersContainer.mockedLocation.getAllMockGPSApps().takeIf { it.isNotEmpty() }?.let {
            CustomLog.writeToFile("MockGPS Apps: " + it.joinToString { ", " })
        }
        val binding = ActivityHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Thread.setDefaultUncaughtExceptionHandler(MyExceptionHandler())
        controller.start(HostMessages.msgInit(savedInstanceState != null))
        uiScope.launch {
            val renders = listOfNotNull(
                HostRenders.renderDrawer(navigationDrawer),
                HostRenders.renderFullScreen(window),
                HostRenders.renderLoader(binding.loadingOverlay),
                HostRenders.renderUpdateLoading(binding.loadingOverlay, binding.pbLoading, binding.tvLoader),
            )
            launch { controller.stateFlow().collect(rendersCollector(renders)) }
            //launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
        }
        prepareNavigation()
        controller.context.errorContext.attach(window.decorView.rootView)
        controller.context.showUpdateDialog = ::showUpdateDialog
        controller.context.showErrorDialog = ::showErrorDialog
        controller.context.installUpdate = ::installUpdate
        controller.context.showTaskUpdateRequired = ::showTaskUpdateRequiredDialog
        controller.context.finishApp = { finish() }

        controller.context.featureCheckersContainer = featureCheckersContainer
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        navigationHolder.setNavigator(navigator)
    }

    private fun showTaskUpdateRequiredDialog(canSkip: Boolean) {
        if (taskUpdateRequiredDialogShowed) {
            return
        }
        taskUpdateRequiredDialogShowed = true
        showDialog(
            R.string.task_update_required,
            R.string.ok to {
                uiScope.sendMessage(controller, HostMessages.msgRequiredUpdateOk())
                taskUpdateRequiredDialogShowed = false
            },
            (R.string.later to {
                uiScope.sendMessage(controller, HostMessages.msgRequiredUpdateLater())
                taskUpdateRequiredDialogShowed = false
            }).takeIf { canSkip }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        featureCheckersContainer.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_INSTALL_PACKAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (resultCode == Activity.RESULT_OK && packageManager.canRequestPackageInstalls()) {
                uiScope.sendMessage(controller, HostMessages.msgRequestUpdates())
            } else {
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", packageName))),
                    REQUEST_CODE_INSTALL_PACKAGE
                )
            }
        }
    }

    private fun showErrorDialog(stringResource: Int) {
        showDialog(
            stringResource,
            R.string.ok to {}
        )
    }

    private fun installUpdate(updateFile: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setDataAndType(
                    FileProvider.getUriForFile(this@HostActivity, "com.relabs.kurjer.file_provider", updateFile),
                    "application/vnd.android.package-archive"
                )
            } else {
                setDataAndType(Uri.fromFile(updateFile), "application/vnd.android.package-archive")
            }
        }
        startActivity(intent)
    }

    private fun showUpdateDialog(appUpdate: AppUpdate): Boolean {
        if (isUpdateAppDialogShowed) {
            return true
        }
        if (appUpdate.version > BuildConfig.VERSION_CODE) {
            isUpdateAppDialogShowed = true

            showDialog(
                R.string.update_new_available,

                R.string.update_install to {
                    isUpdateAppDialogShowed = false
                    checkUpdateRequirements(appUpdate.url)
                    uiScope.sendMessage(controller, HostMessages.msgUpdateDialogShowed(false))
                },

                (R.string.update_later to {
                    isUpdateAppDialogShowed = false
                    uiScope.sendMessage(
                        controller,
                        HostMessages.msgUpdateDialogShowed(false)
                    )
                }).takeIf { !appUpdate.isRequired }
            )
            return true
        }
        return false
    }

    private fun checkUpdateRequirements(url: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", packageName))),
                    REQUEST_CODE_INSTALL_PACKAGE
                )
            } else {
                uiScope.sendMessage(controller, HostMessages.msgStartUpdateLoading(url))
            }
        } else {
            uiScope.sendMessage(controller, HostMessages.msgStartUpdateLoading(url))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        featureCheckersContainer.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
        supervisor.cancelChildren()
        controller.context.errorContext.detach()
        controller.context.showUpdateDialog = { false }
        controller.context.showErrorDialog = {}
        controller.context.installUpdate = {}
        controller.context.showTaskUpdateRequired = {}
        controller.context.finishApp = {}
        navigationHolder.removeNavigator()
        featureCheckersContainer.onDestroy()
        systemWatchersContainer.onDestroy()
    }

    private fun prepareNavigation() {
        bindBackstackListener()

        navigationDrawer = with(DrawerBuilder()) {
            withActivity(this@HostActivity)

            withSliderBackgroundDrawable(NavDrawerBackgroundDrawable(resources))
            withTranslucentStatusBar(false)
            withDisplayBelowStatusBar(true)
            withActionBarDrawerToggle(true)
            withHeaderPadding(false)
            withHeaderDivider(false)

            withHeader(inflateNavigationHeader())

            withHeaderHeight(
                DimenHolder.fromPixel(
                    resources.getDimensionPixelSize(R.dimen.navigation_header_height) + UIUtils.getStatusBarHeight(
                        this@HostActivity
                    )
                )
            )
            build()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindBackstackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
                onFragmentChanged(it)
            }
        }
    }

    private fun inflateNavigationHeader(): View {
        val header = LayoutInflater.from(this).inflate(R.layout.nav_header, null, false)
        val binding = NavHeaderBinding.bind(header)
        binding.textContainer.setPadding(0, UIUtils.getStatusBarHeight(this), 0, 0)
        return header
    }

    override fun onResume() {
        super.onResume()
        systemWatchersContainer.onResume()
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        onFragmentChanged(currentFragment)
        navigationHolder.setNavigator(navigator)
        uiScope.sendMessage(controller, HostMessages.msgResume())
        ReportService.isAppPaused = false

        uiScope.launch(Dispatchers.IO) {
            locationProvider.updatesChannel().apply {
                receiveCatching().getOrNull()
                try {
                    if (isActive) {
                        cancel()
                    }
                } catch (e: Exception) {
                    e.log()
                }
            }
        }


        if (!ReportService.isRunning) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            var attempts = 0
            uiScope.launch(Dispatchers.IO) {
                while (true) {
                    val process = activityManager?.runningAppProcesses?.firstOrNull()
                    if (process != null && process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        startService(Intent(this@HostActivity, ReportService::class.java))
                        break
                    } else if (attempts >= 10) {
                        FirebaseCrashlytics.getInstance()
                            .recordException(RuntimeException("App resumed but importance lower than foreground"))
                        break
                    } else if (activityManager == null) {
                        FirebaseCrashlytics.getInstance().recordException(RuntimeException("App resumed but activityManager is null"))
                        break
                    } else {
                        delay(200)
                        attempts++
                    }
                }
            }
        }
    }

    override fun onPause() {
        navigationHolder.removeNavigator()
        super.onPause()
        systemWatchersContainer.onPause()
        uiScope.sendMessage(controller, HostMessages.msgPause())
        ReportService.isAppPaused = true
    }

    override fun onBackPressed() {
        if (navigationDrawer.isDrawerOpen) {
            navigationDrawer.closeDrawer()
        } else {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if ((currentFragment as? BaseFragment)?.interceptBackPressed() != true) {
                super.onBackPressed()
            }
        }
    }


    private fun onFragmentChanged(fragment: Fragment?) {
        hideKeyboard()
    }

    fun updateAppBar(settings: AppBarSettings) {
        uiScope.sendMessage(controller, HostMessages.msgUpdateAppBar(settings))
    }

    companion object {
        const val REQUEST_CODE_INSTALL_PACKAGE = 997

        fun getIntent(parentContext: Context) = Intent(parentContext, HostActivity::class.java)
    }
}
