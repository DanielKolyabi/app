package ru.relabs.kurjer.presentation.host

import android.app.Activity
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
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.holder.DimenHolder
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.mikepenz.materialize.util.UIUtils
import kotlinx.android.synthetic.main.activity_host.*
import kotlinx.android.synthetic.main.nav_header.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.AppUpdate
import ru.relabs.kurjer.presentation.RootScreen
import ru.relabs.kurjer.presentation.base.fragment.AppBarSettings
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.fragment.IFragmentStyleable
import ru.relabs.kurjer.presentation.base.tea.defaultController
import ru.relabs.kurjer.presentation.base.tea.rendersCollector
import ru.relabs.kurjer.presentation.base.tea.sendMessage
import ru.relabs.kurjer.presentation.customViews.drawables.NavDrawerBackgroundDrawable
import ru.relabs.kurjer.presentation.tasks.TasksFragment
import ru.relabs.kurjer.utils.*
import ru.relabs.kurjer.utils.extensions.hideKeyboard
import ru.relabs.kurjer.utils.extensions.showDialog
import ru.relabs.kurjer.utils.extensions.showSnackbar
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.Router
import java.io.File
import java.io.FileNotFoundException


class HostActivity : AppCompatActivity(), IFragmentHolder {

    private val supervisor = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + supervisor)
    private val controller = defaultController(HostState(), HostContext())

    private val navigationHolder: NavigatorHolder by inject()
    private val router: Router by inject()
    private lateinit var navigationDrawer: Drawer

    private val navigator = CiceroneNavigator(this)

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host)

        Thread.setDefaultUncaughtExceptionHandler(MyExceptionHandler())
        controller.start(HostMessages.msgInit(savedInstanceState != null))
        uiScope.launch {
            val renders = listOf(
                HostRenders.renderDrawer(navigationDrawer),
                HostRenders.renderFullScreen(window),
                HostRenders.renderLoader(loading_overlay),
                HostRenders.renderUpdateLoading(loading_overlay, pb_loading, tv_loader)
            )
            launch { controller.stateFlow().collect(rendersCollector(renders)) }
            //launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
        }
        prepareNavigation()
        controller.context.errorContext.attach(window.decorView.rootView)
        controller.context.copyToClipboard = ::copyToClipboard
        controller.context.showUpdateDialog = ::showUpdateDialog
        controller.context.showErrorDialog = ::showErrorDialog
        controller.context.installUpdate = ::installUpdate
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
        if (appUpdate.version > BuildConfig.VERSION_CODE) {
            showDialog(
                R.string.update_new_available,

                R.string.update_install to {
                    checkUpdateRequirements(appUpdate.url)
                    uiScope.sendMessage(controller, HostMessages.msgUpdateDialogShowed(false))
                },

                (R.string.update_later to {
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

    private fun copyToClipboard(text: String) {
        when (val r = ClipboardHelper.copyToClipboard(this, text)) {
            is Right ->
                showSnackbar(
                    resources.getString(R.string.copied_to_clipboard),
                    resources.getString(R.string.send) to { sendDeviceUUID(text) }
                )
            is Left ->
                showSnackbar(resources.getString(R.string.unknown_runtime_error))
        }
    }

    private fun sendDeviceUUID(text: String) {
        //TODO: Implement share text feature
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
        supervisor.cancelChildren()
        controller.context.errorContext.detach()
        controller.context.showUpdateDialog = { false }
        controller.context.showErrorDialog = {}
        controller.context.installUpdate = {}
        navigationHolder.removeNavigator()
    }

    private fun prepareNavigation() {
        navigationHolder.setNavigator(navigator)
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

            addDrawerItems(*buildDrawerItems())
            withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                override fun onItemClick(
                    view: View?,
                    position: Int,
                    drawerItem: IDrawerItem<*>
                ): Boolean {
                    return when (drawerItem.identifier) {
                        NAVIGATION_TASKS -> navigateTasks()
                        NAVIGATION_CRASH -> sendCrashLog()
                        NAVIGATION_UUID -> copyDeviceId()
                        NAVIGATION_LOGOUT -> logout()
                        else -> true
                    }
                }
            })

            build()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun copyDeviceId(): Boolean {
        uiScope.sendMessage(controller, HostMessages.msgCopyDeviceUUID())
        return false
    }

    private fun sendCrashLog(): Boolean {
        when (val r = CustomLog.share(this)) {
            is Left -> when (val e = r.value) {
                is FileNotFoundException -> showSnackbar(resources.getString(R.string.crash_log_not_found))
                else -> showSnackbar(resources.getString(R.string.unknown_runtime_error))
            }
        }
        return false
    }

    private fun bindBackstackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
                onFragmentChanged(it)
            }
        }
    }

    private fun logout(): Boolean {
        uiScope.sendMessage(controller, HostMessages.msgLogout())
        return false
    }

    private fun navigateTasks(): Boolean {
        router.newRootScreen(RootScreen.Tasks)
        return false
    }

    private fun inflateNavigationHeader(): View {
        val header = LayoutInflater.from(this).inflate(R.layout.nav_header, null, false)
        header.text_container.setPadding(0, UIUtils.getStatusBarHeight(this), 0, 0)
        return header
    }

    private fun buildDrawerItems(): Array<IDrawerItem<*>> {
        return arrayOf(
            buildDrawerItem(
                NAVIGATION_TASKS,
                R.string.menu_tasks
            ),
            buildDrawerItem(
                NAVIGATION_CRASH,
                R.string.menu_info
            ),
            buildDrawerItem(
                NAVIGATION_UUID,
                R.string.menu_uuid
            ),
            buildDrawerItem(
                NAVIGATION_LOGOUT,
                R.string.menu_logout
            )
        )
    }

    private fun buildDrawerItem(id: Long, stringResId: Int): IDrawerItem<*> {
        return MenuDrawerItem(0)
            .withIdentifier(id)
            .withName(stringResId)
    }

    override fun onResume() {
        super.onResume()
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        onFragmentChanged(currentFragment)
        navigationHolder.setNavigator(navigator)
        uiScope.sendMessage(controller, HostMessages.msgResume())
    }

    override fun onPause() {
        super.onPause()
        navigationHolder.removeNavigator()
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


    fun onFragmentChanged(fragment: Fragment?) {
        hideKeyboard()

        when (fragment) {
            is TasksFragment -> setDrawerSelectedItem(NAVIGATION_TASKS)
        }
    }

    fun updateAppBar(settings: AppBarSettings) {
        uiScope.sendMessage(controller, HostMessages.msgUpdateAppBar(settings))
    }

    private fun setDrawerSelectedItem(id: Long) {
        navigationDrawer.setSelection(id, false)
    }

    fun changeNavigationDrawerState() {
        if (navigationDrawer.isDrawerOpen) {
            navigationDrawer.closeDrawer()
        } else {
            navigationDrawer.openDrawer()
        }
    }

    companion object {
        const val REQUEST_CODE_INSTALL_PACKAGE = 997

        const val NAVIGATION_TASKS = 1L
        const val NAVIGATION_CRASH = 2L
        const val NAVIGATION_UUID = 3L
        const val NAVIGATION_LOGOUT = 4L

        fun getIntent(parentContext: Context) = Intent(parentContext, HostActivity::class.java)
    }
}
