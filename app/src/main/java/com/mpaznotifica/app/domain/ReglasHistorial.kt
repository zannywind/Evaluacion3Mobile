package com.mpaznotifica.app.domain

import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.TipoComunicacion

/** Filtros del historial (RF-10). Las fechas son "AAAA-MM-DD"; null significa "sin filtro". */
data class FiltrosHistorial(
    val tipo: TipoComunicacion? = null,
    val emisorId: String? = null,
    val desde: String? = null,
    val hasta: String? = null
) {
    val cantidadActivos: Int
        get() = listOf(tipo, emisorId, desde, hasta).count { it != null }
}

/** Reglas de la bitácora (RF-09, RF-10), sin dependencias de Android. */
object ReglasHistorial {

    const val MSG_FECHAS_INVERTIDAS = "La fecha Desde no puede ser posterior a la fecha Hasta"
    const val MSG_FORMATO_FECHA = "Usa el formato AAAA-MM-DD"

    private val regexFecha = Regex("""^\d{4}-\d{2}-\d{2}$""")

    /** Fecha real del calendario (descarta 2026-02-31). */
    fun fechaValida(texto: String): Boolean {
        if (!regexFecha.matches(texto)) return false
        val anio = texto.substring(0, 4).toInt()
        val mes = texto.substring(5, 7).toInt()
        val dia = texto.substring(8, 10).toInt()
        if (mes !in 1..12) return false
        val bisiesto = (anio % 4 == 0 && anio % 100 != 0) || anio % 400 == 0
        val diasDelMes = when (mes) {
            2 -> if (bisiesto) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return dia in 1..diasDelMes
    }

    fun errorFechas(desde: String?, hasta: String?): String? {
        val d = desde?.trim().orEmpty()
        val h = hasta?.trim().orEmpty()
        if ((d.isNotEmpty() && !fechaValida(d)) || (h.isNotEmpty() && !fechaValida(h))) return MSG_FORMATO_FECHA
        if (d.isNotEmpty() && h.isNotEmpty() && d > h) return MSG_FECHAS_INVERTIDAS
        return null
    }

    /**
     * Aplica los filtros combinados (AND) y ordena del más reciente al más antiguo. Si el rango
     * de fechas es inválido, las fechas no se aplican (la pantalla muestra el error).
     */
    fun aplicar(todos: List<Comunicacion>, filtros: FiltrosHistorial): List<Comunicacion> {
        val usarFechas = errorFechas(filtros.desde, filtros.hasta) == null
        val desde = filtros.desde?.trim()?.takeIf { usarFechas && it.isNotEmpty() }
        val hasta = filtros.hasta?.trim()?.takeIf { usarFechas && it.isNotEmpty() }
        return todos
            .filter { filtros.tipo == null || it.tipo == filtros.tipo }
            .filter { filtros.emisorId == null || it.autorId == filtros.emisorId }
            .filter { desde == null || it.fechaHora.take(10) >= desde }
            .filter { hasta == null || it.fechaHora.take(10) <= hasta }
            .sortedByDescending { it.fechaHora }
    }
}
