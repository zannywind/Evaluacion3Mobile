package com.mpaznotifica.app.domain

import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario

/**
 * Reglas de emisión de comunicados, desacopladas de la interfaz y de Android (probadas en JVM).
 *
 * - RF-02: opciones de emisión según el perfil autenticado.
 * - RF-03: validación del formulario por campo.
 * - RF-04: destinatarios según cuatro alcances (individual, curso, taller, colegio).
 * - RF-05: Inspectoría General emite a cualquiera de los cuatro alcances.
 * - RF-06 / RN-01: el Profesor/a Jefe solo emite a su curso (o a un apoderado de su curso);
 *   nunca a "colegio completo" ni a otro curso. Su alcance sobre talleres está pendiente de
 *   confirmar con el CITT: mientras tanto no se habilita.
 * - RF-12: una autorización con firma legal se informa siempre por correo.
 * - RF-13: canal preferente del apoderado.
 */
object ReglasComunicado {

    const val MIN_ASUNTO = 3
    const val MAX_ASUNTO = 100
    const val MIN_CUERPO = 10
    const val MAX_CUERPO = 1000

    /** Supuesto pendiente con el CITT: tamaño máximo del PDF adjunto. */
    const val MAX_ADJUNTO_BYTES = 5L * 1024 * 1024
    const val TIPO_MIME_PDF = "application/pdf"

    const val MSG_SOLO_CURSO = "Solo puedes enviar comunicados a tu curso"
    const val MSG_TIPO_NO_DISPONIBLE = "Este tipo de comunicado no está disponible para tu perfil"
    const val MSG_SIN_SESION = "Debes iniciar sesión para enviar comunicados"
    const val MSG_SIN_CURSO = "Tu usuario no tiene un curso asignado"
    const val MSG_SIN_DESTINATARIOS = "No hay apoderados para este destino"
    const val AVISO_FIRMA_FISICA =
        "IMPORTANTE: esta autorización requiere firma manuscrita del apoderado. " +
            "Debe firmarse en papel y entregarse en el establecimiento."

    // ---------------------------------------------------------------- RF-02 / RF-05 / RF-06

    fun tiposPermitidos(usuario: Usuario?): List<TipoComunicacion> = when (usuario?.perfil) {
        Perfil.INSPECTORIA_GENERAL -> TipoComunicacion.entries.toList()
        // Comunicaciones pedagógicas, citaciones y salidas pedagógicas de su propio curso.
        Perfil.PROFESOR_JEFE -> listOf(
            TipoComunicacion.PEDAGOGICA,
            TipoComunicacion.CITACION,
            TipoComunicacion.SALIDA_PEDAGOGICA
        )
        null -> emptyList()
    }

    fun alcancesPermitidos(usuario: Usuario?): List<AlcanceEnvio> = when (usuario?.perfil) {
        Perfil.INSPECTORIA_GENERAL -> AlcanceEnvio.entries.toList()
        Perfil.PROFESOR_JEFE -> listOf(AlcanceEnvio.CURSO, AlcanceEnvio.APODERADO_INDIVIDUAL)
        null -> emptyList()
    }

    // ---------------------------------------------------------------- RF-04: destinatarios

    fun apoderadosDelCurso(curso: String?, datos: DatosReferencia): List<Apoderado> {
        if (curso == null) return emptyList()
        val idsEstudiantes = datos.estudiantes.filter { it.curso == curso }.map { it.id }.toSet()
        return datos.apoderados.filter { apoderado -> apoderado.pupilosIds.any { it in idsEstudiantes } }
    }

    /** Opciones del selector "Destino" según el alcance y el perfil. */
    fun destinosPara(alcance: AlcanceEnvio?, usuario: Usuario?, datos: DatosReferencia): List<String> {
        if (usuario == null || alcance == null || alcance !in alcancesPermitidos(usuario)) return emptyList()
        return if (usuario.perfil == Perfil.PROFESOR_JEFE) {
            when (alcance) {
                AlcanceEnvio.CURSO -> listOfNotNull(usuario.cursoAsignado)
                AlcanceEnvio.APODERADO_INDIVIDUAL ->
                    apoderadosDelCurso(usuario.cursoAsignado, datos).map { it.nombreCompleto }
                AlcanceEnvio.TALLER, AlcanceEnvio.COLEGIO_COMPLETO -> emptyList()
            }
        } else {
            when (alcance) {
                AlcanceEnvio.CURSO -> datos.cursos
                AlcanceEnvio.TALLER -> datos.talleres.map { it.nombre }
                AlcanceEnvio.APODERADO_INDIVIDUAL -> datos.apoderados.map { it.nombreCompleto }
                AlcanceEnvio.COLEGIO_COMPLETO -> emptyList()
            }
        }
    }

    /** Apoderados que recibirán el comunicado, sin repetidos (un apoderado con dos pupilos). */
    fun resolverApoderados(alcance: AlcanceEnvio, destino: String?, datos: DatosReferencia): List<Apoderado> =
        when (alcance) {
            AlcanceEnvio.COLEGIO_COMPLETO -> datos.apoderados
            AlcanceEnvio.CURSO -> apoderadosDelCurso(destino, datos)
            AlcanceEnvio.TALLER -> {
                val tallerId = datos.talleres.find { it.nombre == destino }?.id
                val idsEstudiantes = datos.estudiantes
                    .filter { tallerId != null && tallerId in it.talleresIds }
                    .map { it.id }
                    .toSet()
                datos.apoderados.filter { apoderado -> apoderado.pupilosIds.any { it in idsEstudiantes } }
            }
            AlcanceEnvio.APODERADO_INDIVIDUAL -> datos.apoderados.filter { it.nombreCompleto == destino }
        }.distinctBy { it.id }

