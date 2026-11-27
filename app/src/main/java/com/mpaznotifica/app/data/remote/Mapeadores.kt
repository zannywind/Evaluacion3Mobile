package com.mpaznotifica.app.data.remote

import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.ResumenEnvio
import com.mpaznotifica.app.model.Taller
import com.mpaznotifica.app.model.Usuario

/*
 * EXPERIENCIA 3 - Parte 2 (TODO E3-4): mapeadores DTO <-> modelo de dominio.
 *
 * Los DTO (Dtos.kt) son el contrato con el backend y vienen completos. Aquí se implementa la
 * conversión hacia las clases del dominio (paquete model/) y, en sentido contrario, hacia el cuerpo
 * de POST /comunicaciones.
 *
 * Criterio común: una respuesta incompleta o con un valor de enumeración desconocido NUNCA debe
 * llegar a Room ni a la interfaz. Se rechaza con [RespuestaInvalidaException] (que
 * [ManejadorErrores.desdeExcepcion] convierte en "formato inesperado"). Para eso se entregan dos
 * ayudas ya implementadas: [requerido] y [enumDe].
 */

/** Devuelve [valor] si no está vacío; si lo está lanza [RespuestaInvalidaException] con el nombre del campo. */
internal fun requerido(valor: String, campo: String): String {
    if (valor.isBlank()) throw RespuestaInvalidaException("Falta el campo '$campo'")
    return valor
}

/** Convierte el texto del servidor a la enumeración [E]; un valor desconocido lanza [RespuestaInvalidaException]. */
internal inline fun <reified E : Enum<E>> enumDe(valor: String, campo: String): E =
    enumValues<E>().firstOrNull { it.name == valor }
        ?: throw RespuestaInvalidaException("Valor desconocido '$valor' en '$campo'")

// ---------------------------------------------------------------- usuario

/**
 * TODO(E3-4a): [UsuarioDto] -> [Usuario]. Campos obligatorios: id, nombre, usuario y perfil
 * (enumeración `Perfil`). `cursoAsignado` vacío o en blanco = null. El servidor NUNCA entrega la
 * clave: `claveHash` queda en "" (la caché conserva el hash local, ver RoomCache).
 */
fun UsuarioDto.aDominio(): Usuario =
    TODO("E3-4a: validar con requerido()/enumDe<Perfil>() y construir Usuario(claveHash = \"\")")

// ---------------------------------------------------------------- referencia

/** TODO(E3-4b): [TallerDto] -> [Taller]. id y nombre obligatorios. */
fun TallerDto.aDominio(): Taller =
    TODO("E3-4b: Taller(requerido(id, \"id\"), requerido(nombre, \"nombre\"))")

/** TODO(E3-4c): [EstudianteDto] -> [Estudiante]. id, nombreReferencial y curso obligatorios; talleresIds se copia tal cual. */
fun EstudianteDto.aDominio(): Estudiante =
    TODO("E3-4c: validar y construir Estudiante")

/**
 * TODO(E3-4d): [ApoderadoDto] -> [Apoderado]. id y nombreCompleto obligatorios; `canalPreferente`
 * se convierte con `enumDe<CanalDespacho>`; correo y telefono pueden venir vacíos (contacto
 * incompleto es un dato válido: el despacho a ese canal fallará, RF-08).
 */
fun ApoderadoDto.aDominio(): Apoderado =
    TODO("E3-4d: validar y construir Apoderado")

// ---------------------------------------------------------------- bitácora

/**
 * TODO(E3-4e): [ComunicacionDto] -> [Comunicacion]. Obligatorios: id, asunto, cuerpo, tipo
 * (`TipoComunicacion`), alcance (`AlcanceEnvio`), autorId, autorNombre y fechaHora.
 * `destinoReferencia` y `adjuntoPdfNombre` vacíos = null. El autor y la fecha/hora los fija el
 * servidor (RF-11): se respetan tal como llegan.
 */
fun ComunicacionDto.aDominio(): Comunicacion =
    TODO("E3-4e: validar y construir Comunicacion")

/** TODO(E3-4f): [EnvioDto] -> [EnvioDestinatario]. Obligatorios: id, comunicacionId, apoderadoId, canal (`CanalDespacho`) y estado (`EstadoEnvio`). */
fun EnvioDto.aDominio(): EnvioDestinatario =
    TODO("E3-4f: validar y construir EnvioDestinatario")

/** TODO(E3-4g): [ResumenDto] -> [ResumenEnvio] (despachados, fallidos, requiereFirmaFisica). */
fun ResumenDto.aDominio(): ResumenEnvio =
    TODO("E3-4g: ResumenEnvio(despachados, fallidos, requiereFirmaFisica)")

// ---------------------------------------------------------------- hacia el servidor

/**
 * TODO(E3-4h): [EnvioDestinatario] -> [NuevoEnvioDto] para el cuerpo de POST /comunicaciones
 * (apoderadoId, canal.name, estado.name, detalle).
 */
fun EnvioDestinatario.aDto(): NuevoEnvioDto =
    TODO("E3-4h: NuevoEnvioDto(apoderadoId, canal.name, estado.name, detalle)")

/**
 * TODO(E3-4i): [Comunicacion] -> [NuevaComunicacionDto] (cuerpo de POST /comunicaciones).
 * Criterios: enumeraciones como `.name`; `destino` es el nombre elegido en el formulario (curso,
 * taller o apoderado), NO `destinoReferencia`, que vale null para colegio completo; `envios` se
 * mapea con [aDto]. El cuerpo de una autorización con firma física ya incluye el aviso (RF-12):
 * no se vuelve a modificar aquí.
 */
fun Comunicacion.aNuevaDto(destino: String?, envios: List<EnvioDestinatario>): NuevaComunicacionDto =
    TODO("E3-4i: NuevaComunicacionDto(id, asunto, cuerpo, tipo.name, alcance.name, destino, adjuntoPdfNombre, requiereFirmaFisica, envios.map { it.aDto() })")

/** Comunicado registrado por el servidor, listo para guardarse en Room (ya implementada). */
data class ComunicacionRegistrada(
    val comunicacion: Comunicacion,
    val envios: List<EnvioDestinatario>,
    val resumen: ResumenEnvio
)

/**
 * TODO(E3-4j): [ComunicacionRegistradaDto] -> [ComunicacionRegistrada]. Si falta `comunicacion`
 * lanza [RespuestaInvalidaException]. Si falta `resumen`, se calcula con `ResumenEnvio.de(envios,
 * comunicacion.requiereFirmaFisica)`.
 */
fun ComunicacionRegistradaDto.aDominio(): ComunicacionRegistrada =
    TODO("E3-4j: mapear comunicacion, envios y resumen")
