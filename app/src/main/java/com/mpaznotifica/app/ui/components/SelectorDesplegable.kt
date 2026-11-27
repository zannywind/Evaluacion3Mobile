package com.mpaznotifica.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Lista desplegable reutilizable (Material 3, ExposedDropdownMenuBox) para elegir un valor de
 * una lista: tipos, alcances, destinos, canales, etc.
 *
 * ExposedDropdownMenu y menuAnchor() son funciones del bloque de ExposedDropdownMenuBox, por eso
 * se usan dentro de él y NO se importan.
 *
 * @param opciones   valores posibles.
 * @param seleccion  valor elegido (null = nada elegido todavía).
 * @param textoDe    cómo mostrar cada valor (por ejemplo { it.etiqueta }).
 * @param error      mensaje de error de validación (null = sin error).
 * @param ayuda      texto de apoyo bajo el campo cuando no hay error.
 * @param leadingIcon ícono al inicio del campo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectorDesplegable(
    etiqueta: String,
    opciones: List<T>,
    seleccion: T?,
    textoDe: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
    textoVacio: String = "Seleccionar…",
    error: String? = null,
    ayuda: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    var expandido by remember { mutableStateOf(false) }
    val textoApoyo = error ?: ayuda

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { if (enabled) expandido = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = if (seleccion != null) textoDe(seleccion) else textoVacio,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(etiqueta) },
            leadingIcon = leadingIcon,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            isError = error != null,
            supportingText = if (textoApoyo != null) {
                { Text(textoApoyo) }
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(textoDe(opcion)) },
                    onClick = {
                        onSeleccion(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}
