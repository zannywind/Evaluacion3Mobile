package com.mpaznotifica.app.ui.screen

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.navigation.Rutas
import com.mpaznotifica.app.ui.components.AvisoConexion
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

/**
 * Panel principal, visible después de iniciar sesión. Muestra el perfil activo, un resumen de la
 * bitácora (caché de la API en Room) y los accesos a redactar, historial y apoderados. Las restricciones por perfil
 * no dependen de estos botones: el ViewModel y las reglas de dominio las validan al enviar.
 */
@Composable
fun PanelScreen(
    navController: NavHostController,
    viewModel: MpazNotificaViewModel,
    onCerrarSesion: () -> Unit
) {
    val usuario by viewModel.usuarioActivo.collectAsState()
    val comunicados by viewModel.comunicados.collectAsState()
    val envios by viewModel.envios.collectAsState()
    val estadoDatos by viewModel.estadoDatos.collectAsState()
    val sinSesionRemota by viewModel.sinSesionRemota.collectAsState()

    val totalAnimado by animateIntAsState(
        targetValue = comunicados.size,
        animationSpec = tween(durationMillis = 600),
        label = "totalComunicados"
    )
    val despachados = envios.count { it.estado == EstadoEnvio.DESPACHADO }
    val fallidos = envios.size - despachados
    val esInspectoria = usuario?.perfil == Perfil.INSPECTORIA_GENERAL

    Scaffold(
        topBar = { BarraSuperior("MPAZ Notifica", navController, onCerrarSesion) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Hola, ${usuario?.nombre.orEmpty()}",
                style = MaterialTheme.typography.titleLarge
            )

            AvisoConexion(
                estado = estadoDatos,
                sinSesionRemota = sinSesionRemota,
                onReintentar = { viewModel.sincronizar() }
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Perfil: ${usuario?.perfil?.etiqueta.orEmpty()}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    usuario?.cursoAsignado?.let { curso ->
                        Text(text = "Curso de jefatura: $curso", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = "Comunicados en la bitácora: $totalAnimado",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(text = " $despachados despachado(s)   ", style = MaterialTheme.typography.bodySmall)
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(text = " $fallidos fallido(s)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(Rutas.REDACTAR) }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Redactar comunicado")
            }

            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(Rutas.HISTORIAL) }
            ) {
                Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Historial de comunicados")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(Rutas.APODERADOS) }
            ) {
                Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (esInspectoria) "Apoderados y canal preferente" else "Apoderados de mi curso")
            }
        }
    }
}
