package com.mpaznotifica.app.ui.screen.apoderados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpaznotifica.app.domain.DatosReferencia
import com.mpaznotifica.app.domain.ValidadorContacto
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.ui.components.AvisoCard
import com.mpaznotifica.app.ui.components.AvisoConexion
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.ui.components.EstadoVacio
import com.mpaznotifica.app.ui.components.SelectorDesplegable
import com.mpaznotifica.app.ui.components.icono
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

/**
 * Apoderados y canal preferente de despacho (RF-13).
 *
 * Inspectoría General consulta a todos los apoderados y configura el canal preferente (correo,
 * SMS o WhatsApp); solo se ofrecen canales para los que el apoderado tiene un dato de contacto
 * válido. El Profesor/a Jefe solo consulta a los apoderados de su curso. El canal se guarda en
 * el servidor (y en su caché Room) y se usa en los despachos siguientes (las autorizaciones con firma física van siempre
 * por correo).
 *
 * Supuesto pendiente de confirmar con el CITT: quién configura el canal preferente.
 */
@Composable
fun ApoderadosScreen(
    navController: NavHostController,
    viewModel: MpazNotificaViewModel
) {
    val usuario by viewModel.usuarioActivo.collectAsState()
    val datos by viewModel.datos.collectAsState()
    val apoderados = viewModel.apoderadosVisibles(usuario)
    val puedeEditar = viewModel.puedeConfigurarCanal(usuario)
    val estadoDatos by viewModel.estadoDatos.collectAsState()
    val sinSesionRemota by viewModel.sinSesionRemota.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.sincronizar() }
    LaunchedEffect(Unit) {
        viewModel.mensajes.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            BarraSuperior(if (puedeEditar) "Apoderados y canal preferente" else "Apoderados de mi curso", navController)
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                AvisoConexion(
                    estado = estadoDatos,
                    sinSesionRemota = sinSesionRemota,
                    onReintentar = { viewModel.sincronizar() }
                )
            }
            if (!puedeEditar) {
                item {
                    AvisoCard(
                        texto = "Solo Inspectoría General puede cambiar el canal preferente. Aquí puedes consultarlo.",
                        icono = Icons.Filled.Info
                    )
                }
            }
            if (apoderados.isEmpty()) {
                item {
                    EstadoVacio(
                        icono = Icons.Filled.Groups,
                        titulo = "Sin apoderados",
                        detalle = "No hay apoderados para mostrar con tu perfil."
                    )
                }
            }
            items(apoderados, key = { it.id }) { apoderado ->
                TarjetaApoderado(
                    apoderado = apoderado,
                    datos = datos,
                    puedeEditar = puedeEditar,
                    onGuardar = { canal -> viewModel.actualizarCanalPreferente(apoderado.id, canal) }
                )
            }
        }
    }
}

@Composable
private fun TarjetaApoderado(
    apoderado: Apoderado,
    datos: DatosReferencia,
    puedeEditar: Boolean,
    onGuardar: (CanalDespacho) -> Unit
) {
    var canal by remember(apoderado.id, apoderado.canalPreferente) {
        mutableStateOf(apoderado.canalPreferente)
    }
    val disponibles = ValidadorContacto.canalesDisponibles(apoderado)
    val actualValido = ValidadorContacto.valido(apoderado, apoderado.canalPreferente)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = apoderado.nombreCompleto, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${apoderado.correo} · ${apoderado.telefono}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Pupilos: " + datos.nombresPupilos(apoderado).joinToString(", ").ifBlank { "sin pupilos" },
                style = MaterialTheme.typography.bodySmall
            )
            if (puedeEditar) {
                SelectorDesplegable(
                    etiqueta = "Canal preferente",
                    opciones = disponibles,
                    seleccion = canal,
                    textoDe = { it.etiqueta },
                    onSeleccion = { canal = it },
                    error = if (!actualValido && canal == apoderado.canalPreferente) {
                        "El contacto actual no es válido para este canal: el envío fallará"
                    } else {
                        null
                    },
                    ayuda = if (disponibles.isEmpty()) "Sin datos de contacto válidos" else null,
                    leadingIcon = { Icon(canal.icono(), contentDescription = null) },
                    enabled = disponibles.isNotEmpty()
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canal != apoderado.canalPreferente,
                    onClick = { onGuardar(canal) }
                ) {
                    Text("Guardar canal")
                }
            } else {
                Text(
                    text = "Canal preferente: ${apoderado.canalPreferente.etiqueta}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
