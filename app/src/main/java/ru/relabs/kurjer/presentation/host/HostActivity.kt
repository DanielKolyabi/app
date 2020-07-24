package ru.relabs.kurjer.presentation.host

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
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
import ru.relabs.kurjer.R
import ru.relabs.kurjer.presentation.RootScreen
import ru.relabs.kurjer.presentation.base.fragment.AppBarSettings
import ru.relabs.kurjer.presentation.base.fragment.BaseFragment
import ru.relabs.kurjer.presentation.base.fragment.IFragmentStyleable
import ru.relabs.kurjer.presentation.base.tea.debugCollector
import ru.relabs.kurjer.presentation.base.tea.defaultController
import ru.relabs.kurjer.presentation.base.tea.rendersCollector
import ru.relabs.kurjer.presentation.base.tea.sendMessage
import ru.relabs.kurjer.presentation.customViews.drawables.NavDrawerBackgroundDrawable
import ru.relabs.kurjer.presentation.tasks.TasksFragment
import ru.relabs.kurjer.utils.*
import ru.relabs.kurjer.utils.extensions.hideKeyboard
import ru.relabs.kurjer.utils.extensions.showSnackbar
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.Router
import ru.terrakok.cicerone.android.support.SupportAppNavigator
import ru.terrakok.cicerone.android.support.SupportAppScreen
import ru.terrakok.cicerone.commands.Command
import java.io.FileNotFoundException


class HostActivity : AppCompatActivity(), IFragmentHolder {

    private val supervisor = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + supervisor)
    private val controller = defaultController(HostState(), HostContext())

    private val navigationHolder: NavigatorHolder by inject()
    private val router: Router by inject()
    private lateinit var navigationDrawer: Drawer

    private val navigator =
        object : SupportAppNavigator(this, supportFragmentManager, R.id.fragment_container) {
            override fun createFragment(screen: SupportAppScreen): Fragment? {
                return super.createFragment(screen).also { onFragmentChanged(it) }
            }

            override fun setupFragmentTransaction(
                command: Command,
                currentFragment: Fragment?,
                nextFragment: Fragment?,
                fragmentTransaction: FragmentTransaction
            ) {
                super.setupFragmentTransaction(
                    command,
                    currentFragment,
                    nextFragment,
                    fragmentTransaction
                )
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
        }

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
        setContentView(R.layout.activity_host)

        controller.start(HostMessages.msgInit(savedInstanceState != null))
        uiScope.launch {
            val renders = listOf(
                HostRenders.renderDrawer(navigationDrawer),
                HostRenders.renderFullScreen(window),
                HostRenders.renderLoader(loading_overlay)
            )
            launch { controller.stateFlow().collect(rendersCollector(renders)) }
            launch { controller.stateFlow().collect(debugCollector { debug(it) }) }
        }
        prepareNavigation()
        controller.context.errorContext.attach(window.decorView.rootView)
        controller.context.copyToClipboard = ::copyToClipboard
    }

    fun copyToClipboard(text: String){
        when(val r = ClipboardHelper.copyToClipboard(this, text)){
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
        when(val r = CustomLog.share(this)){
            is Left -> when(val e = r.value){
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


    private fun onFragmentChanged(fragment: Fragment?) {
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
        const val NAVIGATION_TASKS = 1L
        const val NAVIGATION_CRASH = 2L
        const val NAVIGATION_UUID = 3L
        const val NAVIGATION_LOGOUT = 4L

        const val KEY_INTENT_INTENT_ACTION = "notification_action"

        fun getIntent(parentContext: Context) = Intent(parentContext, HostActivity::class.java)
    }
}
