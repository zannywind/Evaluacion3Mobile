package com.mpaznotifica.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mpaznotifica.app.model.PruebaServidor
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

/**
 * Diálogo "Servidor": cambia la dirección de la API sin recompilar (por ejemplo, a la URL https
 * de VS Code Port Forwarding el día de la presentación). Valida el texto, permite "Probar
 * conexión" (GET /health) antes de guardar y persiste la dirección en el dispositivo. Si
 * "Probar conexión" falla, ofrece "Usar modo local" para seguir demostrando con la base del
 * dispositivo (el interruptor completo está en la pantalla de ingreso).
 */
@Composable
fun DialogoServidor(
    viewModel: MpazNotificaViewModel,
    onCerrar: () -> Unit
) {
    val urlActual by viewModel.urlServidor.collectAsState()
    val prueba by viewModel.pruebaServidor.collectAsState()
    var texto by rememberSaveable { mutableStateOf(urlActual) }
    var tocado by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { viewModel.limpiarPruebaServidor() }
    }

    val error = viewModel.validarServidor(texto)
    val mostrarError = if (tocado) error else null

    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { Icon(Icons.Filled.Dns, contentDescription = null) },
        title = { Text("Servidor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Dirección de la API de MPAZ Notifica. Emulador: http://10.0.2.2:3000/. " +
                        "Con VS Code Port Forwarding, pega la URL https que muestra la pestaña Puertos.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = texto,
                    onValueChange = {
                        texto = it
                        tocado = true
                        viewModel.limpiarPruebaServidor()
                    },
                    label = { Text("Dirección del servidor") },
                    isError = mostrarError != null,
                    supportingText = { Text(mostrarError ?: "Debe terminar en / (se agrega sola)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                when (val resultado = prueba) {
                    PruebaServidor.Inactiva -> Unit
                    PruebaServidor.Probando -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Probando conexión…", style = MaterialTheme.typography.bodyMedium)
                    }
                    is PruebaServidor.Correcta ->
                        AvisoCard(texto = resultado.mensaje, icono = Icons.Filled.CheckCircle)
                    is PruebaServidor.Fallida -> {
                        AvisoCard(texto = resultado.mensaje, icono = Icons.Filled.ErrorOutline, esError = true)
                        TextButton(onClick = { viewModel.cambiarModo(true) }) { Text("Usar modo local") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        enabled = error == null && prueba !is PruebaServidor.Probando,
                        onClick = { viewModel.probarServidor(texto) }
                    ) { Text("Probar conexión") }
                    TextButton(onClick = {
                        viewModel.restaurarServidor()
                        texto = viewModel.urlServidor.value
                    }) { Text("Restaurar") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = error == null,
                onClick = { if (viewModel.guardarServidor(texto) == null) onCerrar() }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
