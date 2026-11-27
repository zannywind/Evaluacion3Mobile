package com.mpaznotifica.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mpaznotifica.app.ui.components.FranjaModo
import com.mpaznotifica.app.ui.screen.LoginScreen
import com.mpaznotifica.app.ui.screen.PanelScreen
import com.mpaznotifica.app.ui.screen.apoderados.ApoderadosScreen
import com.mpaznotifica.app.ui.screen.comunicados.DetalleComunicadoScreen
import com.mpaznotifica.app.ui.screen.comunicados.HistorialScreen
import com.mpaznotifica.app.ui.screen.comunicados.RedactarComunicadoScreen
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

object Rutas {
    const val LOGIN = "login"
    const val PANEL = "panel"
    const val REDACTAR = "redactar"
    const val HISTORIAL = "historial"
    const val DETALLE = "detalle/{id}"
    const val APODERADOS = "apoderados"

    const val ARG_ID = "id"

    fun detalle(id: String) = "detalle/$id"
}

/**
 * Flujo de pantallas (un solo NavHost y un único MpazNotificaViewModel compartido):
 *
 *   LOGIN ──(login OK)──> PANEL ──> REDACTAR
 *                               ──> HISTORIAL ──> DETALLE
 *                               ──> APODERADOS (canal preferente)
 *
 * Botón atrás: el atrás nativo de Android funciona solo (lo maneja el NavHost). Al ingresar,
 * LOGIN sale de la pila. "Cerrar sesión" limpia la sesión y el efecto de guarda vuelve a LOGIN
 * vaciando la pila; la misma guarda protege si el proceso se recrea sin sesión.
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: MpazNotificaViewModel = viewModel()
) {
    val usuario by viewModel.usuarioActivo.collectAsState()
    val rutaActual = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(usuario, rutaActual) {
        if (usuario == null && rutaActual != null && rutaActual != Rutas.LOGIN) {
            navController.navigate(Rutas.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    // Franja "MODO LOCAL / MODO SERVIDOR" fija sobre todas las pantallas; la franja ya cubre la
    // barra de estado, por eso las pantallas de abajo no la vuelven a reservar.
    Column(modifier = Modifier.fillMaxSize()) {
        FranjaModo(viewModel)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .consumeWindowInsets(WindowInsets.statusBars)
        ) {
            NavHost(
                navController = navController,
                startDestination = Rutas.LOGIN,
                enterTransition = { slideInHorizontally(animationSpec = tween(280)) { ancho -> ancho / 5 } + fadeIn(tween(280)) },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { slideOutHorizontally(animationSpec = tween(250)) { ancho -> ancho / 5 } + fadeOut(tween(250)) }
            ) {
                composable(Rutas.LOGIN) {
                    LoginScreen(
                        viewModel = viewModel,
                        onIngresar = {
                            navController.navigate(Rutas.PANEL) {
                                popUpTo(Rutas.LOGIN) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Rutas.PANEL) {
                    PanelScreen(navController, viewModel, onCerrarSesion = { viewModel.logout() })
                }
                composable(Rutas.REDACTAR) {
                    RedactarComunicadoScreen(navController, viewModel)
                }
                composable(Rutas.HISTORIAL) {
                    HistorialScreen(navController, viewModel)
                }
                composable(
                    route = Rutas.DETALLE,
                    arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.StringType })
                ) { entrada ->
                    DetalleComunicadoScreen(
                        navController = navController,
                        viewModel = viewModel,
                        comunicadoId = entrada.arguments?.getString(Rutas.ARG_ID)
                    )
                }
                composable(Rutas.APODERADOS) {
                    ApoderadosScreen(navController, viewModel)
                }
            }
        }
    }
}
