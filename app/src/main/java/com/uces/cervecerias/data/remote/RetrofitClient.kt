package com.uces.cervecerias.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Punto unico de acceso a la API.
 *
 * Se declara como object y no como class: aplica el patron Singleton, porque
 * abrir una conexion distinta cada vez que se necesita la API seria un
 * desperdicio de recursos.
 *
 * Las dos propiedades son by lazy, asi se construyen recien la primera vez
 * que alguien las usa y no al arrancar la aplicacion.
 */
object RetrofitClient {

    private const val BASE_URL = "https://api.openbrewerydb.org/v1/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /** Implementacion del contrato, generada por Retrofit. */
    val apiService: BreweryApiService by lazy {
        retrofit.create(BreweryApiService::class.java)
    }
}
