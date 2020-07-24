package ru.relabs.kurjer.presentation.host

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.drawerlayout.widget.DrawerLayout
import com.mikepenz.materialdrawer.Drawer
import ru.relabs.kurjer.presentation.base.tea.renderT
import ru.relabs.kurjer.utils.extensions.visible

object HostRenders {
    fun renderDrawer(navigationDrawer: Drawer): HostRender = renderT(
        { state -> state.settings.navDrawerLocked },
        { locked ->
            navigationDrawer.drawerLayout.setDrawerLockMode(
                if (locked) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED
            )
        }
    )

    fun renderFullScreen(window: Window): HostRender = renderT(
        { it.settings.isFullScreen },
        {
            when (it) {
                true -> {
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                    }
                }
                false -> {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                    }
                }
            }
        }
    )

    fun renderLoader(view: View): HostRender = renderT(
        { it.loaders > 0 },
        { view.visible = it }
    )
}