    // ---------------------------------------------------------------- RF-03: validación

    fun validar(
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion?,
        alcance: AlcanceEnvio?,
        destino: String?,
        usuario: Usuario?,
        datos: DatosReferencia
    ): ErroresComunicado {
        val errorAsunto = when {
            asunto.isBlank() -> "El asunto es obligatorio"
            asunto.trim().length < MIN_ASUNTO -> "El asunto debe tener al menos $MIN_ASUNTO caracteres"
            asunto.trim().length > MAX_ASUNTO -> "El asunto no puede superar los $MAX_ASUNTO caracteres"
            else -> null
        }
        val errorCuerpo = when {
            cuerpo.isBlank() -> "El cuerpo del mensaje es obligatorio"
            cuerpo.trim().length < MIN_CUERPO -> "El mensaje debe tener al menos $MIN_CUERPO caracteres"
            cuerpo.trim().length > MAX_CUERPO -> "El mensaje no puede superar los $MAX_CUERPO caracteres"
            else -> null
        }
        if (usuario == null) {
            return ErroresComunicado(asunto = errorAsunto, cuerpo = errorCuerpo, alcance = MSG_SIN_SESION)
        }
        val errorTipo = when {
            tipo == null -> "Selecciona el tipo de comunicación"
            tipo !in tiposPermitidos(usuario) -> MSG_TIPO_NO_DISPONIBLE
            else -> null
        }
        val esProfesor = usuario.perfil == Perfil.PROFESOR_JEFE
        val errorAlcance = when {
            alcance == null -> "Selecciona el alcance"
            esProfesor && usuario.cursoAsignado == null -> MSG_SIN_CURSO
            alcance !in alcancesPermitidos(usuario) ->
                if (esProfesor) MSG_SOLO_CURSO else "Tu perfil no puede emitir a ${alcance.etiqueta}"
            alcance == AlcanceEnvio.COLEGIO_COMPLETO && datos.apoderados.isEmpty() -> MSG_SIN_DESTINATARIOS
            else -> null
        }
        val errorDestino = when {
            alcance == null || errorAlcance != null -> null
            alcance == AlcanceEnvio.COLEGIO_COMPLETO -> null
            destino.isNullOrBlank() -> "Selecciona el destino"
            destino !in destinosPara(alcance, usuario, datos) ->
                if (esProfesor) MSG_SOLO_CURSO else "El destino seleccionado no es válido"
            resolverApoderados(alcance, destino, datos).isEmpty() -> MSG_SIN_DESTINATARIOS
            else -> null
        }
        return ErroresComunicado(
            asunto = errorAsunto,
            cuerpo = errorCuerpo,
            tipo = errorTipo,
            alcance = errorAlcance,
            destino = errorDestino
        )
    }

    /** Valida el archivo elegido como adjunto: debe ser PDF y no superar el tamaño máximo. */
    fun errorAdjunto(tipoMime: String?, tamanoBytes: Long?): String? = when {
        tipoMime != TIPO_MIME_PDF -> "El archivo adjunto debe ser un PDF"
        tamanoBytes != null && tamanoBytes > MAX_ADJUNTO_BYTES -> "El PDF supera el máximo de 5 MB"
        else -> null
    }

    // ---------------------------------------------------------------- RF-07 / RF-12: canal

    /** Una autorización con firma física se informa siempre por correo (RF-12). */
    fun canalDe(apoderado: Apoderado, requiereFirmaFisica: Boolean): CanalDespacho =
        if (requiereFirmaFisica) CanalDespacho.EMAIL else apoderado.canalPreferente

    /** Quita el aviso de firma física si el cuerpo ya lo trae (así nunca se duplica ni cuenta en el largo). */
    fun sinAviso(cuerpo: String): String {
        val texto = cuerpo.trim()
        return if (texto.endsWith(AVISO_FIRMA_FISICA)) texto.removeSuffix(AVISO_FIRMA_FISICA).trim() else texto
    }

    /** Texto que se registra: si hay firma física se agrega el aviso de entrega en papel. */
    fun cuerpoFinal(cuerpo: String, requiereFirmaFisica: Boolean): String =
        if (requiereFirmaFisica) "${cuerpo.trim()}\n\n$AVISO_FIRMA_FISICA" else cuerpo.trim()

    // ---------------------------------------------------------------- RF-13: canal preferente

    /** Solo Inspectoría General configura el canal preferente (supuesto pendiente con el CITT). */
    fun puedeConfigurarCanal(usuario: Usuario?): Boolean = usuario?.perfil == Perfil.INSPECTORIA_GENERAL

    /** Apoderados que el perfil puede consultar: todos, o los de su curso si es Profesor/a Jefe. */
    fun apoderadosVisibles(usuario: Usuario?, datos: DatosReferencia): List<Apoderado> = when (usuario?.perfil) {
        Perfil.INSPECTORIA_GENERAL -> datos.apoderados
        Perfil.PROFESOR_JEFE -> apoderadosDelCurso(usuario.cursoAsignado, datos)
        null -> emptyList()
    }
}
