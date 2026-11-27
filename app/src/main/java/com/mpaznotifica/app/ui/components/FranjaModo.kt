package com.mpaznotifica.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

/**
 * Franja fija en la parte superior de TODAS las pantallas que indica qué se está demostrando:
 * "MODO LOCAL" (solo Room, sin servidor) o "MODO SERVIDOR" (API + caché).
 */
@Composable
fun FranjaModo(viewModel: MpazNotificaViewModel, modifier: Modifier = Modifier) {
    val local by viewModel.modoLocal.collectAsState()
    val url by viewModel.urlServidor.collectAsState()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("franja_modo"),
        color = if (local) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (local) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (local) Icons.Filled.PhoneAndroid else Icons.Filled.Cloud,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (local) "MODO LOCAL · datos solo en este dispositivo (sin servidor)" else "MODO SERVIDOR · $url",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

/**
 * Interruptor "Modo local (sin servidor)" con su explicación y, en modo local, la acción
 * "Restablecer datos locales". El cambio se aplica de inmediato.
 */
@Composable
fun InterruptorModoLocal(viewModel: MpazNotificaViewModel, modifier: Modifier = Modifier) {
    val local by viewModel.modoLocal.collectAsState()
    var confirmando by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<Boolean?>(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Modo local (sin servidor)", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (local) {
                        "Activado: la app usa solo la base de este dispositivo, sin conexión."
                    } else {
                        "Desactivado: la app trabaja con el servidor."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = local,
                onCheckedChange = {
                    resultado = null
                    viewModel.cambiarModo(it)
                },
                modifier = Modifier.testTag("interruptor_modo_local")
            )
        }
        Text(
            text = "Respaldo para demostrar sin servicio. Los datos locales y los del servidor son " +
                "independientes: lo creado en modo local no se sincroniza al volver al modo servidor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (local) {
            TextButton(onClick = { confirmando = true }) { Text("Restablecer datos locales") }
            when (resultado) {
                true -> Text("Datos locales restablecidos.", style = MaterialTheme.typography.bodySmall)
                false -> Text("No se pudieron restablecer los datos locales.", style = MaterialTheme.typography.bodySmall)
                null -> Unit
            }
        }
    }

    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text("Restablecer datos locales") },
            text = {
                Text(
                    "Se borran los comunicados creados en modo local y se restauran los datos de ejemplo. " +
                        "No afecta al servidor."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmando = false
                    viewModel.restablecerDatosLocales { resultado = it }
                }) { Text("Restablecer") }
            },
            dismissButton = { TextButton(onClick = { confirmando = false }) { Text("Cancelar") } }
        )
    }
}
