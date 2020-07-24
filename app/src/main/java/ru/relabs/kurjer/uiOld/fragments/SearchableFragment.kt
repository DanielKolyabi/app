package ru.relabs.kurjer.uiOld.fragments

import android.widget.AutoCompleteTextView

/**
 * Created by ProOrange on 27.11.2018.
 */
interface SearchableFragment {
    fun onSearchItems(filter: String): List<String>
    fun onItemSelected(item: String, searchView: AutoCompleteTextView)
}