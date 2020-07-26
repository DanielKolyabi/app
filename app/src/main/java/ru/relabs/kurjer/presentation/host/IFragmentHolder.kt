package ru.relabs.kurjer.presentation.host

import androidx.fragment.app.Fragment

/**
 * Created by Daniil Kurchanov on 25.12.2019.
 */
interface IFragmentHolder{
    fun onFragmentAttached(fragment: Fragment)
}