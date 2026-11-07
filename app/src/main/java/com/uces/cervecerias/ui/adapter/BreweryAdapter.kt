package com.uces.cervecerias.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.uces.cervecerias.R
import com.uces.cervecerias.data.model.Brewery
import com.uces.cervecerias.data.model.SavedStatus
import com.uces.cervecerias.databinding.ItemBreweryBinding
import com.uces.cervecerias.util.LogoUrl

/**
 * Adaptador del listado de cervecerias.
 *
 * Es el mismo para el directorio y para las guardadas: las dos pantallas
 * muestran la misma fila, solo cambia de donde salen los datos.
 *
 * Hereda de ListAdapter en lugar de RecyclerView.Adapter para aprovechar
 * DiffUtil, que compara la lista nueva contra la anterior y redibuja solo las
 * filas que cambiaron en vez de toda la pantalla.
 *
 * @param onClick accion al tocar una fila.
 * @param onToggleSave accion al tocar el marcador.
 */
class BreweryAdapter(
    private val onClick: (Brewery) -> Unit,
    private val onToggleSave: (Brewery) -> Unit
) : ListAdapter<Brewery, BreweryAdapter.BreweryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreweryViewHolder {
        val binding = ItemBreweryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BreweryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BreweryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BreweryViewHolder(
        private val binding: ItemBreweryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(brewery: Brewery) = with(binding) {
            textName.text = brewery.name
            textMeta.text = brewery.typeAndCity
            textAddress.text = brewery.street.orEmpty()
            textAddress.visibility =
                if (brewery.street.isNullOrBlank()) View.GONE else View.VISIBLE

            bindLogo(brewery)
            bindClosedState(brewery)
            bindSavedMarker(brewery)

            root.setOnClickListener { onClick(brewery) }
            buttonSave.setOnClickListener { onToggleSave(brewery) }
        }

        /**
         * Carga el logo con Picasso. Cuando la cerveceria no tiene sitio web
         * se muestra su inicial, para que la fila no quede con un hueco.
         */
        private fun bindLogo(brewery: Brewery) = with(binding) {
            val logoUrl = LogoUrl.forWebsite(brewery.websiteUrl)

            if (logoUrl == null) {
                imageLogo.visibility = View.GONE
                textInitial.visibility = View.VISIBLE
                textInitial.text = brewery.name.take(1).uppercase()
            } else {
                imageLogo.visibility = View.VISIBLE
                textInitial.visibility = View.GONE
                // Si el favicon no existe o no responde, la fila no puede
                // quedar con un disco vacio: se cae a la inicial del nombre.
                Picasso.get()
                    .load(logoUrl)
                    .into(imageLogo, object : Callback {
                        override fun onSuccess() = Unit

                        override fun onError(e: Exception?) {
                            imageLogo.visibility = View.GONE
                            textInitial.visibility = View.VISIBLE
                            textInitial.text = brewery.name.take(1).uppercase()
                        }
                    })
            }
        }

        /** Las cervecerias cerradas se atenuan y se marcan. */
        private fun bindClosedState(brewery: Brewery) = with(binding) {
            val alpha = if (brewery.isClosed) 0.45f else 1f
            imageLogo.alpha = alpha
            textInitial.alpha = alpha
            textName.alpha = alpha
            labelClosed.visibility = if (brewery.isClosed) View.VISIBLE else View.GONE
        }

        private fun bindSavedMarker(brewery: Brewery) = with(binding) {
            val iconRes = when (brewery.savedStatus) {
                SavedStatus.VISITED -> R.drawable.ic_marker_visited
                SavedStatus.PENDING -> R.drawable.ic_marker_pending
                null -> R.drawable.ic_marker_outline
            }
            buttonSave.setImageResource(iconRes)

            val description = when (brewery.savedStatus) {
                SavedStatus.VISITED -> R.string.marker_visited
                SavedStatus.PENDING -> R.string.marker_pending
                null -> R.string.marker_none
            }
            buttonSave.contentDescription = root.context.getString(description)
        }
    }

    companion object {

        /**
         * DiffUtil necesita dos comparaciones: si dos filas representan la
         * misma cerveceria, y si ademas su contenido es identico.
         */
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Brewery>() {

            override fun areItemsTheSame(oldItem: Brewery, newItem: Brewery): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Brewery, newItem: Brewery): Boolean =
                oldItem == newItem
        }
    }
}
