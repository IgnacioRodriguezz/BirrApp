package com.uces.cervecerias

import android.app.Application
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.OkHttpClient

/**
 * Configuracion que corre una sola vez, al arrancar la aplicacion.
 *
 * OpenStreetMap exige que cada cliente se identifique con un User-Agent
 * propio; a los que no lo hacen les devuelve una imagen que dice "blocked"
 * en lugar del mapa. Picasso usa OkHttp por debajo, asi que se le arma un
 * cliente con esa cabecera y se lo deja como instancia unica, para que el
 * resto de la aplicacion siga llamando a Picasso.get() como siempre.
 */
class BirrApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .build()

        Picasso.setSingletonInstance(
            Picasso.Builder(this)
                .downloader(OkHttp3Downloader(client))
                .build()
        )
    }

    companion object {
        /** Politica de uso de OSM: hay que identificar la aplicacion. */
        private const val USER_AGENT = "BirrApp/1.0 (trabajo practico UCES)"
    }
}
