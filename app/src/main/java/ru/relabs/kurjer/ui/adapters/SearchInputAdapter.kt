package ru.relabs.kurjer.ui.adapters

import android.content.Context
import androidx.fragment.app.FragmentManager
import android.widget.ArrayAdapter
import android.widget.Filter
import ru.relabs.kurjer.ui.fragments.SearchableFragment

/**
 * Created by ProOrange on 27.11.2018.
 */

class SearchInputAdapter(ctx: Context, resId: Int, textId: Int, val fragmentManager: FragmentManager?) : ArrayAdapter<String>(ctx, resId, textId) {

    override fun getFilter(): Filter {
        return stringFilter
    }


    val stringFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            if (constraint?.toString().orEmpty().isEmpty()) {
                return FilterResults()
            }
            val current = fragmentManager?.findFragmentByTag("fragment") as? SearchableFragment
            current ?: return FilterResults()

            val data = current.onSearchItems(constraint?.toString().orEmpty())

            return FilterResults().apply {
                count = data.size
                values = data
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            results ?: return
            val filteredList = results.values;
            if (results.count > 0) {
                clear();
                (filteredList as? List<String>)?.forEach {
                    add(it)
                }
                notifyDataSetChanged();
            }
        }
    }
}