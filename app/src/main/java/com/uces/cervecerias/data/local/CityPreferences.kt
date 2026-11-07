package com.uces.cervecerias.data.local

import android.content.Context

/**
 * Guarda la ciudad elegida por el usuario en SharedPreferences.
 *
 * Es un dato simple de tipo clave-valor y no justifica una tabla: para eso
 * estan las preferencias compartidas.
 */
class CityPreferences(context: Context) {

    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** Ciudad guardada, o null si el usuario todavia no eligio ninguna. */
    var city: String?
        get() = preferences.getString(KEY_CITY, null)
        set(value) = preferences.edit().putString(KEY_CITY, value).apply()

    fun clear() = preferences.edit().remove(KEY_CITY).apply()

    companion object {
        private const val PREFERENCES_NAME = "brewery_preferences"
        private const val KEY_CITY = "selected_city"
    }
}
