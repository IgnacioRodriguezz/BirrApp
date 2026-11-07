package com.uces.cervecerias.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.uces.cervecerias.data.model.Brewery
import com.uces.cervecerias.data.model.SavedStatus

/**
 * Intermediario entre la base SQLite que vive en el dispositivo y el codigo.
 *
 * Se encarga de crear la tabla y de abrir la conexion. Las operaciones sobre
 * los datos viven en el repositorio, no aca.
 */
class BreweryDatabaseHelper(
    context: Context
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Esta tabla guarda solo cervecerias marcadas por el usuario, que se
        // pueden volver a marcar. No hace falta migrar: se recrea.
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    /**
     * Convierte una cerveceria en el paquete de valores que espera SQLite.
     * Vive aca porque es la unica clase que conoce los nombres de las columnas.
     */
    fun toContentValues(brewery: Brewery, status: SavedStatus): ContentValues =
        ContentValues().apply {
            put(COLUMN_ID, brewery.id)
            put(COLUMN_NAME, brewery.name)
            put(COLUMN_TYPE, brewery.breweryType)
            put(COLUMN_STREET, brewery.street)
            put(COLUMN_CITY, brewery.city)
            put(COLUMN_STATE, brewery.state)
            put(COLUMN_COUNTRY, brewery.country)
            put(COLUMN_PHONE, brewery.phone)
            put(COLUMN_WEBSITE, brewery.websiteUrl)
            put(COLUMN_LATITUDE, brewery.latitude)
            put(COLUMN_LONGITUDE, brewery.longitude)
            put(COLUMN_STATUS, status.storageValue)
        }

    companion object {

        // El sufijo .db deja claro que el archivo es una base de datos.
        private const val DATABASE_NAME = "breweries.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NAME = "saved_breweries"

        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_TYPE = "brewery_type"
        const val COLUMN_STREET = "street"
        const val COLUMN_CITY = "city"
        const val COLUMN_STATE = "state"
        const val COLUMN_COUNTRY = "country"
        const val COLUMN_PHONE = "phone"
        const val COLUMN_WEBSITE = "website_url"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_STATUS = "saved_status"

        /**
         * El id es el que devuelve la API, asi que es TEXT y no autoincremental:
         * de ese modo la clave primaria evita duplicados por si sola.
         */
        private const val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_TYPE TEXT,
                $COLUMN_STREET TEXT,
                $COLUMN_CITY TEXT,
                $COLUMN_STATE TEXT,
                $COLUMN_COUNTRY TEXT,
                $COLUMN_PHONE TEXT,
                $COLUMN_WEBSITE TEXT,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_STATUS TEXT NOT NULL
            )
        """
    }
}
