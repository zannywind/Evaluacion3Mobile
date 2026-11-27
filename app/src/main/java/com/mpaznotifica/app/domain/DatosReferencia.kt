package com.mpaznotifica.app.domain

import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.Estudiante
import com.mpaznotifica.app.model.Taller

/** Fotografía de los datos de referencia (base sintética) sobre los que operan las reglas. */
data class DatosReferencia(
    val cursos: List<String> = emptyList(),
    val talleres: List<Taller> = emptyList(),
    val estudiantes: List<Estudiante> = emptyList(),
    val apoderados: List<Apoderado> = emptyList()
) {
    fun apoderadoPorId(id: String): Apoderado? = apoderados.find { it.id == id }

    /** Pupilos del apoderado con su curso, p. ej. "Estudiante Ficticio 1 (4° Básico A)". */
    fun nombresPupilos(apoderado: Apoderado): List<String> =
        estudiantes
            .filter { it.id in apoderado.pupilosIds }
            .map { "${it.nombreReferencial} (${it.curso})" }
}
