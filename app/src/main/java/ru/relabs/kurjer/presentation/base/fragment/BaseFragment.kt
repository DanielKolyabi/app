package ru.relabs.kurjer.presentation.base.fragment

import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import ru.relabs.kurjer.presentation.host.IFragmentHolder
import java.lang.ref.WeakReference


abstract class BaseFragment : Fragment() {

    var exitAnimation = ExitAnimation.Default
    protected val supervisor = SupervisorJob()
    protected val uiScope = CoroutineScope(Dispatchers.Main + supervisor)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as? IFragmentHolder)?.onFragmentAttached(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        supervisor.cancelChildren()
    }

    override fun onResume() {
        super.onResume()
    }

    /**
     * Back pressed handle
     * @return Should back pressed be intercepted
     */
    open fun interceptBackPressed(): Boolean {
        return false
    }

    enum class ExitAnimation {
        Default, Left, Right
    }
}