package com.tvpro.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SearchHistoryManager {
    private const val PREF_NAME = "tvpro_search_preferences"
    private const val KEY_SEARCHES = "recent_searches"
    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getRecentSearches(context: Context): List<String> {
        val json = getPrefs(context).getString(KEY_SEARCHES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addRecentSearch(context: Context, query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val current = getRecentSearches(context).toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val capped = current.take(8)
        getPrefs(context).edit().putString(KEY_SEARCHES, gson.toJson(capped)).apply()
    }

    fun removeRecentSearch(context: Context, query: String) {
        val current = getRecentSearches(context).toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        getPrefs(context).edit().putString(KEY_SEARCHES, gson.toJson(current)).apply()
    }

    fun clearHistory(context: Context) {
        getPrefs(context).edit().remove(KEY_SEARCHES).apply()
    }
}
