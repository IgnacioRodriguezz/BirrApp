package com.uces.cervecerias.ui

/**
 * Estado de una pantalla que depende de datos remotos.
 *
 * Se modela como sealed class para que las vistas tengan que contemplar todos
 * los casos: mientras el estado sea exhaustivo, no existe la posibilidad de
 * olvidarse de dibujar el error o el vacio.
 */
sealed class UiState<out T> {

    data object Loading : UiState<Nothing>()

    data class Success<T>(val data: T) : UiState<T>()

    /** Vacio no es un error: la busqueda funciono pero no hubo coincidencias. */
    data class Empty(val message: String) : UiState<Nothing>()

    /**
     * @param fallbackData datos locales que se pueden mostrar igual, para que
     *   un fallo de red no deje la pantalla en blanco.
     */
    data class Error(
        val message: String,
        val fallbackData: List<Any> = emptyList()
    ) : UiState<Nothing>()
}
