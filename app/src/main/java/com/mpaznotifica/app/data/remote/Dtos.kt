package com.mpaznotifica.app.data.remote

/*
 * Objetos de transferencia (JSON del backend). Los nombres de campo coinciden letra por letra con
 * backend/data/seed.json y con las respuestas de docs/API.md. Todos los campos tienen valor por
 * defecto: así Gson usa el constructor sin argumentos y un campo ausente en el JSON queda con un
 * valor neutro que Mapeadores.kt detecta y rechaza, en lugar de un null en un tipo no nulo.
 */

data class SaludDto(
    val estado: String = "",
    val version: String = "",
    val hora: String = ""
)

data class LoginRequestDto(
    val usuario: String = "",
    val clave: String = ""
)

/** Usuario SIN clave ni hash: el servidor nunca los entrega. */
data class UsuarioDto(
    val id: String = "",
    val nombre: String = "",
    val usuario: String = "",
    val perfil: String = "",
    val cursoAsignado: String? = null
)

data class LoginResponseDto(
    val token: String = "",
    val usuario: UsuarioDto? = null
)

data class TallerDto(
    val id: String = "",
    val nombre: String = ""
)

data class EstudianteDto(
    val id: String = "",
    val nombreReferencial: String = "",
    val curso: String = "",
    val apoderadoId: String = "",
    val talleresIds: List<String> = emptyList()
)

data class ApoderadoDto(
    val id: String = "",
    val nombreCompleto: String = "",
    val correo: String = "",
    val telefono: String = "",
    val canalPreferente: String = "",
    val pupilosIds: List<String> = emptyList()
)

data class CanalRequestDto(
    val canal: String = ""
)

data class ComunicacionDto(
    val id: String = "",
    val asunto: String = "",
    val cuerpo: String = "",
    val tipo: String = "",
    val alcance: String = "",
    val destinoReferencia: String? = null,
    val adjuntoPdfNombre: String? = null,
    val requiereFirmaFisica: Boolean = false,
    val autorId: String = "",
    val autorNombre: String = "",
    val fechaHora: String = ""
)

data class EnvioDto(
    val id: String = "",
    val comunicacionId: String = "",
    val apoderadoId: String = "",
    val canal: String = "",
    val estado: String = "",
    val detalle: String = ""
)

/** Resultado del despacho de un destinatario que la app informa al registrar el comunicado. */
data class NuevoEnvioDto(
    val apoderadoId: String = "",
    val canal: String = "",
    val estado: String = "",
    val detalle: String = ""
)

/** Cuerpo de POST /comunicaciones. [destino] es el nombre del curso, taller o apoderado. */
data class NuevaComunicacionDto(
    val id: String = "",
    val asunto: String = "",
    val cuerpo: String = "",
    val tipo: String = "",
    val alcance: String = "",
    val destino: String? = null,
    val adjuntoPdfNombre: String? = null,
    val requiereFirmaFisica: Boolean = false,
    val envios: List<NuevoEnvioDto> = emptyList()
)

data class ResumenDto(
    val despachados: Int = 0,
    val fallidos: Int = 0,
    val requiereFirmaFisica: Boolean = false
)

/** Respuesta de POST /comunicaciones y de GET /comunicaciones/{id}. */
data class ComunicacionRegistradaDto(
    val comunicacion: ComunicacionDto? = null,
    val envios: List<EnvioDto> = emptyList(),
    val resumen: ResumenDto? = null
)
