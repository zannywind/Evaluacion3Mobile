package com.mpaznotifica.app.ui.screen.comunicados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpaznotifica.app.domain.Fechas
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.ui.components.AvisoCard
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.ui.components.EstadoVacio
import com.mpaznotifica.app.ui.components.icono
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

/**
 * Detalle de un comunicado del historial. Cada registro incluye automáticamente autor,
 * fecha/hora, contenido, destinatarios y canal empleado (RF-11) y el estado del envío por
 * destinatario (RF-08).
 *
 * RN-02: el comunicado es INMUTABLE, por eso esta pantalla es solo de lectura (no tiene botones
 * de editar ni eliminar).
 */
@Composable
fun DetalleComunicadoScreen(
    navController: NavHostController,
    viewModel: MpazNotificaViewModel,
    comunicadoId: String?
) {
    val comunicados by viewModel.comunicados.collectAsState()
    val todosLosEnvios by viewModel.envios.collectAsState()
    val datos by viewModel.datos.collectAsState()

    val comunicado = comunicados.find { it.id == comunicadoId }
    val envios = remember(todosLosEnvios, comunicadoId) {
        todosLosEnvios.filter { it.comunicacionId == comunicadoId }
    }

    Scaffold(
        topBar = { BarraSuperior("Detalle del comunicado", navController) }
    ) { padding ->
        if (comunicado == null) {
            EstadoVacio(
                icono = Icons.Filled.Warning,
                titulo = "Comunicado no encontrado",
                detalle = "El registro solicitado no existe en la bitácora.",
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(comunicado.asunto, style = MaterialTheme.typography.titleLarge)
                    Text(comunicado.cuerpo, style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Tipo: ${comunicado.tipo.etiqueta}")
                    Text(
                        "Alcance: ${comunicado.alcance.etiqueta}" +
                            (comunicado.destinoReferencia?.let { " ($it)" } ?: "")
                    )
                    Text("Autor: ${comunicado.autorNombre}")
                    Text("Fecha y hora: ${Fechas.legible(comunicado.fechaHora)}")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adjunto: ${comunicado.adjuntoPdfNombre ?: "sin adjunto"}")
                    }
                }
            }

            if (comunicado.requiereFirmaFisica) {
                AvisoCard(
                    texto = "Autorización con firma legal: se notificó por correo. Corresponde la entrega " +
                        "física del documento firmado a mano; la app no captura firmas.",
                    icono = Icons.Filled.Warning
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Registro inmutable: no se puede editar ni eliminar.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val despachados = envios.count { it.estado == EstadoEnvio.DESPACHADO }
            Text(
                text = "Destinatarios y estado del envío (${envios.size})",
                style = MaterialTheme.typography.titleSmall
            )
            if (envios.isEmpty()) {
                Text("Sin registros de envío.", style = MaterialTheme.typography.bodySmall)
            } else {
                Text(
                    text = "$despachados despachado(s) · ${envios.size - despachados} fallido(s)",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            envios.forEach { envio ->
                FilaEnvio(envio = envio, apoderado = datos.apoderadoPorId(envio.apoderadoId))
            }
        }
    }
}

@Composable
private fun FilaEnvio(envio: EnvioDestinatario, apoderado: Apoderado?) {
    val fallido = envio.estado == EstadoEnvio.FALLIDO
    val colorEstado = if (fallido) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val iconoEstado: ImageVector = if (fallido) Icons.Filled.Error else Icons.Filled.CheckCircle

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = iconoEstado, contentDescription = envio.estado.etiqueta, tint = colorEstado)
            Column(modifier = Modifier.weight(1f)) {
                Text(apoderado?.nombreCompleto ?: envio.apoderadoId, style = MaterialTheme.typography.titleSmall)
                Text("${envio.canal.etiqueta} · ${envio.estado.etiqueta}", color = colorEstado)
                if (fallido && envio.detalle.isNotBlank()) {
                    Text(
                        text = envio.detalle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = envio.canal.icono(),
                contentDescription = envio.canal.etiqueta,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
