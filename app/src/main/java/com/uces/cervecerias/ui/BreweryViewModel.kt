package com.uces.cervecerias.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.uces.cervecerias.R
import com.uces.cervecerias.data.BreweryRepository
import com.uces.cervecerias.data.model.Brewery
import com.uces.cervecerias.data.model.SavedStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Logica de la aplicacion.
 *
 * Extiende AndroidViewModel porque el repositorio necesita un Context para
 * abrir la base de datos, y Application es el unico contexto que sobrevive a
 * los cambios de configuracion sin provocar fugas de memoria.
 *
 * Las operaciones de datos se delegan al repositorio: aca solo se decide que
 * pedir y en que estado queda la pantalla.
 */
class BreweryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BreweryRepository(application)

    private val _directoryState = MutableLiveData<UiState<List<Brewery>>>()
    val directoryState: LiveData<UiState<List<Brewery>>> = _directoryState

    private val _savedBreweries = MutableLiveData<List<Brewery>>()
    val savedBreweries: LiveData<List<Brewery>> = _savedBreweries

    private val _suggestions = MutableLiveData<List<Brewery>>()
    val suggestions: LiveData<List<Brewery>> = _suggestions

    /** Filtro de tipo activo, o null cuando se muestran todos. */
    private val _activeType = MutableLiveData<String?>(null)
    val activeType: LiveData<String?> = _activeType

    init {
        loadInitialDirectory()
        loadSavedBreweries()
    }

    // ------------------------------------------------------------ listado

    /**
     * Primera carga. Si el usuario ya eligio una ciudad se abre ahi, y si no
     * se muestra el listado general para que la pantalla nunca arranque vacia.
     */
    fun loadInitialDirectory() {
        val city = repository.selectedCity
        if (city.isNullOrBlank()) loadAll() else searchByCity(city)
    }

    fun loadAll() = runRequest {
        _activeType.postValue(null)
        repository.fetchBreweries()
    }

    fun searchByCity(city: String) = runRequest(
        emptyMessage = getString(R.string.empty_city, city)
    ) {
        repository.selectedCity = city
        repository.fetchBreweriesByCity(city)
    }

    fun searchByName(name: String) = runRequest(
        emptyMessage = getString(R.string.empty_name, name)
    ) {
        repository.fetchBreweriesByName(name)
    }

    fun filterByType(type: String?) = runRequest {
        _activeType.postValue(type)
        if (type == null) repository.fetchBreweries()
        else repository.fetchBreweriesByType(type)
    }

    /** Sugerencias del buscador. Un fallo aca no merece molestar al usuario. */
    fun requestSuggestions(query: String) {
        if (query.length < MIN_QUERY_LENGTH) {
            _suggestions.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { repository.autocomplete(query) }
            _suggestions.postValue(result.getOrDefault(emptyList()))
        }
    }

    /**
     * Ejecuta una peticion y traduce el resultado a un estado de pantalla.
     *
     * Concentrar aca el try/catch evita repetir el mismo bloque en cada
     * busqueda y garantiza que ninguna se olvide de apagar el indicador de
     * carga o de contemplar el error.
     */
    private fun runRequest(
        emptyMessage: String = getString(R.string.empty_generic),
        request: suspend () -> List<Brewery>
    ) {
        _directoryState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val breweries = request()
                _directoryState.postValue(
                    if (breweries.isEmpty()) UiState.Empty(emptyMessage)
                    else UiState.Success(breweries)
                )
            } catch (exception: IOException) {
                Log.w(TAG, "Fallo de red al consultar la API", exception)
                // Sin conexion: se muestra lo guardado en vez de una pantalla
                // en blanco, que es la forma mas rapida de perder al usuario.
                _directoryState.postValue(
                    UiState.Error(
                        getString(R.string.error_network),
                        repository.getSavedBreweries()
                    )
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Error inesperado al consultar la API", exception)
                _directoryState.postValue(
                    UiState.Error(
                        getString(R.string.error_generic),
                        repository.getSavedBreweries()
                    )
                )
            } finally {
                // Las guardadas pueden haber cambiado de estado en el detalle.
                loadSavedBreweries()
            }
        }
    }

    // ---------------------------------------------------------- guardadas

    fun loadSavedBreweries() {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = runCatching { repository.getSavedBreweries() }
                .getOrDefault(emptyList())
            _savedBreweries.postValue(saved)
        }
    }

    /**
     * Marca una cerveceria con un estado. Si ya estaba marcada con ese mismo
     * estado la quita, de modo que el mismo gesto sirve para guardar y para
     * desguardar.
     */
    fun toggleSaved(brewery: Brewery, status: SavedStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getSavedStatus(brewery.id)

            if (current == status) {
                repository.deleteBrewery(brewery.id)
            } else {
                repository.saveBrewery(brewery, status)
            }

            refreshStatusInDirectory(brewery.id)
            loadSavedBreweries()
        }
    }

    fun removeSaved(brewery: Brewery) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBrewery(brewery.id)
            refreshStatusInDirectory(brewery.id)
            loadSavedBreweries()
        }
    }

    /**
     * Reemplaza en el listado visible la cerveceria cuyo estado cambio, para
     * que la marca se actualice sin volver a pedirle nada a la API.
     */
    private suspend fun refreshStatusInDirectory(breweryId: String) {
        val state = _directoryState.value
        if (state !is UiState.Success) return

        val newStatus = repository.getSavedStatus(breweryId)
        val updated = state.data.map { brewery ->
            if (brewery.id == breweryId) brewery.copy(savedStatus = newStatus)
            else brewery
        }

        withContext(Dispatchers.Main) {
            _directoryState.value = UiState.Success(updated)
        }
    }

    // ------------------------------------------------------- preferencias

    val selectedCity: String?
        get() = repository.selectedCity

    fun clearSelectedCity() {
        repository.selectedCity = null
    }

    private fun getString(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)

    companion object {
        /** Debajo de tres letras el autocompletado devuelve demasiado ruido. */
        private const val TAG = "BreweryViewModel"

        /** Debajo de tres letras el autocompletado devuelve demasiado ruido. */
        private const val MIN_QUERY_LENGTH = 3
    }
}
