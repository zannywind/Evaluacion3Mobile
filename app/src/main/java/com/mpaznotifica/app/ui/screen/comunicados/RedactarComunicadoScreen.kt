package com.mpaznotifica.app.ui.screen.comunicados

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.model.AdjuntoPdf
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.EstadoEnvioUi
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.SolicitudComunicado
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.nativo.ArchivosPdf
import com.mpaznotifica.app.nativo.Notificador
import com.mpaznotifica.app.nativo.Vibracion
import com.mpaznotifica.app.ui.components.AvisoCard
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.ui.components.SelectorDesplegable
import com.mpaznotifica.app.ui.components.apoyo
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel
import kotlinx.coroutines.launch

/**
 * Redacción y envío de comunicados (RF-02 a RF-08, RF-12).
 *
 * - Asunto, cuerpo, tipo y adjunto PDF opcional (selector de archivos del sistema).
 * - Tipos, alcances y destinos según el perfil: el Profesor/a Jefe solo ve su curso.
 * - Validación por campo (isError + supportingText + ícono) una vez intentado el envío.
 * - Casilla de autorización con firma física: aviso animado de rol solo informativo.
 * - Confirmación previa, progreso del despacho simulado y alerta con el resultado, acompañada
 *   de vibración y notificación local.
 */
