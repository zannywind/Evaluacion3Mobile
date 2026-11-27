package com.mpaznotifica.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.EstadoDatos

/** Texto de apoyo de un campo: devuelve null si no hay mensaje (el campo queda sin texto bajo él). */
fun apoyo(mensaje: String?): (@Composable () -> Unit)? {
    if (mensaje == null) return null
    return { Text(mensaje) }
}

/** Etiqueta compacta con ícono (tipo, alcance, estado, canal). */
@Composable
fun EtiquetaInfo(
    texto: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    colorFondo: Color = MaterialTheme.colorScheme.secondaryContainer,
    colorTexto: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = colorFondo,
        contentColor = colorTexto
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = texto, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Tarjeta de aviso con ícono: informativa (naranjo suave) o de error. */
@Composable
fun AvisoCard(
    texto: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    esError: Boolean = false
) {
    val fondo = if (esError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    val contenido =
        if (esError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = fondo, contentColor = contenido)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icono, contentDescription = null)
            Text(text = texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Estado vacío de una lista. */
@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Ícono de cada canal de despacho. */
fun CanalDespacho.icono(): ImageVector = when (this) {
    CanalDespacho.EMAIL -> Icons.Filled.Email
    CanalDespacho.SMS -> Icons.Filled.Sms
    CanalDespacho.WHATSAPP -> Icons.Filled.Chat
}

/**
 * Estado de la conexión con el servidor: indicador de carga, hora de la última actualización, o
 * aviso de que se muestran los datos guardados / hubo un error, con "Reintentar".
 */
@Composable
fun AvisoConexion(
    estado: EstadoDatos,
    sinSesionRemota: Boolean,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (estado) {
        EstadoDatos.Inactivo -> Unit
        EstadoDatos.Cargando -> Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Actualizando datos desde el servidor…",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        is EstadoDatos.Actualizado -> Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CloudDone,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "  Datos actualizados ${estado.hora}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onReintentar) {
                Icon(Icons.Filled.Refresh, contentDescription = "Actualizar datos")
            }
        }
        is EstadoDatos.SinConexion -> Column(modifier = modifier.fillMaxWidth()) {
            AvisoCard(texto = estado.mensaje, icono = Icons.Filled.CloudOff)
            if (!sinSesionRemota) TextButton(onClick = onReintentar) { Text("Reintentar") }
        }
        is EstadoDatos.Error -> Column(modifier = modifier.fillMaxWidth()) {
            AvisoCard(texto = estado.mensaje, icono = Icons.Filled.CloudOff, esError = true)
            TextButton(onClick = onReintentar) { Text("Reintentar") }
        }
    }
}
