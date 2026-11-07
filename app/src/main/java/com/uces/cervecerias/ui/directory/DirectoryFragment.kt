package com.uces.cervecerias.ui.directory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.uces.cervecerias.R
import com.uces.cervecerias.data.model.Brewery
import com.uces.cervecerias.data.model.SavedStatus
import com.uces.cervecerias.databinding.FragmentDirectoryBinding
import com.uces.cervecerias.ui.BreweryViewModel
import com.uces.cervecerias.ui.UiState
import com.uces.cervecerias.ui.adapter.BreweryAdapter
import com.uces.cervecerias.ui.detail.DetailActivity

/**
 * Listado principal: buscador, filtros por tipo y RecyclerView.
 */
class DirectoryFragment : Fragment() {

    /**
     * El binding de un fragment se guarda como nullable y se libera en
     * onDestroyView: la vista muere antes que el fragment, y sostenerla
     * despues provoca fugas de memoria.
     */
    private var _binding: FragmentDirectoryBinding? = null
    private val binding get() = _binding!!

    // Compartido con la Activity para que las dos pestanas vean los mismos datos.
    private val viewModel: BreweryViewModel by activityViewModels()

    private lateinit var adapter: BreweryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDirectoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupFilters()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = BreweryAdapter(
            onClick = { brewery -> DetailActivity.start(requireContext(), brewery) },
            onToggleSave = { brewery -> onToggleSave(brewery) }
        )
        binding.recyclerBreweries.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerBreweries.adapter = adapter
    }

    /**
     * Primer toque al marcador: queda como pendiente. Segundo toque: pasa a
     * visitada. Tercero: se quita. Un mismo gesto recorre los tres estados.
     */
    private fun onToggleSave(brewery: Brewery) {
        when (brewery.savedStatus) {
            null -> viewModel.toggleSaved(brewery, SavedStatus.PENDING)
            SavedStatus.PENDING -> viewModel.toggleSaved(brewery, SavedStatus.VISITED)
            SavedStatus.VISITED -> viewModel.removeSaved(brewery)
        }
    }

    private fun setupSearch() = with(binding.inputSearch) {

        // Sugerencias del endpoint de autocompletado de la API.
        viewModel.suggestions.observe(viewLifecycleOwner) { suggestions ->
            val names = suggestions.map { it.name }
            setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )
            )
        }

        addTextChangedListener(
            onTextChanged = { text -> viewModel.requestSuggestions(text) }
        )

        setOnItemClickListener { parent, _, position, _ ->
            val name = parent.getItemAtPosition(position) as String
            viewModel.searchByName(name)
            clearFocus()
        }

        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitSearch(text.toString())
                true
            } else {
                false
            }
        }
    }

    /**
     * El buscador acepta ciudad o nombre. Se prueba primero por ciudad, que es
     * el caso frecuente, y el ViewModel resuelve el vacio si no hay resultados.
     */
    private fun submitSearch(query: String) {
        val term = query.trim()
        if (term.isEmpty()) {
            viewModel.loadAll()
            return
        }
        viewModel.searchByCity(term)
        binding.inputSearch.clearFocus()
    }

    private fun setupFilters() = with(binding) {
        chipAll.setOnClickListener { viewModel.filterByType(null) }
        chipMicro.setOnClickListener { viewModel.filterByType("micro") }
        chipBrewpub.setOnClickListener { viewModel.filterByType("brewpub") }
        chipRegional.setOnClickListener { viewModel.filterByType("regional") }
    }

    /**
     * Una sola fuente de verdad para lo que se ve en pantalla: el estado del
     * ViewModel decide que mostrar y que ocultar, sin estados intermedios.
     */
    private fun observeViewModel() {
        viewModel.directoryState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> render(loading = true)

                is UiState.Success -> {
                    render(list = true)
                    adapter.submitList(state.data)
                    binding.textSubtitle.text = resources.getQuantityString(
                        R.plurals.result_count, state.data.size, state.data.size
                    )
                }

                is UiState.Empty -> {
                    adapter.submitList(emptyList())
                    render(message = state.message)
                }

                is UiState.Error -> {
                    // Aunque la API falle, se muestra lo guardado localmente.
                    @Suppress("UNCHECKED_CAST")
                    val fallback = state.fallbackData as List<Brewery>
                    adapter.submitList(fallback)
                    render(
                        list = fallback.isNotEmpty(),
                        message = state.message,
                        retry = true
                    )
                }
            }
        }
    }

    /**
     * Un unico lugar decide la visibilidad de cada bloque de la pantalla.
     *
     * El aviso tiene dos formas segun haya o no filas para mostrar: franja
     * angosta arriba de la lista, o cartel centrado donde iria la lista.
     */
    private fun render(
        loading: Boolean = false,
        list: Boolean = false,
        message: String? = null,
        retry: Boolean = false
    ) = with(binding) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        recyclerBreweries.visibility = if (list) View.VISIBLE else View.INVISIBLE

        val banner = message != null && list
        val fullScreen = message != null && !list

        errorBanner.visibility = if (banner) View.VISIBLE else View.GONE
        textBannerMessage.text = if (banner) message else ""
        buttonBannerRetry.visibility = if (banner && retry) View.VISIBLE else View.GONE
        buttonBannerRetry.setOnClickListener { viewModel.loadInitialDirectory() }

        stateContainer.visibility = if (fullScreen) View.VISIBLE else View.GONE
        textStateMessage.text = if (fullScreen) message.orEmpty() else ""
        buttonRetry.visibility = if (fullScreen && retry) View.VISIBLE else View.GONE
        buttonRetry.setOnClickListener { viewModel.loadInitialDirectory() }
    }

    override fun onResume() {
        super.onResume()
        // Al volver del detalle, el estado guardado puede haber cambiado.
        viewModel.loadSavedBreweries()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/** Atajo para no repetir el TextWatcher completo cuando solo interesa un evento. */
private fun android.widget.TextView.addTextChangedListener(
    onTextChanged: (String) -> Unit
) {
    addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
            onTextChanged(s?.toString().orEmpty())
        }
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    })
}
