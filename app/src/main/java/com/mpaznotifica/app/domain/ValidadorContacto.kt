package com.mpaznotifica.app.domain

import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho

/**
 * Validación del dato de contacto que exige cada canal. La usan el servicio de mensajería
 * simulado (para decidir DESPACHADO/FALLIDO) y la configuración del canal preferente (RF-13).
 */
object ValidadorContacto {

    private val regexCorreo = Regex("""^[\w.+-]+@[\w-]+(\.[\w-]+)+$""")

    /** Celular chileno en formato internacional: +56 9 y ocho dígitos. */
    private val regexTelefono = Regex("""^\+569\d{8}$""")

    fun correoValido(correo: String): Boolean = regexCorreo.matches(correo.trim())

    fun telefonoValido(telefono: String): Boolean = regexTelefono.matches(telefono.trim())

    /** El correo sirve al canal EMAIL; el teléfono, a SMS y WHATSAPP. */
    fun contactoValido(canal: CanalDespacho, contacto: String): Boolean = when (canal) {
        CanalDespacho.EMAIL -> correoValido(contacto)
        CanalDespacho.SMS, CanalDespacho.WHATSAPP -> telefonoValido(contacto)
    }

    fun contactoDe(apoderado: Apoderado, canal: CanalDespacho): String = when (canal) {
        CanalDespacho.EMAIL -> apoderado.correo
        CanalDespacho.SMS, CanalDespacho.WHATSAPP -> apoderado.telefono
    }

    fun valido(apoderado: Apoderado, canal: CanalDespacho): Boolean =
        contactoValido(canal, contactoDe(apoderado, canal))

    /** Canales por los que se puede contactar al apoderado con los datos que tiene. */
    fun canalesDisponibles(apoderado: Apoderado): List<CanalDespacho> =
        CanalDespacho.entries.filter { valido(apoderado, it) }

    fun motivoInvalido(canal: CanalDespacho): String = when (canal) {
        CanalDespacho.EMAIL -> "Correo electrónico inválido o ausente"
        CanalDespacho.SMS, CanalDespacho.WHATSAPP -> "Teléfono inválido o ausente para ${canal.etiqueta}"
    }
}
