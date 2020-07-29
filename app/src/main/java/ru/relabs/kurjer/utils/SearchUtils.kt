package ru.relabs.kurjer.utils

object SearchUtils {
    fun isMatches(text: String, filter: String): Boolean {
        if (filter.isEmpty()) {
            return true
        }

        val splittedFilter = filter.toLowerCase().split(" ")
        val splittedText = text.toLowerCase().split(" ")
        return splittedFilter.all { filter ->
            splittedText.any { word -> word.contains(filter) }
        }
    }
}