package com.uces.cervecerias.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Modelo de datos de una cerveceria.
 *
 * Solo se mapean los campos de la API que la aplicacion realmente usa.
 * La API devuelve bastantes mas (origin, location, address_2, address_3),
 * pero traerlos sin necesidad solo agrega ruido al modelo.
 */
data class Brewery(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("brewery_type") val breweryType: String?,
    @SerializedName("street") val street: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("website_url") val websiteUrl: String?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("latitude") val latitude: Double?,

    /**
     * Este campo no viene de la API: lo resuelve la aplicacion cruzando el id
     * contra la base local. Arranca siempre en null, que significa "no guardada".
     */
    @Transient var savedStatus: SavedStatus? = null
) : Serializable {

    /** Texto corto para la fila del listado: "micro - San Diego". */
    val typeAndCity: String
        get() = listOfNotNull(breweryType, city).joinToString(" \u00b7 ")

    /** Direccion completa, armada con lo que haya disponible. */
    val fullAddress: String
        get() = listOfNotNull(street, city, state, country).joinToString(", ")

    val hasCoordinates: Boolean
        get() = latitude != null && longitude != null

    /** La API marca como "closed" las cervecerias que ya no operan. */
    val isClosed: Boolean
        get() = breweryType == "closed"
}
