package com.uces.cervecerias.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.uces.cervecerias.R
import com.uces.cervecerias.data.model.Brewery
import com.uces.cervecerias.data.model.SavedStatus
import com.uces.cervecerias.ui.BreweryViewModel
import com.uces.cervecerias.ui.theme.CerveceriasTheme

/**
 * Contenedor de la pantalla de detalle.
 *
 * Es una ComponentActivity y no una AppCompatActivity porque todo su contenido
 * es Compose: no infla ningun layout XML.
 */
class DetailActivity : ComponentActivity() {

    private val viewModel: BreweryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val brewery = readBreweryExtra()
        if (brewery == null) {
            finish()
            return
        }

        setContent {
            CerveceriasTheme {
                val saved by viewModel.savedBreweries.observeAsState(emptyList())
                val status = saved.firstOrNull { it.id == brewery.id }?.savedStatus

                DetailScreen(
                    brewery = brewery,
                    savedStatus = status,
                    onBack = { finish() },
                    onSelectStatus = { viewModel.toggleSaved(brewery, it) },
                    onCall = { phone -> openDial(phone) },
                    onOpenWebsite = { url -> openWebsite(url) },
                    noMapMessage = getString(R.string.detail_no_map)
                )
            }
        }
    }

    /** Abre el marcador del telefono con el numero cargado. */
    private fun openDial(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        launchSafely(intent)
    }

    private fun openWebsite(url: String) {
        val normalized = if (url.startsWith("http")) url else "https://$url"
        launchSafely(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
    }

    /**
     * No todos los dispositivos tienen una aplicacion para cada accion: sin
     * este control, abrir un sitio en un emulador sin navegador tira excepcion.
     */
    private fun launchSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (exception: Exception) {
            Toast.makeText(this, R.string.error_no_app, Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun readBreweryExtra(): Brewery? =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_BREWERY, Brewery::class.java)
        } else {
            intent.getSerializableExtra(EXTRA_BREWERY) as? Brewery
        }

    companion object {

        private const val EXTRA_BREWERY = "extra_brewery"

        /**
         * Punto de entrada unico a esta pantalla: quien la abre no necesita
         * conocer el nombre del extra ni armar el Intent.
         */
        fun start(context: Context, brewery: Brewery) {
            val intent = Intent(context, DetailActivity::class.java)
                .putExtra(EXTRA_BREWERY, brewery)
            context.startActivity(intent)
        }
    }
}