@Composable
fun RedactarComunicadoScreen(
    navController: NavHostController,
    viewModel: MpazNotificaViewModel
) {
    val context = LocalContext.current
    val usuario by viewModel.usuarioActivo.collectAsState()
    val datos by viewModel.datos.collectAsState()
    val estadoEnvio by viewModel.envioUi.collectAsState()

    var asunto by rememberSaveable { mutableStateOf("") }
    var cuerpo by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf<TipoComunicacion?>(null) }
    var alcance by rememberSaveable { mutableStateOf<AlcanceEnvio?>(null) }
    var destino by rememberSaveable { mutableStateOf<String?>(null) }
    var requiereFirma by rememberSaveable { mutableStateOf(false) }
    var intentoEnvio by rememberSaveable { mutableStateOf(false) }
    var confirmando by rememberSaveable { mutableStateOf(false) }
    var permisoSolicitado by rememberSaveable { mutableStateOf(false) }
    var adjunto by remember { mutableStateOf<AdjuntoPdf?>(null) }
    var errorAdjunto by remember { mutableStateOf<String?>(null) }

    val scroll = rememberScrollState()
    val alcanceScope = rememberCoroutineScope()

    // Un envío anterior no debe reaparecer al abrir la pantalla.
    LaunchedEffect(Unit) { viewModel.reiniciarEnvio() }

    // La vibración acompaña la alerta de resultado.
    LaunchedEffect(estadoEnvio) {
        val actual = estadoEnvio
        if (actual is EstadoEnvioUi.Listo) Vibracion.confirmar(context, exito = actual.resumen.fallidos == 0)
    }

    val selectorPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val resultado = ArchivosPdf.inspeccionar(context, uri)
            adjunto = resultado.adjunto
            errorAdjunto = resultado.error
        }
    }

    // En Android 13+ se pide el permiso de notificaciones una vez, justo antes de confirmar.
    val solicitudPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        confirmando = true
    }

    val errores: ErroresComunicado = if (intentoEnvio) {
        viewModel.validarComunicado(asunto, cuerpo, tipo, alcance, destino)
    } else {
        ErroresComunicado()
    }
    val tiposDisponibles = viewModel.tiposPermitidos(usuario)
    val alcancesDisponibles = viewModel.alcancesPermitidos(usuario)
    val destinosDisponibles = remember(alcance, usuario, datos) { viewModel.destinosPara(alcance, usuario) }
    val cantidadDestinatarios = viewModel.contarDestinatarios(alcance, destino)
    val enviando = estadoEnvio is EstadoEnvioUi.Enviando

    Scaffold(
        topBar = { BarraSuperior("Redactar comunicado", navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scroll)
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (usuario?.perfil == Perfil.PROFESOR_JEFE) {
                AvisoCard(
                    texto = "Como Profesor/a Jefe solo puedes enviar comunicados a tu curso" +
                        (usuario?.cursoAsignado?.let { " ($it)" } ?: "") + ".",
                    icono = Icons.Filled.Info
                )
            }

            OutlinedTextField(
                value = asunto,
                onValueChange = { asunto = it },
                label = { Text("Asunto") },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                trailingIcon = if (errores.asunto != null) {
                    { Icon(Icons.Filled.ErrorOutline, contentDescription = "Campo con error", tint = MaterialTheme.colorScheme.error) }
                } else {
                    null
                },
                isError = errores.asunto != null,
                supportingText = apoyo(errores.asunto ?: "${asunto.trim().length}/${ReglasComunicado.MAX_ASUNTO}"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                enabled = !enviando,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cuerpo,
                onValueChange = { cuerpo = it },
                label = { Text("Cuerpo del mensaje") },
                leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) },
                trailingIcon = if (errores.cuerpo != null) {
                    { Icon(Icons.Filled.ErrorOutline, contentDescription = "Campo con error", tint = MaterialTheme.colorScheme.error) }
                } else {
                    null
                },
                isError = errores.cuerpo != null,
                supportingText = apoyo(errores.cuerpo ?: "${cuerpo.trim().length}/${ReglasComunicado.MAX_CUERPO}"),
                minLines = 4,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                enabled = !enviando,
                modifier = Modifier.fillMaxWidth()
            )

            SelectorDesplegable(
                etiqueta = "Tipo de comunicación",
                opciones = tiposDisponibles,
                seleccion = tipo,
                textoDe = { it.etiqueta },
                onSeleccion = { tipo = it },
                error = errores.tipo,
                leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) },
                enabled = !enviando
            )

            SelectorDesplegable(
                etiqueta = "Alcance (destinatarios)",
                opciones = alcancesDisponibles,
                seleccion = alcance,
                textoDe = { it.etiqueta },
                onSeleccion = {
                    alcance = it
                    destino = null
                },
                error = errores.alcance,
                leadingIcon = { Icon(Icons.Filled.Groups, contentDescription = null) },
                enabled = !enviando
            )

            AnimatedVisibility(
                visible = alcance != null && alcance != AlcanceEnvio.COLEGIO_COMPLETO,
                enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                SelectorDesplegable(
                    etiqueta = "Destino",
                    opciones = destinosDisponibles,
                    seleccion = destino,
                    textoDe = { it },
                    onSeleccion = { destino = it },
                    error = errores.destino,
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    enabled = !enviando
                )
            }

            AnimatedVisibility(
                visible = alcance != null && cantidadDestinatarios > 0,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(200))
            ) {
                Text(
                    text = "Destinatarios: $cantidadDestinatarios apoderado(s). " +
                        if (requiereFirma) "Se informará por correo a todos."
                        else "Cada uno recibirá el comunicado por su canal preferente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Adjunto PDF opcional (recurso nativo: selector de archivos del sistema).
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    enabled = !enviando,
                    onClick = { selectorPdf.launch(arrayOf(ReglasComunicado.TIPO_MIME_PDF)) }
                ) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (adjunto == null) "Adjuntar PDF (opcional)" else "Cambiar PDF")
                }
            }
            AnimatedVisibility(
                visible = adjunto != null,
                enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = adjunto?.let { "${it.nombre} (${tamanoLegible(it.tamanoBytes)})" }.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(enabled = !enviando, onClick = { adjunto = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Quitar PDF")
                    }
                }
            }
            AnimatedVisibility(visible = errorAdjunto != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = errorAdjunto.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // RF-12: autorización con firma legal, rol solo informativo.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = requiereFirma,
                    onCheckedChange = { requiereFirma = it },
                    enabled = !enviando
                )
                Text("Es una autorización que requiere firma física del apoderado")
            }
            AnimatedVisibility(
                visible = requiereFirma,
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                exit = fadeOut(tween(250)) + shrinkVertically(tween(250))
            ) {
                AvisoCard(
                    texto = "Rol solo informativo: la app notifica por correo y avisa que la autorización " +
                        "debe entregarse físicamente con firma manuscrita. No se capturan firmas en la app.",
                    icono = Icons.Filled.Warning
                )
            }

            Button(
                enabled = !enviando,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                onClick = {
                    intentoEnvio = true
                    val hayErrores =
                        viewModel.validarComunicado(asunto, cuerpo, tipo, alcance, destino).hayErrores
                    if (hayErrores || errorAdjunto != null) {
                        alcanceScope.launch { scroll.animateScrollTo(0) }
                    } else if (viewModel.necesitaPermisoNotificaciones() && !permisoSolicitado) {
                        permisoSolicitado = true
                        solicitudPermiso.launch(Notificador.PERMISO)
                    } else {
                        confirmando = true
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar")
            }
        }
    }

    // Confirmación previa al envío.
    val tipoElegido = tipo
    val alcanceElegido = alcance
    if (confirmando && tipoElegido != null && alcanceElegido != null) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
            title = { Text("¿Enviar comunicado?") },
            text = {
                Text(
                    "Se enviará a $cantidadDestinatarios apoderado(s) (despacho simulado en el servidor). Una vez enviado, " +
                        "el comunicado queda registrado en el historial y no se puede editar."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmando = false
                        viewModel.enviarComunicado(
                            SolicitudComunicado(
                                asunto = asunto,
                                cuerpo = cuerpo,
                                tipo = tipoElegido,
                                alcance = alcanceElegido,
                                destino = destino,
                                adjunto = adjunto,
                                requiereFirmaFisica = requiereFirma
                            )
                        )
                    }
                ) { Text("Enviar") }
            },
            dismissButton = { TextButton(onClick = { confirmando = false }) { Text("Cancelar") } }
        )
    }

    // Progreso, resultado y errores del despacho simulado.
    when (val estado = estadoEnvio) {
        is EstadoEnvioUi.Enviando -> {
            val avance by animateFloatAsState(
                targetValue = if (estado.total == 0) 0f else estado.procesados.toFloat() / estado.total,
                animationSpec = tween(250),
                label = "avanceEnvio"
            )
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Enviando comunicado…") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(progress = { avance }, modifier = Modifier.fillMaxWidth())
                        Text("${estado.procesados} de ${estado.total} destinatario(s) procesados")
                    }
                },
                confirmButton = {}
            )
        }

        is EstadoEnvioUi.Listo -> {
            val resumen = estado.resumen
            AlertDialog(
                onDismissRequest = {},
                icon = {
                    Icon(
                        imageVector = if (resumen.fallidos == 0) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("Comunicado enviado") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${resumen.despachados} despachado(s)")
                        }
                        if (resumen.fallidos > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${resumen.fallidos} fallido(s): revisa el detalle en el historial")
                            }
                        }
                        if (resumen.requiereFirmaFisica) {
                            Text(
                                "Recuerda: la autorización debe entregarse físicamente con firma manuscrita.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.reiniciarEnvio()
                            navController.popBackStack()
                        }
                    ) { Text("Aceptar") }
                }
            )
        }

        is EstadoEnvioUi.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.reiniciarEnvio() },
                icon = { Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text(if (estado.puedeReintentar) "No se pudo registrar" else "No se pudo enviar") },
                text = { Text(estado.mensaje) },
                confirmButton = {
                    if (estado.puedeReintentar) {
                        TextButton(onClick = { viewModel.reintentarRegistro() }) { Text("Reintentar") }
                    } else {
                        TextButton(onClick = { viewModel.reiniciarEnvio() }) { Text("Aceptar") }
                    }
                },
                dismissButton = if (estado.puedeReintentar) {
                    { TextButton(onClick = { viewModel.reiniciarEnvio() }) { Text("Descartar") } }
                } else {
                    null
                }
            )
        }

        EstadoEnvioUi.Inactivo -> Unit
    }
}

private fun tamanoLegible(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}
