package com.uces.cervecerias.data

import android.content.Context
import com.uces.cervecerias.data.local.BreweryDatabaseHelper
import com.uces.cervecerias.data.local.CityPreferences
import com.uces.cervecerias.data.model.Brewery
import com.uces.cervecerias.data.model.SavedStatus
import com.uces.cervecerias.data.remote.BreweryApiService
import com.uces.cervecerias.data.remote.RetrofitClient

/**
 * Unica puerta de entrada a los datos para el resto de la aplicacion.
 *
 * El ViewModel no sabe si una cerveceria viene de la API o de SQLite: le pide
 * los datos al repositorio y este decide de donde sacarlos.
 */
class BreweryRepository(context: Context) {

    private val apiService: BreweryApiService = RetrofitClient.apiService
    private val databaseHelper = BreweryDatabaseHelper(context.applicationContext)
    private val cityPreferences = CityPreferences(context.applicationContext)

    // ------------------------------------------------------------- remoto

    suspend fun fetchBreweries(): List<Brewery> =
        apiService.getBreweries().withSavedStatus()

    suspend fun fetchBreweriesByCity(city: String): List<Brewery> =
        apiService.getBreweriesByCity(city.toApiQuery()).withSavedStatus()

    suspend fun fetchBreweriesByName(name: String): List<Brewery> =
        apiService.getBreweriesByName(name.toApiQuery()).withSavedStatus()

    suspend fun fetchBreweriesByType(type: String): List<Brewery> =
        apiService.getBreweriesByType(type).withSavedStatus()

    suspend fun autocomplete(query: String): List<Brewery> =
        apiService.autocomplete(query.toApiQuery())

    /** La API espera los espacios como guion bajo: "san diego" -> "san_diego". */
    private fun String.toApiQuery(): String = trim().lowercase().replace(" ", "_")

    // -------------------------------------------------------------- local

    /** Devuelve todas las cervecerias guardadas, las mas recientes primero. */
    fun getSavedBreweries(): List<Brewery> {
        val breweries = mutableListOf<Brewery>()
        val database = databaseHelper.readableDatabase

        val cursor = database.query(
            BreweryDatabaseHelper.TABLE_NAME,
            null, null, null, null, null,
            "${BreweryDatabaseHelper.COLUMN_NAME} ASC"
        )

        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(BreweryDatabaseHelper.COLUMN_ID)
            val nameIndex = it.getColumnIndexOrThrow(BreweryDatabaseHelper.COLUMN_NAME)
            val typeIndex = it.getColumnIndexOrThrow(BreweryDatabaseHelper.COLUMN_TYPE)
            val streetIndex = it.getColumnIndexOrThrow(BreweryDatabaseHelper.COLUMN_STREET)
            val cityIndex = it.getColumnIndexOrThrow(BreweryDatabaseHelper.COLUMN_CITY)
            val stateIndex = it.getColumnIndexOrThrow(BreweryDatabaseHelper.COLUMN_STATE)
            val countryIndex = it.getColumnIndexOrThrow(BreweryDatabaseHelper.COLUMN_COUNTRY)
            val phoneIndex = it.getColumnIndexOrThrow(BreweryDatabaseHelper.COLUMN_PHONE)
            val websiteIndex = it.getColumnIndexOrThrow(BreweryDatabaseHelper.COLUMN_WEBSITE)
            val latitudeIndex = it.getColumnIndexOrThrow(BreweryDatabaseHelper.COLUMN_LATITUDE)
            val longitudeIndex = it.getColumnIndexOrThrow(BreweryDatabaseHelper.COLUMN_LONGITUDE)
            val statusIndex = it.getColumnIndexOrThrow(BreweryDatabaseHelper.COLUMN_STATUS)

            while (it.moveToNext()) {
                breweries.add(
                    Brewery(
                        id = it.getString(idIndex),
                        name = it.getString(nameIndex),
                        breweryType = it.getString(typeIndex),
                        street = it.getString(streetIndex),
                        city = it.getString(cityIndex),
                        state = it.getString(stateIndex),
                        country = it.getString(countryIndex),
                        phone = it.getString(phoneIndex),
                        websiteUrl = it.getString(websiteIndex),
                        latitude = if (it.isNull(latitudeIndex)) null else it.getDouble(latitudeIndex),
                        longitude = if (it.isNull(longitudeIndex)) null else it.getDouble(longitudeIndex),
                        savedStatus = SavedStatus.fromStorage(it.getString(statusIndex))
                    )
                )
            }
        }

        database.close()
        return breweries
    }

    /**
     * Guarda o actualiza el estado de una cerveceria.
     * Devuelve true si la operacion sobre la tabla fue exitosa.
     */
    fun saveBrewery(brewery: Brewery, status: SavedStatus): Boolean {
        val database = databaseHelper.writableDatabase
        val values = databaseHelper.toContentValues(brewery, status)

        // CONFLICT_REPLACE evita tener que consultar antes si ya existe.
        val rowId = database.insertWithOnConflict(
            BreweryDatabaseHelper.TABLE_NAME,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )

        database.close()
        return rowId != -1L
    }

    /** Elimina una cerveceria guardada. Devuelve true si borro alguna fila. */
    fun deleteBrewery(breweryId: String): Boolean {
        val database = databaseHelper.writableDatabase

        val deletedRows = database.delete(
            BreweryDatabaseHelper.TABLE_NAME,
            "${BreweryDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(breweryId)
        )

        database.close()
        return deletedRows > 0
    }

    /** Estado con el que esta guardada una cerveceria, o null si no lo esta. */
    fun getSavedStatus(breweryId: String): SavedStatus? {
        val database = databaseHelper.readableDatabase

        val cursor = database.query(
            BreweryDatabaseHelper.TABLE_NAME,
            arrayOf(BreweryDatabaseHelper.COLUMN_STATUS),
            "${BreweryDatabaseHelper.COLUMN_ID} = ?",
            arrayOf(breweryId),
            null, null, null
        )

        val status = cursor.use {
            if (it.moveToFirst()) SavedStatus.fromStorage(it.getString(0)) else null
        }

        database.close()
        return status
    }

    /**
     * Completa la lista que vino de la API con el estado guardado de cada una.
     * Se hace en una sola lectura de la tabla y no una por cerveceria.
     */
    private fun List<Brewery>.withSavedStatus(): List<Brewery> {
        val savedById = getSavedBreweries().associate { it.id to it.savedStatus }
        return onEach { it.savedStatus = savedById[it.id] }
    }

    // -------------------------------------------------------- preferencias

    var selectedCity: String?
        get() = cityPreferences.city
        set(value) {
            cityPreferences.city = value
        }
}
