package com.uces.cervecerias.util

import android.net.Uri

/**
 * Arma la URL del logo de una cerveceria.
 *
 * Open Brewery DB no publica imagenes, pero si el sitio web de cada local. El
 * favicon de ese sitio es, en la practica, su logo, y se obtiene del servicio
 * publico de iconos de Google.
 *
 * Devuelve null cuando la cerveceria no tiene sitio cargado, que es alrededor
 * del 14 por ciento de los casos; en esa situacion la vista muestra la inicial.
 */
object LogoUrl {

    private const val SIZE = 128

    fun forWebsite(websiteUrl: String?): String? {
        if (websiteUrl.isNullOrBlank()) return null

        val host = runCatching { Uri.parse(websiteUrl).host }.getOrNull()
            ?: return null

        val domain = host.removePrefix("www.")
        if (domain.isBlank()) return null

        return "https://www.google.com/s2/favicons?domain=$domain&sz=$SIZE"
    }
}
