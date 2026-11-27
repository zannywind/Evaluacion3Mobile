package com.mpaznotifica.app.ui.screen.comunicados

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpaznotifica.app.domain.Fechas
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.ResumenEnvio
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario
import com.mpaznotifica.app.navigation.Rutas
import com.mpaznotifica.app.ui.components.AvisoConexion
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.ui.components.EstadoVacio
import com.mpaznotifica.app.ui.components.EtiquetaInfo
import com.mpaznotifica.app.ui.components.SelectorDesplegable
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

/** Qué fecha del filtro se está eligiendo en el selector de fecha. */
private enum class CampoFechaFiltro { DESDE, HASTA }

/**
 * Historial y trazabilidad (RF-09, RF-10, RF-11): bitácora de todos los comunicados emitidos,
 * visible para los perfiles emisores, con filtros combinables por tipo, emisor y rango de
 * fechas. Cada tarjeta resume el resultado del despacho; al tocarla se abre el detalle de solo
 * lectura (RN-02).
 */
@Composable
fun HistorialScreen(
    navController: NavHostController,
    viewModel: MpazNotificaViewModel
) {
    val historial by viewModel.historial.collectAsState()
    val filtros by viewModel.filtros.collectAsState()
    val errorFechas by viewModel.errorFechas.collectAsState()
    val resumenes by viewModel.resumenes.collectAsState()
    val emisores by viewModel.emisores.collectAsState()
    val estadoDatos by viewModel.estadoDatos.collectAsState()
    val sinSesionRemota by viewModel.sinSesionRemota.collectAsState()

    var mostrarFiltros by rememberSaveable { mutableStateOf(false) }
    var campoEnEdicion by remember { mutableStateOf<CampoFechaFiltro?>(null) }

    // Al abrir la bitácora se pide al servidor la versión más reciente (si ya hay una en curso, no repite).
    LaunchedEffect(Unit) { viewModel.sincronizar() }

    Scaffold(
        topBar = { BarraSuperior("Historial de comunicados", navController) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AvisoConexion(
                    estado = estadoDatos,
                    sinSesionRemota = sinSesionRemota,
                    onReintentar = { viewModel.sincronizar() }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${historial.size} comunicado(s)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FilledTonalButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                        Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                        val activos = filtros.cantidadActivos
                        Text(if (activos > 0) "  Filtros ($activos)" else "  Filtros")
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = mostrarFiltros,
                    enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectorDesplegable<TipoComunicacion?>(
                                etiqueta = "Tipo de comunicación",
                                opciones = listOf<TipoComunicacion?>(null) + TipoComunicacion.entries,
                                seleccion = filtros.tipo,
                                textoDe = { it?.etiqueta ?: "Todos los tipos" },
                                onSeleccion = { viewModel.filtrarPorTipo(it) },
                                textoVacio = "Todos los tipos",
                                leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) }
                            )
                            SelectorDesplegable<Usuario?>(
                                etiqueta = "Emisor",
                                opciones = listOf<Usuario?>(null) + emisores,
                                seleccion = emisores.find { it.id == filtros.emisorId },
                                textoDe = { it?.nombre ?: "Todos los emisores" },
                                onSeleccion = { viewModel.filtrarPorEmisor(it?.id) },
                                textoVacio = "Todos los emisores",
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CampoFecha(
                                    etiqueta = "Desde",
                                    valor = filtros.desde,
                                    hayError = errorFechas != null,
                                    onAbrir = { campoEnEdicion = CampoFechaFiltro.DESDE },
                                    onLimpiar = { viewModel.filtrarDesde(null) },
                                    modifier = Modifier.weight(1f)
                                )
                                CampoFecha(
                                    etiqueta = "Hasta",
                                    valor = filtros.hasta,
                                    hayError = errorFechas != null,
                                    onAbrir = { campoEnEdicion = CampoFechaFiltro.HASTA },
                                    onLimpiar = { viewModel.filtrarHasta(null) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (errorFechas != null) {
                                Text(
                                    text = errorFechas.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            if (filtros.cantidadActivos > 0) {
                                TextButton(onClick = { viewModel.limpiarFiltros() }) {
                                    Text("Limpiar filtros")
                                }
                            }
                        }
                    }
                }
            }

            if (historial.isEmpty()) {
                item {
                    EstadoVacio(
                        icono = Icons.Filled.History,
                        titulo = "Sin comunicados",
                        detalle = if (filtros.cantidadActivos > 0) {
                            "Ningún comunicado coincide con los filtros."
                        } else {
                            "Aún no hay comunicados enviados."
                        }
                    )
                }
            }

            items(historial, key = { it.id }) { comunicado ->
                TarjetaComunicado(
                    comunicado = comunicado,
                    resumen = resumenes[comunicado.id],
                    onClick = { navController.navigate(Rutas.detalle(comunicado.id)) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    val campo = campoEnEdicion
    if (campo != null) {
        DialogoFecha(
            inicial = if (campo == CampoFechaFiltro.DESDE) filtros.desde else filtros.hasta,
            onElegida = { fecha ->
                if (campo == CampoFechaFiltro.DESDE) viewModel.filtrarDesde(fecha) else viewModel.filtrarHasta(fecha)
            },
            onCerrar = { campoEnEdicion = null }
        )
    }
}

@Composable
private fun TarjetaComunicado(
    comunicado: Comunicacion,
    resumen: ResumenEnvio?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = comunicado.asunto, style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EtiquetaInfo(texto = comunicado.tipo.etiqueta, icono = Icons.Filled.Category)
                EtiquetaInfo(
                    texto = comunicado.alcance.etiqueta + (comunicado.destinoReferencia?.let { " · $it" } ?: ""),
                    icono = Icons.Filled.Groups
                )
            }
            if (comunicado.requiereFirmaFisica) {
                EtiquetaInfo(
                    texto = "Firma física pendiente (rol informativo)",
                    icono = Icons.Filled.Warning,
                    colorFondo = MaterialTheme.colorScheme.tertiaryContainer,
                    colorTexto = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            if (resumen != null && resumen.total > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "${resumen.despachados} despachado(s)", style = MaterialTheme.typography.bodySmall)
                    if (resumen.fallidos > 0) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(text = "${resumen.fallidos} fallido(s)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text(
                text = "${Fechas.legible(comunicado.fechaHora)} · ${comunicado.autorNombre}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Campo de solo lectura que abre el selector de fecha al tocarlo (o al tocar su ícono de
 * calendario). El error del rango se marca en ambos campos (isError) y el mensaje se muestra
 * una sola vez bajo la fila.
 */
@Composable
private fun CampoFecha(
    etiqueta: String,
    valor: String?,
    hayError: Boolean,
    onAbrir: () -> Unit,
    onLimpiar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fuente = remember { MutableInteractionSource() }
    val abrir by rememberUpdatedState(onAbrir)
    LaunchedEffect(fuente) {
        fuente.interactions.collect { interaccion ->
            if (interaccion is PressInteraction.Release) abrir()
        }
    }
    OutlinedTextField(
        value = valor?.let { Fechas.fechaLegible(it) }.orEmpty(),
        onValueChange = {},
        readOnly = true,
        label = { Text(etiqueta) },
        placeholder = { Text("dd-mm-aaaa") },
        leadingIcon = {
            IconButton(onClick = onAbrir) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Elegir fecha $etiqueta")
            }
        },
        trailingIcon = if (valor != null) {
            {
                IconButton(onClick = onLimpiar) {
                    Icon(Icons.Filled.Clear, contentDescription = "Quitar fecha $etiqueta")
                }
            }
        } else {
            null
        },
        isError = hayError,
        singleLine = true,
        interactionSource = fuente,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoFecha(
    inicial: String?,
    onElegida: (String) -> Unit,
    onCerrar: () -> Unit
) {
    val estado = rememberDatePickerState(initialSelectedDateMillis = inicial?.let { Fechas.aMillisUtc(it) })
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(
                enabled = estado.selectedDateMillis != null,
                onClick = {
                    estado.selectedDateMillis?.let { onElegida(Fechas.deMillisUtc(it)) }
                    onCerrar()
                }
            ) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    ) {
        DatePicker(state = estado)
    }
}
