package com.uces.cervecerias.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uces.cervecerias.data.model.Brewery
import com.uces.cervecerias.data.model.SavedStatus
import com.uces.cervecerias.ui.theme.CerveceriasTheme
import com.uces.cervecerias.ui.theme.HairlineSoft
import com.uces.cervecerias.ui.theme.Ink
import com.uces.cervecerias.ui.theme.Pending
import com.uces.cervecerias.ui.theme.Signal
import com.uces.cervecerias.ui.theme.Surface
import com.uces.cervecerias.ui.theme.TextPrimary
import com.uces.cervecerias.ui.theme.TextSecondary
import com.uces.cervecerias.ui.theme.TextTertiary
import com.uces.cervecerias.util.MapTiles

/**
 * Detalle de una cerveceria, escrito enteramente en Jetpack Compose.
 */
@Composable
fun DetailScreen(
    brewery: Brewery,
    savedStatus: SavedStatus?,
    onBack: () -> Unit,
    onSelectStatus: (SavedStatus) -> Unit,
    onCall: (String) -> Unit,
    onOpenWebsite: (String) -> Unit,
    noMapMessage: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
    ) {

        Box {
            if (brewery.hasCoordinates) {
                BreweryMap(
                    block = MapTiles.blockFor(brewery.latitude!!, brewery.longitude!!)
                )
            } else {
                MapUnavailable(noMapMessage)
            }
            BackButton(onBack = onBack, modifier = Modifier.padding(16.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            brewery.breweryType?.let { type ->
                Text(
                    text = type.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Signal
                )
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = brewery.name,
                fontSize = 27.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = listOfNotNull(brewery.city, brewery.state ?: brewery.country)
                    .joinToString(", "),
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(24.dp))

            StatusSelector(
                selected = savedStatus,
                onSelect = onSelectStatus
            )

            Spacer(Modifier.height(28.dp))

            DetailRow(label = "DIRECCION", value = brewery.fullAddress)

            DetailRow(
                label = "TELEFONO",
                value = brewery.phone ?: "sin dato",
                actionLabel = brewery.phone?.let { "Llamar" },
                onAction = { brewery.phone?.let(onCall) }
            )

            DetailRow(
                label = "SITIO WEB",
                value = brewery.websiteUrl?.removePrefix("http://")
                    ?.removePrefix("https://")?.removePrefix("www.")
                    ?.trimEnd('/') ?: "sin dato",
                actionLabel = brewery.websiteUrl?.let { "Abrir" },
                onAction = { brewery.websiteUrl?.let(onOpenWebsite) },
                showDivider = false
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Datos de Open Brewery DB \u00b7 Mapa \u00a9 OpenStreetMap",
                fontSize = 10.5.sp,
                color = TextTertiary
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * Control segmentado para elegir el estado. Un solo componente en vez de dos
 * botones sueltos: deja claro que las opciones son excluyentes.
 */
@Composable
private fun StatusSelector(
    selected: SavedStatus?,
    onSelect: (SavedStatus) -> Unit
) {
    Column {
        Text(
            text = "GUARDAR COMO",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SegmentButton(
                text = "Visitada",
                selected = selected == SavedStatus.VISITED,
                accent = Signal,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(SavedStatus.VISITED) }
            )
            SegmentButton(
                text = "Pendiente",
                selected = selected == SavedStatus.PENDING,
                accent = Pending,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(SavedStatus.PENDING) }
            )
        }
    }
}

@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Ink else TextSecondary
        )
    }
}

/** Fila de dato: etiqueta chica arriba, valor abajo, accion opcional al costado. */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    showDivider: Boolean = true
) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                Spacer(Modifier.height(5.dp))
                Text(text = value, fontSize = 15.sp, color = TextPrimary)
            }

            if (actionLabel != null && onAction != null) {
                Text(
                    text = actionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Signal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HairlineSoft)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Ink.copy(alpha = 0.78f))
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "\u2039", fontSize = 26.sp, color = TextPrimary)
    }
}

// Dos vistas previas: con datos completos y sin ubicacion, que son los dos
// casos reales que devuelve la API.

@Preview(name = "Con mapa", showBackground = true, backgroundColor = 0xFF13161A)
@Composable
private fun DetailScreenPreview() {
    CerveceriasTheme {
        DetailScreen(
            brewery = Brewery(
                id = "demo",
                name = "2Kids Brewing Company",
                breweryType = "micro",
                street = "8680 Miralani Dr Ste 123",
                city = "San Diego",
                state = "California",
                country = "United States",
                phone = "8584805437",
                websiteUrl = "http://www.2kidsbrewing.com",
                longitude = -117.137429,
                latitude = 32.896584
            ),
            savedStatus = SavedStatus.VISITED,
            onBack = {},
            onSelectStatus = {},
            onCall = {},
            onOpenWebsite = {},
            noMapMessage = "Sin ubicacion"
        )
    }
}

@Preview(name = "Sin ubicacion", showBackground = true, backgroundColor = 0xFF13161A)
@Composable
private fun DetailScreenNoMapPreview() {
    CerveceriasTheme {
        DetailScreen(
            brewery = Brewery(
                id = "demo2",
                name = "Cerveceria sin coordenadas",
                breweryType = "brewpub",
                street = null,
                city = "Kronach",
                state = "Bayern",
                country = "Germany",
                phone = null,
                websiteUrl = null,
                longitude = null,
                latitude = null
            ),
            savedStatus = null,
            onBack = {},
            onSelectStatus = {},
            onCall = {},
            onOpenWebsite = {},
            noMapMessage = "Esta cerveceria no tiene ubicacion cargada"
        )
    }
}
