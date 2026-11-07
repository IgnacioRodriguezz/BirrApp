package com.uces.cervecerias.data.remote

import com.uces.cervecerias.data.model.Brewery
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Contrato con la API de Open Brewery DB.
 *
 * Cada funcion declara un endpoint. Todas son suspend porque son llamadas de
 * red y tienen que ejecutarse fuera del hilo principal.
 *
 * Documentacion: https://www.openbrewerydb.org/documentation
 */
interface BreweryApiService {

    /** Listado general, con paginado. */
    @GET("breweries")
    suspend fun getBreweries(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = PAGE_SIZE
    ): List<Brewery>

    /** Filtra por ciudad. La API espera los espacios como guion bajo. */
    @GET("breweries")
    suspend fun getBreweriesByCity(
        @Query("by_city") city: String,
        @Query("per_page") perPage: Int = PAGE_SIZE
    ): List<Brewery>

    /** Filtra por nombre, con coincidencia parcial. */
    @GET("breweries")
    suspend fun getBreweriesByName(
        @Query("by_name") name: String,
        @Query("per_page") perPage: Int = PAGE_SIZE
    ): List<Brewery>

    /** Filtra por tipo de establecimiento: micro, brewpub, regional, large. */
    @GET("breweries")
    suspend fun getBreweriesByType(
        @Query("by_type") type: String,
        @Query("per_page") perPage: Int = PAGE_SIZE
    ): List<Brewery>

    /**
     * Endpoint de sugerencias. Devuelve hasta 15 coincidencias y se usa para
     * el autocompletado del buscador.
     */
    @GET("breweries/autocomplete")
    suspend fun autocomplete(@Query("query") query: String): List<Brewery>

    companion object {
        /**
         * La API limita la cantidad de resultados por pagina. Pedir todo de una
         * no es performante y puede terminar en timeout.
         */
        const val PAGE_SIZE = 50
    }
}
