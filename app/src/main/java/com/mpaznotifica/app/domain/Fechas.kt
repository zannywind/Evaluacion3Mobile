package com.mpaznotifica.app.domain

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Utilidades de fecha sin java.time (el proyecto soporta Android 7, API 24). */
object Fechas {

    private const val PATRON_FECHA_HORA = "yyyy-MM-dd HH:mm:ss"
    private const val PATRON_FECHA = "yyyy-MM-dd"

    /** Fecha y hora actual del dispositivo, como se registra en la bitácora (RF-11). */
    fun ahora(): String = SimpleDateFormat(PATRON_FECHA_HORA, Locale.ROOT).format(Date())

    /** Convierte los milisegundos (UTC) que entrega el selector de fecha a "AAAA-MM-DD". */
    fun deMillisUtc(millis: Long): String {
        val formato = SimpleDateFormat(PATRON_FECHA, Locale.ROOT)
        formato.timeZone = TimeZone.getTimeZone("UTC")
        return formato.format(Date(millis))
    }

    /** Milisegundos (UTC) de una fecha "AAAA-MM-DD", o null si no es una fecha válida. */
    fun aMillisUtc(fecha: String): Long? {
        val formato = SimpleDateFormat(PATRON_FECHA, Locale.ROOT)
        formato.timeZone = TimeZone.getTimeZone("UTC")
        formato.isLenient = false
        return try {
            formato.parse(fecha)?.time
        } catch (e: ParseException) {
            null
        }
    }

    /** "2026-09-15 09:30:00" -> "15-09-2026 09:30". Si el texto no calza, se devuelve igual. */
    fun legible(fechaHora: String): String {
        val coincidencia = Regex("""^(\d{4})-(\d{2})-(\d{2}) (\d{2}:\d{2})""").find(fechaHora) ?: return fechaHora
        val (anio, mes, dia, hora) = coincidencia.destructured
        return "$dia-$mes-$anio $hora"
    }

    /** "2026-09-15" -> "15-09-2026". */
    fun fechaLegible(fecha: String): String {
        val partes = fecha.split("-")
        return if (partes.size == 3) "${partes[2]}-${partes[1]}-${partes[0]}" else fecha
    }
}
