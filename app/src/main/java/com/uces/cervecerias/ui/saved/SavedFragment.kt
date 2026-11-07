package com.uces.cervecerias.ui.saved

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uces.cervecerias.R
import com.uces.cervecerias.data.model.Brewery
import com.uces.cervecerias.data.model.SavedStatus
import com.uces.cervecerias.databinding.FragmentSavedBinding
import com.uces.cervecerias.ui.BreweryViewModel
import com.uces.cervecerias.ui.adapter.BreweryAdapter
import com.uces.cervecerias.ui.detail.DetailActivity

/**
 * Cervecerias guardadas en la base local, separadas entre visitadas y
 * pendientes. Es la unica pantalla que funciona sin conexion.
 */
class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BreweryViewModel by activityViewModels()

    private lateinit var adapter: BreweryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        viewModel.loadSavedBreweries()
    }

    private fun setupRecyclerView() {
        adapter = BreweryAdapter(
            onClick = { brewery -> DetailActivity.start(requireContext(), brewery) },
            onToggleSave = { brewery -> confirmRemove(brewery) }
        )
        binding.recyclerSaved.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSaved.adapter = adapter
    }

    /**
     * Quitar algo de la lista es destructivo y no se puede deshacer, asi que
     * se pide confirmacion antes.
     */
    private fun confirmRemove(brewery: Brewery) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_remove_title)
            .setMessage(getString(R.string.dialog_remove_message, brewery.name))
            .setPositiveButton(R.string.dialog_remove_confirm) { _, _ ->
                viewModel.removeSaved(brewery)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.savedBreweries.observe(viewLifecycleOwner) { breweries ->
            adapter.submitList(breweries)

            val isEmpty = breweries.isEmpty()
            binding.emptyContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.recyclerSaved.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.textTitle.text = getString(R.string.tab_saved)

            val visited = breweries.count { it.savedStatus == SavedStatus.VISITED }
            val pending = breweries.count { it.savedStatus == SavedStatus.PENDING }
            binding.textCounts.text =
                getString(R.string.saved_counts, visited, pending)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
