package com.mpaznotifica.app.data

import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.Taller
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario

/**
 * Base de datos sintética: datos 100% FICTICIOS (Ley 19.628 y Ley 21.719: prohibido usar datos
 * reales de estudiantes o apoderados). MpazRepository la copia a Room la primera vez que se
 * abre la app; desde ahí todo se lee y escribe en Room.
 *
 * Las claves de los usuarios de prueba no están en texto plano: aquí solo figura su hash
 * (SHA-256 con la sal de demostración de domain/Credenciales.kt). Las claves de prueba están en el README.
 */
object MockData {

    private const val HASH_INSPECTORIA = "dfa8c1c8c621471391ec1965eb346d9b2efda11915045d13c146135d10b0a66a"
    private const val HASH_PROFESOR = "382d77264fa0692379587045f7640e74e02993fc2898b78bc2ea6d93c76788f5"

    val usuarios = listOf(
        Usuario("U1", "Inspectoría General (demo)", "inspectoria@mpaznotifica.cl", HASH_INSPECTORIA, Perfil.INSPECTORIA_GENERAL),
        Usuario("U2", "Profesor/a Jefe 4° A (demo)", "profesor.4basicoA@mpaznotifica.cl", HASH_PROFESOR, Perfil.PROFESOR_JEFE, "4° Básico A"),
        Usuario("U3", "Profesor/a Jefe 2° A (demo)", "profesor.2basicoA@mpaznotifica.cl", HASH_PROFESOR, Perfil.PROFESOR_JEFE, "2° Básico A"),
    )

    val talleres = listOf(
        Taller("T1", "Taller de Robótica"),
        Taller("T2", "Taller de Fútbol"),
    )

    // Un apoderado puede tener más de un pupilo (AP1 tiene a E1 y E4; AP6 a E7 y E8).
    val estudiantes = listOf(
        Estudiante("E1", "Estudiante Ficticio 1", "4° Básico A", listOf("T1")),
        Estudiante("E2", "Estudiante Ficticio 2", "4° Básico A"),
        Estudiante("E3", "Estudiante Ficticio 3", "2° Básico A", listOf("T2")),
        Estudiante("E4", "Estudiante Ficticio 4", "8° Básico A", listOf("T1", "T2")),
        Estudiante("E5", "Estudiante Ficticio 5", "4° Básico A", listOf("T2")),
        Estudiante("E6", "Estudiante Ficticio 6", "1° Básico A"),
        Estudiante("E7", "Estudiante Ficticio 7", "3° Básico A"),
        Estudiante("E8", "Estudiante Ficticio 8", "5° Básico A"),
    )

    val cursos: List<String> = estudiantes.map { it.curso }.distinct().sorted()

    val apoderados = listOf(
        Apoderado("AP1", "Apoderado Ficticio 1", "apoderado1@ejemplo.cl", "+56900000001", CanalDespacho.EMAIL, listOf("E1", "E4")),
        Apoderado("AP2", "Apoderado Ficticio 2", "apoderado2@ejemplo.cl", "+56900000002", CanalDespacho.WHATSAPP, listOf("E2")),
        Apoderado("AP3", "Apoderado Ficticio 3", "apoderado3@ejemplo.cl", "+56900000003", CanalDespacho.SMS, listOf("E3")),
        // AP4 tiene el teléfono incompleto a propósito: permite ver un despacho FALLIDO.
        Apoderado("AP4", "Apoderado Ficticio 4", "apoderado4@ejemplo.cl", "+5690000", CanalDespacho.SMS, listOf("E5")),
        Apoderado("AP5", "Apoderado Ficticio 5", "apoderado5@ejemplo.cl", "+56900000005", CanalDespacho.EMAIL, listOf("E6")),
        Apoderado("AP6", "Apoderado Ficticio 6", "apoderado6@ejemplo.cl", "+56900000006", CanalDespacho.WHATSAPP, listOf("E7", "E8")),
    )

    /** Historial inicial de la bitácora (los despachos de estos registros ya ocurrieron). */
    val comunicados = listOf(
        Comunicacion(
            id = "C1", asunto = "Reunión de apoderados 4° Básico A",
            cuerpo = "Se cita a reunión de apoderados el viernes a las 18:00 hrs.",
            tipo = TipoComunicacion.CITACION, alcance = AlcanceEnvio.CURSO, destinoReferencia = "4° Básico A",
            autorId = "U2", autorNombre = "Profesor/a Jefe 4° A (demo)", fechaHora = "2026-09-01 10:00:00"
        ),
        Comunicacion(
            id = "C2", asunto = "Salida pedagógica al museo",
            cuerpo = ReglasComunicado.cuerpoFinal(
                "Se informa salida pedagógica al museo. La autorización debe firmarse en papel y devolverse al profesor jefe.",
                requiereFirmaFisica = true
            ),
            tipo = TipoComunicacion.SALIDA_PEDAGOGICA, alcance = AlcanceEnvio.COLEGIO_COMPLETO,
            requiereFirmaFisica = true,
            autorId = "U1", autorNombre = "Inspectoría General (demo)", fechaHora = "2026-09-15 09:30:00"
        ),
        Comunicacion(
            id = "C3", asunto = "Suspensión de clases por capacitación",
            cuerpo = "Se informa que el lunes no habrá clases por jornada de capacitación docente.",
            tipo = TipoComunicacion.INSTITUCIONAL, alcance = AlcanceEnvio.COLEGIO_COMPLETO,
            autorId = "U1", autorNombre = "Inspectoría General (demo)", fechaHora = "2026-10-05 08:00:00"
        ),
    )

    val envios: List<EnvioDestinatario> = buildList {
        // C1: curso 4° Básico A (AP1, AP2, AP4) por el canal preferente de cada uno.
        add(envio("EV1", "C1", "AP1", CanalDespacho.EMAIL, EstadoEnvio.DESPACHADO))
        add(envio("EV2", "C1", "AP2", CanalDespacho.WHATSAPP, EstadoEnvio.DESPACHADO))
        add(envio("EV3", "C1", "AP4", CanalDespacho.SMS, EstadoEnvio.FALLIDO))
        // C2: autorización con firma física, todos por correo.
        apoderados.forEachIndexed { i, ap ->
            add(envio("EV${4 + i}", "C2", ap.id, CanalDespacho.EMAIL, EstadoEnvio.DESPACHADO))
        }
        // C3: colegio completo por el canal preferente de cada apoderado.
        apoderados.forEachIndexed { i, ap ->
            val estado = if (ap.id == "AP4") EstadoEnvio.FALLIDO else EstadoEnvio.DESPACHADO
            add(envio("EV${10 + i}", "C3", ap.id, ap.canalPreferente, estado))
        }
    }

    private fun envio(
        id: String,
        comunicacionId: String,
        apoderadoId: String,
        canal: CanalDespacho,
        estado: EstadoEnvio
    ) = EnvioDestinatario(
        id = id,
        comunicacionId = comunicacionId,
        apoderadoId = apoderadoId,
        canal = canal,
        estado = estado,
        detalle = if (estado == EstadoEnvio.DESPACHADO) {
            "Despachado (simulado) por ${canal.etiqueta}"
        } else {
            "Teléfono inválido o ausente para ${canal.etiqueta}"
        }
    )
}
