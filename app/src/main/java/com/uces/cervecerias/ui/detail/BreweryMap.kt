package com.uces.cervecerias.ui.detail

import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.uces.cervecerias.R
import com.uces.cervecerias.ui.theme.Ink
import com.uces.cervecerias.ui.theme.Signal
import com.uces.cervecerias.ui.theme.Surface
import com.uces.cervecerias.ui.theme.TextSecondary
import com.uces.cervecerias.util.MapBlock

/**
 * Mapa de ubicacion de la cerveceria.
 *
 * Open Brewery DB no publica imagenes, pero si coordenadas. Con ellas se piden
 * las imagenes de tile de OpenStreetMap: cuatro, dispuestas en 2x2 y
 * desplazadas para que el local quede bajo el marcador. Un tile suelto no
 * alcanza, porque la grilla es fija y el punto casi nunca cae en el medio.
 *
 * El bloque se arma dentro de un FrameLayout con margenes en pixeles en lugar
 * de componerlo con Column y Row. Al ser mas grande que el recuadro que lo
 * contiene, el acomodamiento automatico lo corria de lugar y parte de las
 * imagenes terminaba fuera de la pantalla; con posiciones absolutas cada tile
 * queda exactamente donde lo manda el calculo.
 *
 * Las imagenes las descarga Picasso, igual que los logos del listado.
 */
@Composable
fun BreweryMap(
    block: MapBlock,
    height: Dp = 230.dp,
    modifier: Modifier = Modifier
) {
    // Cada reintento vuelve a pedir las cuatro imagenes desde cero.
    var attempt by remember(block) { mutableStateOf(0) }
    var failed by remember(block, attempt) { mutableStateOf(0) }

    // Si una sola imagen no llega, el mapa queda incompleto y confundiria mas
    // de lo que ayuda: se muestra el aviso con la opcion de volver a intentar.
    if (failed > 0) {
        MapUnavailable(
            message = stringResource(R.string.detail_map_error),
            height = height,
            modifier = modifier,
            actionLabel = stringResource(R.string.detail_map_retry),
            onAction = { attempt += 1 }
        )
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
    ) {
        val density = LocalDensity.current
        val viewportWidth = with(density) { maxWidth.roundToPx() }
        val viewportHeight = with(density) { maxHeight.roundToPx() }
        val pinHeight = with(density) { PIN_HEIGHT.roundToPx() }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context -> FrameLayout(context) },
            update = { frame ->
                // El aviso de fallo se guarda antes de armar nada, para que lo
                // encuentre cualquier descarga que termine mal.
                frame.setTag(R.id.map_error_listener, { failed += 1 })

                // La etiqueta evita rearmar el bloque en cada recomposicion:
                // Picasso cancela el pedido anterior sobre la misma vista al
                // rearrancar, y una cancelacion no avisa por onError, con lo
                // cual los tiles quedaban en negro sin que nadie se enterara.
                val key = "${block.originX}/${block.originY}#$attempt"
                if (frame.getTag(R.id.map_block_key) == key) return@AndroidView
                frame.setTag(R.id.map_block_key, key)
                frame.removeAllViews()

                // Cada tile se dibuja del ancho del recuadro, asi que el bloque
                // completo mide el doble: queda margen para correrlo.
                val tile = viewportWidth
                val blockSize = tile * 2
                val pointX = blockSize * block.offsetXRatio
                val pointY = blockSize * block.offsetYRatio

                // Desplazamiento para dejar el punto bajo el marcador, sin
                // destapar los bordes del recuadro.
                val shiftX = (viewportWidth / 2f - pointX)
                    .coerceIn((viewportWidth - blockSize).toFloat(), 0f)
                val shiftY = (viewportHeight * PIN_HEIGHT_FRACTION - pointY)
                    .coerceIn((viewportHeight - blockSize).toFloat(), 0f)

                block.tileUrls.forEachIndexed { index, url ->
                    val column = index % 2
                    val row = index / 2
                    val image = ImageView(frame.context).apply {
                        scaleType = ImageView.ScaleType.FIT_XY
                    }
                    val params = FrameLayout.LayoutParams(tile, tile).apply {
                        leftMargin = (shiftX + column * tile).toInt()
                        topMargin = (shiftY + row * tile).toInt()
                    }
                    frame.addView(image, params)

                    // Sin fit(): el ImageView todavia mide cero cuando Picasso
                    // consulta su tamano y la carga quedaria esperando.
                    Picasso.get()
                        .load(url)
                        .into(image, object : Callback {
                            override fun onSuccess() = Unit

                            override fun onError(e: Exception?) {
                                Log.w(TAG, "No se pudo cargar el tile $url", e)
                                reportTileError(frame)
                            }
                        })
                }

                // El marcador es el logo de la aplicacion. Se ubica con la
                // punta sobre las coordenadas, no con su centro.
                val pin = ContextCompat.getDrawable(frame.context, R.drawable.ic_map_pin)
                if (pin != null) {
                    val pinWidth = pinHeight * pin.intrinsicWidth / pin.intrinsicHeight
                    val marker = ImageView(frame.context).apply { setImageDrawable(pin) }
                    val params = FrameLayout.LayoutParams(pinWidth, pinHeight).apply {
                        leftMargin = (pointX + shiftX - pinWidth / 2f).toInt()
                        topMargin = (pointY + shiftY).toInt() - pinHeight
                    }
                    frame.addView(marker, params)
                }
            }
        )

        // Degradado inferior: el mapa se funde con el fondo en vez de cortarse.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.62f to Color.Transparent,
                        1f to Ink
                    )
                )
        )
    }
}

/** Fraccion de la altura del mapa donde se apoya la punta del marcador. */
private const val PIN_HEIGHT_FRACTION = 0.44f

/** Alto del marcador. */
private val PIN_HEIGHT = 52.dp

private const val TAG = "BreweryMap"

/** Avisa hacia Compose que una de las imagenes no se pudo descargar. */
@Suppress("UNCHECKED_CAST")
private fun reportTileError(frame: FrameLayout) {
    val listener = frame.getTag(R.id.map_error_listener) as? Function0<Unit>
    listener?.invoke()
}

/**
 * Aviso que reemplaza al mapa cuando no se puede mostrar: porque la cerveceria
 * no tiene coordenadas cargadas, o porque las imagenes no llegaron.
 */
@Composable
fun MapUnavailable(
    message: String,
    height: Dp = 230.dp,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = actionLabel,
                    color = Signal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}
