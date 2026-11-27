package com.mpaznotifica.app

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * EXPERIENCIA 3 - Parte 4: prueba de interfaz (Compose UI Test) de la pantalla de ingreso.
 * Se ejecuta en un emulador o dispositivo: `./gradlew connectedDebugAndroidTest`.
 *
 * Preparación sugerida:
 *   composeRule.setContent {
 *       MpazNotificaTheme { LoginScreen(viewModel = MpazNotificaViewModel(ApplicationProvider.getApplicationContext()), onIngresar = {}) }
 *   }
 * (ajuste los parámetros a la firma real de LoginScreen). Busque elementos por texto visible con
 * `onNodeWithText(...)`; no hay testTag definidos.
 *
 * Retire @Ignore al completar. Mínimo 3 casos:
 *  1. Con los campos vacíos, al pulsar "Ingresar" aparecen los errores bajo "Correo institucional" y "Clave de acceso".
 *  2. Con una clave incorrecta aparece "Correo o clave incorrectos".
 *  3. En modo servidor (interruptor "Modo local (sin servidor)" apagado), el botón "Servidor" abre el
 *     diálogo y una dirección inválida muestra su error y deshabilita Guardar (en modo local el botón
 *     no existe). Con USAR_API=false la app parte en modo local.
 */
@Ignore("E3: completar")
class LoginUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun camposVaciosMuestranErrores() {
        // TODO(E3-P4)
    }

    @Test
    fun claveIncorrectaMuestraMensajeGeneral() {
        // TODO(E3-P4)
    }

    @Test
    fun dialogoServidorValidaLaDireccion() {
        // TODO(E3-P4)
    }
}
