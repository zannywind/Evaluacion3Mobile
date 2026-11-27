package com.mpaznotifica.app.data.local

import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario

// Conversión entidad (tabla) <-> modelo de la aplicación.

fun UsuarioEntity.aModelo() = Usuario(id, nombre, usuario, claveHash, Perfil.valueOf(perfil), cursoAsignado)

fun Usuario.aEntidad() = UsuarioEntity(id, nombre, usuario, claveHash, perfil.name, cursoAsignado)

fun ApoderadoEntity.aModelo(pupilosIds: List<String>) =
    Apoderado(id, nombreCompleto, correo, telefono, CanalDespacho.valueOf(canalPreferente), pupilosIds)

fun Apoderado.aEntidad() = ApoderadoEntity(id, nombreCompleto, correo, telefono, canalPreferente.name)

fun ComunicacionEntity.aModelo() = Comunicacion(
    id = id,
    asunto = asunto,
    cuerpo = cuerpo,
    tipo = TipoComunicacion.valueOf(tipo),
    alcance = AlcanceEnvio.valueOf(alcance),
    destinoReferencia = destinoReferencia,
    adjuntoPdfNombre = adjuntoPdfNombre,
    requiereFirmaFisica = requiereFirmaFisica,
    autorId = autorId,
    autorNombre = autorNombre,
    fechaHora = fechaHora
)

fun Comunicacion.aEntidad() = ComunicacionEntity(
    id = id,
    asunto = asunto,
    cuerpo = cuerpo,
    tipo = tipo.name,
    alcance = alcance.name,
    destinoReferencia = destinoReferencia,
    adjuntoPdfNombre = adjuntoPdfNombre,
    requiereFirmaFisica = requiereFirmaFisica,
    autorId = autorId,
    autorNombre = autorNombre,
    fechaHora = fechaHora
)

fun EnvioEntity.aModelo() = EnvioDestinatario(
    id = id,
    comunicacionId = comunicacionId,
    apoderadoId = apoderadoId,
    canal = CanalDespacho.valueOf(canal),
    estado = EstadoEnvio.valueOf(estado),
    detalle = detalle
)

fun EnvioDestinatario.aEntidad() = EnvioEntity(
    id = id,
    comunicacionId = comunicacionId,
    apoderadoId = apoderadoId,
    canal = canal.name,
    estado = estado.name,
    detalle = detalle
)
