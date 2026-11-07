package com.uces.cervecerias.data.model

/**
 * Estado con el que el usuario guarda una cerveceria.
 *
 * Se persiste en SQLite como texto para que la tabla siga siendo legible
 * al inspeccionarla desde el Device Explorer.
 */
enum class SavedStatus(val storageValue: String) : java.io.Serializable {

    VISITED("visited"),
    PENDING("pending");

    companion object {

        /** Devuelve null si el texto no corresponde a ningun estado conocido. */
        fun fromStorage(value: String?): SavedStatus? =
            entries.firstOrNull { it.storageValue == value }
    }
}
