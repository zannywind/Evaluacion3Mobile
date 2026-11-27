package com.mpaznotifica.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mpaznotifica.app.R
import com.mpaznotifica.app.ui.components.AvisoCard
import com.mpaznotifica.app.ui.components.DialogoServidor
import com.mpaznotifica.app.ui.components.InterruptorModoLocal
import com.mpaznotifica.app.ui.components.apoyo
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

/**
 * Pantalla de autenticación (RF-01): inicio de sesión diferenciado por perfil.
 *
 * - Valida cada campo por separado (correo con formato válido, clave obligatoria) y muestra el
 *   error bajo el campo con ícono.
 * - Modo local: las credenciales se contrastan con los usuarios de la base local (la clave se
 *   compara por hash, no en texto plano).
 * - Modo servidor: se validan en el servidor (POST /auth/login); sin conexión, el ViewModel usa
 *   como respaldo los usuarios guardados en Room. El botón "Servidor" abre el diálogo para
 *   cambiar la dirección de la API sin recompilar la app.
 * - El interruptor "Modo local (sin servidor)" cambia de modo en cualquier momento; en esta
 *   pantalla va aparte del diálogo "Servidor", que depende de los TODO(E3-9) y solo existe en
 *   modo servidor.
 * - El perfil (Inspectoría General o Profesor/a Jefe) lo determina el usuario encontrado.
 */
@Composable
fun LoginScreen(
    viewModel: MpazNotificaViewModel,
    onIngresar: () -> Unit
) {
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var claveVisible by remember { mutableStateOf(false) }
    var contenidoVisible by remember { mutableStateOf(false) }
    var mostrarServidor by rememberSaveable { mutableStateOf(false) }

    val errores by viewModel.erroresLogin.collectAsState()
    val errorCredenciales by viewModel.errorCredenciales.collectAsState()
    val ingresando by viewModel.ingresando.collectAsState()
    val urlServidor by viewModel.urlServidor.collectAsState()
    val modoLocal by viewModel.modoLocal.collectAsState()
    val foco = LocalFocusManager.current

    val errorUsuario = errores.usuario
    val errorClave = errores.clave

    LaunchedEffect(Unit) { contenidoVisible = true }

    fun ingresar() {
        foco.clearFocus()
        viewModel.iniciarSesion(usuario, clave, onIngresar)
    }

    // Si se pasa a modo local con el diálogo abierto, se cierra (y no reaparece al volver).
    LaunchedEffect(modoLocal) { if (modoLocal) mostrarServidor = false }

    // El diálogo "Servidor" solo existe en modo servidor.
    if (!modoLocal && mostrarServidor) {
        DialogoServidor(viewModel = viewModel, onCerrar = { mostrarServidor = false })
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = contenidoVisible,
                enter = fadeIn(animationSpec = tween(500)) +
                    slideInVertically(animationSpec = tween(500)) { alto -> alto / 6 }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_app),
                        contentDescription = "Logo de MPAZ Notifica",
                        modifier = Modifier.size(110.dp)
                    )
                    Text(text = "MPAZ Notifica", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Ingreso de personal docente y directivo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = usuario,
                        onValueChange = {
                            usuario = it
                            if (errorUsuario != null || errorCredenciales != null) viewModel.limpiarErroresLogin()
                        },
                        label = { Text("Correo institucional") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        trailingIcon = if (errorUsuario != null) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = "Campo con error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            null
                        },
                        isError = errorUsuario != null,
                        supportingText = apoyo(errorUsuario),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = clave,
                        onValueChange = {
                            clave = it
                            if (errorClave != null || errorCredenciales != null) viewModel.limpiarErroresLogin()
                        },
                        label = { Text("Clave de acceso") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { claveVisible = !claveVisible }) {
                                Icon(
                                    imageVector = if (claveVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (claveVisible) "Ocultar clave" else "Mostrar clave"
                                )
                            }
                        },
                        isError = errorClave != null,
                        supportingText = apoyo(errorClave),
                        singleLine = true,
                        visualTransformation = if (claveVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { ingresar() }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(
                        visible = errorCredenciales != null,
                        enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                    ) {
                        AvisoCard(
                            texto = errorCredenciales.orEmpty(),
                            icono = Icons.Filled.ErrorOutline,
                            esError = true
                        )
                    }

                    Button(
                        onClick = { ingresar() },
                        enabled = !ingresando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (ingresando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Ingresar")
                        }
                    }

                    if (!modoLocal) {
                        TextButton(onClick = { mostrarServidor = true }) {
                            Icon(Icons.Filled.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Servidor: $urlServidor")
                        }
                    }

                    InterruptorModoLocal(viewModel = viewModel)
                }
            }
        }
    }
}
