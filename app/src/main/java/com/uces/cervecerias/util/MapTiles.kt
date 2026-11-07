package com.uces.cervecerias.util

import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.tan

/**
 * Traduce coordenadas geograficas a tiles de OpenStreetMap.
 *
 * La API de cervecerias devuelve latitud y longitud pero ninguna imagen, asi
 * que el mapa se arma pidiendo las imagenes de tile que OSM publica. Cada tile
 * es un cuadrado de 256 px y la grilla es fija, de manera que un punto rara vez
 * cae en el centro de su tile: por eso se piden cuatro y se recorta alrededor.
 *
 * Formula estandar de Slippy Map:
 * https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
 */
object MapTiles {

    const val ZOOM = 16
    const val TILE_SIZE = 256

    /** Posicion continua del punto dentro de la grilla, en unidades de tile. */
    private fun tileXFor(longitude: Double): Double =
        (longitude + 180.0) / 360.0 * (1 shl ZOOM)

    private fun tileYFor(latitude: Double): Double {
        val radians = Math.toRadians(latitude)
        return (1.0 - asinh(tan(radians)) / Math.PI) / 2.0 * (1 shl ZOOM)
    }

    /**
     * Bloque de 2x2 tiles elegido de modo que el punto quede lo mas cerca
     * posible del centro, junto con su posicion exacta dentro del bloque.
     */
    fun blockFor(latitude: Double, longitude: Double): MapBlock {
        val exactX = tileXFor(longitude)
        val exactY = tileYFor(latitude)

        val tileX = floor(exactX).toInt()
        val tileY = floor(exactY).toInt()

        // Si el punto cae en la mitad izquierda del tile, conviene incluir el
        // tile anterior para que quede centrado; lo mismo en vertical.
        val originX = if (exactX - tileX >= 0.5) tileX else tileX - 1
        val originY = if (exactY - tileY >= 0.5) tileY else tileY - 1

        return MapBlock(
            originX = originX,
            originY = originY,
            offsetXRatio = ((exactX - originX) / 2.0).toFloat(),
            offsetYRatio = ((exactY - originY) / 2.0).toFloat()
        )
    }

    fun tileUrl(x: Int, y: Int): String =
        "https://tile.openstreetmap.org/$ZOOM/$x/$y.png"
}

/**
 * Bloque de 2x2 tiles.
 *
 * Los ratios indican donde cae el punto dentro del bloque completo, expresados
 * entre 0 y 1, para poder ubicar el marcador sin volver a calcular nada.
 */
data class MapBlock(
    val originX: Int,
    val originY: Int,
    val offsetXRatio: Float,
    val offsetYRatio: Float
) {
    val tileUrls: List<String>
        get() = listOf(
            MapTiles.tileUrl(originX, originY),
            MapTiles.tileUrl(originX + 1, originY),
            MapTiles.tileUrl(originX, originY + 1),
            MapTiles.tileUrl(originX + 1, originY + 1)
        )
}
