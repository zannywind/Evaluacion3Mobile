package com.mpaznotifica.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mpaznotifica.app.BuildConfig
import com.mpaznotifica.app.data.local.AppDatabase
import com.mpaznotifica.app.data.remote.ApiClient
import com.mpaznotifica.app.data.remote.ApiResult
import com.mpaznotifica.app.data.remote.ConfiguracionServidor
import com.mpaznotifica.app.data.remote.CausaError
import com.mpaznotifica.app.data.remote.Despachador
import com.mpaznotifica.app.data.remote.FuenteRemota
import com.mpaznotifica.app.data.remote.PreferenciasUrl
import com.mpaznotifica.app.data.remote.SesionApi
import com.mpaznotifica.app.data.remote.UrlServidor
import com.mpaznotifica.app.data.repository.FuenteLocal
import com.mpaznotifica.app.data.repository.FuenteServidor
import com.mpaznotifica.app.data.repository.MpazRepository
import com.mpaznotifica.app.data.repository.PreferenciasModo
import com.mpaznotifica.app.data.repository.RoomCache
import com.mpaznotifica.app.data.repository.SelectorFuente
import com.mpaznotifica.app.domain.Credenciales
import com.mpaznotifica.app.domain.DatosReferencia
import com.mpaznotifica.app.domain.Fechas
import com.mpaznotifica.app.domain.FiltrosHistorial
import com.mpaznotifica.app.domain.ReglasComunicado
import com.mpaznotifica.app.domain.ReglasHistorial
import com.mpaznotifica.app.domain.ValidadorContacto
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.Destinatario
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.ErroresLogin
import com.mpaznotifica.app.model.EstadoDatos
import com.mpaznotifica.app.model.EstadoEnvioUi
import com.mpaznotifica.app.model.PruebaServidor
import com.mpaznotifica.app.model.ResumenEnvio
import com.mpaznotifica.app.model.SolicitudComunicado
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario
import com.mpaznotifica.app.nativo.ArchivosPdf
import com.mpaznotifica.app.nativo.EstadoRed
import com.mpaznotifica.app.nativo.Notificador
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel único, compartido por todas las pantallas (se crea una sola vez en AppNavigation).
 * Orquesta el flujo (sesión, envío, historial, canal preferente); las reglas de negocio viven en
 * el paquete domain (probadas en JVM) y los datos llegan por una única interfaz (SelectorFuente).
 * Las pantallas solo dibujan el estado (StateFlow + collectAsState) y llaman a estas funciones.
 *
 * Dos modos, que se eligen EN TIEMPO DE EJECUCIÓN con el interruptor "Modo local (sin servidor)"
 * (la preferencia se consulta en cada operación):
 *  - modo local: solo Room (base propia) + despacho simulado, con las mismas reglas del dominio y
 *    sin servidor. Es el respaldo y el valor de partida.
 *  - modo servidor: login, sincronización, envío y canal preferente pasan por el backend (la API es
 *    la fuente de verdad y Room es la caché que las pantallas observan). Depende de los TODO(E3-n).
 * BuildConfig.USAR_API (gradle.properties) solo define el valor INICIAL del interruptor:
 * false = modo local, true = modo servidor. Después manda lo que el usuario elija.
 */
class MpazNotificaViewModel(application: Application) : AndroidViewModel(application) {

    // Piezas del modo servidor (se crean sin tocar la red; solo se usan en ese modo).
    private val configuracion = ConfiguracionServidor(PreferenciasUrl(application), BuildConfig.API_BASE_URL)
    private val sesionApi = SesionApi()
    private val cliente = ApiClient(configuracion, sesionApi)

    /** Preferencia "modo local / modo servidor". USAR_API solo da el valor inicial (true = servidor). */
    private val preferenciaModo = PreferenciasModo(application, inicialLocal = !BuildConfig.USAR_API)

    /**
     * Única interfaz de datos: modo local = base Room propia (mpaz_local.db) con las reglas del
     * dominio; modo servidor = MpazRepository (API + caché Room en mpaz_notifica.db). Las dos
     * bases son independientes: lo creado en modo local no se sincroniza con el servidor.
     */
    private val repositorio = SelectorFuente(
        preferencia = preferenciaModo,
        local = FuenteLocal(RoomCache(AppDatabase.obtenerLocal(application))),
        servidor = FuenteServidor(
            MpazRepository(AppDatabase.obtener(application), FuenteRemota { cliente.api() })
        ) { cliente.mensajeria() }
    )
    private val notificador = Notificador(application)

    init {
        viewModelScope.launch { repositorio.asegurarDatosIniciales() }
    }

    /** Mensaje para el usuario cuando no hay respuesta del servidor, según haya o no red en el dispositivo. */
    private fun textoSinConexion(resultado: ApiResult.SinConexion): String =
        if (!EstadoRed.disponible(getApplication())) {
            "El dispositivo no tiene conexión a internet."
        } else {
            resultado.mensaje
        }

    private fun <T> Flow<T>.comoEstado(inicial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, inicial)

    // =====================================================================
    // Datos de referencia y bitácora (se leen de Room, que la API mantiene al día)
    // =====================================================================

    /** Usuarios emisores, para el filtro "Emisor" del historial. */
    val emisores: StateFlow<List<Usuario>> = repositorio.usuarios.comoEstado(emptyList())

    val datos: StateFlow<DatosReferencia> =
        combine(repositorio.talleres, repositorio.estudiantes, repositorio.apoderados) { talleres, estudiantes, apoderados ->
            DatosReferencia(
                cursos = estudiantes.map { it.curso }.distinct().sorted(),
                talleres = talleres,
                estudiantes = estudiantes,
                apoderados = apoderados
            )
        }.comoEstado(DatosReferencia())

    val comunicados: StateFlow<List<Comunicacion>> = repositorio.comunicaciones.comoEstado(emptyList())

    val envios: StateFlow<List<EnvioDestinatario>> = repositorio.envios.comoEstado(emptyList())

    /** Resumen de despachos por comunicado, para el historial y el detalle. */
    val resumenes: StateFlow<Map<String, ResumenEnvio>> =
        combine(comunicados, envios) { lista, todos ->
            val porComunicado = todos.groupBy { it.comunicacionId }
            lista.associate { it.id to ResumenEnvio.de(porComunicado[it.id].orEmpty(), it.requiereFirmaFisica) }
        }.comoEstado(emptyMap())

    /** Avisos breves para la interfaz (Snackbar). */
    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    // =====================================================================
    // Modo local / modo servidor
    // =====================================================================

    /** true = modo local (solo Room, sin servidor); false = modo servidor. Alimenta la franja de modo. */
    val modoLocal: StateFlow<Boolean> = preferenciaModo.modoLocal

    /**
     * Cambia de modo en caliente. La sesión pertenece a un modo, así que si hay una abierta se
     * cierra (se vuelve al login). Los datos locales y los del servidor son independientes: lo
     * creado en modo local no se sincroniza con el servidor.
     */
    fun cambiarModo(local: Boolean) {
        if (preferenciaModo.esLocal() == local) return
        if (_envioUi.value is EstadoEnvioUi.Enviando) {
            _mensajes.tryEmit("Espera a que termine el envío para cambiar de modo")
            return
        }
        if (_usuarioActivo.value != null) logout()
        preferenciaModo.cambiar(local)
        if (!local) _urlServidor.value = direccionActual()
        _estadoDatos.value = EstadoDatos.Inactivo
        _sinSesionRemota.value = false
        _pruebaServidor.value = PruebaServidor.Inactiva
        viewModelScope.launch { repositorio.asegurarDatosIniciales() }
    }

    /** Solo modo local: borra lo creado en modo local y restaura los datos de ejemplo. */
    fun restablecerDatosLocales(alTerminar: (Boolean) -> Unit = {}) {
        if (!repositorio.esLocal) {
            alTerminar(false)
            return
        }
        viewModelScope.launch {
            val exito = try {
                repositorio.restablecer() is ApiResult.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            _mensajes.tryEmit(
                if (exito) "Datos locales restablecidos" else "No se pudieron restablecer los datos locales"
            )
            alTerminar(exito)
        }
    }

    // =====================================================================
    // Servidor (dirección de la API, configurable sin recompilar)
    // =====================================================================

    /** Dirección en uso. Mientras ConfiguracionServidor (TODO E3-9b) no esté implementada, se muestra la de compilación. */
    private fun direccionActual(): String =
        try {
            configuracion.urlBase()
        } catch (e: NotImplementedError) {
            BuildConfig.API_BASE_URL
        }

    private val _urlServidor = MutableStateFlow(if (preferenciaModo.esLocal()) BuildConfig.API_BASE_URL else direccionActual())
    val urlServidor: StateFlow<String> = _urlServidor.asStateFlow()

    private val _pruebaServidor = MutableStateFlow<PruebaServidor>(PruebaServidor.Inactiva)
    val pruebaServidor: StateFlow<PruebaServidor> = _pruebaServidor.asStateFlow()

    fun validarServidor(texto: String): String? = UrlServidor.validar(texto)

    /** Guarda la dirección (si es válida) y devuelve el error, o null si quedó guardada. */
    fun guardarServidor(texto: String): String? {
        val error = configuracion.guardar(texto)
        if (error == null) {
            _urlServidor.value = configuracion.urlBase()
            _pruebaServidor.value = PruebaServidor.Inactiva
        }
        return error
    }

    /** Vuelve a la dirección de compilación (API_BASE_URL de gradle.properties). */
    fun restaurarServidor() {
        configuracion.restaurar()
        _urlServidor.value = configuracion.urlBase()
        _pruebaServidor.value = PruebaServidor.Inactiva
    }

    fun limpiarPruebaServidor() {
        _pruebaServidor.value = PruebaServidor.Inactiva
    }

    /** "Probar conexión": consulta GET /health de la dirección escrita, antes de guardarla. */
    fun probarServidor(texto: String) {
        if (_pruebaServidor.value is PruebaServidor.Probando) return
        _pruebaServidor.value = PruebaServidor.Probando
        viewModelScope.launch {
            _pruebaServidor.value = when (val resultado = cliente.probarConexion(texto)) {
                is ApiResult.Success -> PruebaServidor.Correcta("Conexión correcta con el servidor de MPAZ Notifica")
                is ApiResult.SinConexion -> PruebaServidor.Fallida(textoSinConexion(resultado))
                is ApiResult.Error -> PruebaServidor.Fallida(resultado.mensaje)
            }
        }
    }

    // =====================================================================
    // Sincronización con el servidor (API -> Room -> pantallas)
    // =====================================================================

    private val _estadoDatos = MutableStateFlow<EstadoDatos>(EstadoDatos.Inactivo)
    val estadoDatos: StateFlow<EstadoDatos> = _estadoDatos.asStateFlow()

    /** true si se ingresó sin conexión: se ven los datos guardados, pero no se puede enviar ni editar. */
    private val _sinSesionRemota = MutableStateFlow(false)
    val sinSesionRemota: StateFlow<Boolean> = _sinSesionRemota.asStateFlow()

    private var sincronizando = false

    /** Descarga del servidor la referencia y la bitácora y actualiza la caché (Room). */
    fun sincronizar() {
        if (sincronizando || _usuarioActivo.value == null) return
        if (repositorio.esLocal) {
            // Modo local: no hay servidor con el que sincronizar.
            _estadoDatos.value = EstadoDatos.Inactivo
            return
        }
        if (!sesionApi.activa) {
            _estadoDatos.value = EstadoDatos.SinConexion(MSG_MODO_SIN_CONEXION)
            return
        }
        sincronizando = true
        _estadoDatos.value = EstadoDatos.Cargando
        viewModelScope.launch {
            try {
                when (val resultado = repositorio.sincronizar()) {
                    is ApiResult.Success -> _estadoDatos.value = EstadoDatos.Actualizado(Fechas.legible(Fechas.ahora()))
                    is ApiResult.SinConexion -> _estadoDatos.value = EstadoDatos.SinConexion(
                        textoSinConexion(resultado) + " Se muestran los últimos datos guardados."
                    )
                    is ApiResult.Error -> {
                        if (resultado.causa == CausaError.NO_AUTORIZADO) {
                            sesionExpirada()
                        } else {
                            _estadoDatos.value = EstadoDatos.Error(resultado.mensaje)
                        }
                    }
                }
            } finally {
                sincronizando = false
            }
        }
    }

    /** El servidor rechazó el token (venció o se reinició el servidor): vuelve al login con aviso. */
    private fun sesionExpirada() {
        logout()
        _errorCredenciales.value = "Tu sesión expiró. Ingresa nuevamente."
    }

    // =====================================================================
    // Autenticación y perfiles (RF-01)
    // =====================================================================

    private val _usuarioActivo = MutableStateFlow<Usuario?>(null)
    val usuarioActivo: StateFlow<Usuario?> = _usuarioActivo.asStateFlow()

    private val _erroresLogin = MutableStateFlow(ErroresLogin())
    val erroresLogin: StateFlow<ErroresLogin> = _erroresLogin.asStateFlow()

    /** Mensaje general de credenciales incorrectas (no pertenece a un campo en particular). */
    private val _errorCredenciales = MutableStateFlow<String?>(null)
    val errorCredenciales: StateFlow<String?> = _errorCredenciales.asStateFlow()

    private val _ingresando = MutableStateFlow(false)
    val ingresando: StateFlow<Boolean> = _ingresando.asStateFlow()

    fun limpiarErroresLogin() {
        _erroresLogin.value = ErroresLogin()
        _errorCredenciales.value = null
    }

    /**
     * Valida los campos y luego inicia sesión según el modo:
     *  - Modo local: contrasta el hash de la clave (misma sal) con los usuarios de la base local.
     *  - Modo servidor: inicia sesión en el servidor (POST /auth/login). Si no hay conexión,
     *    respaldo: se contrastan con los usuarios guardados en Room y se ingresa en modo sin
     *    conexión (solo lectura de los datos guardados).
     */
    fun iniciarSesion(correo: String, clave: String, onExito: () -> Unit) {
        if (_ingresando.value) return
        val errores = Credenciales.validar(correo, clave)
        _erroresLogin.value = errores
        _errorCredenciales.value = null
        if (errores.hayErrores) return
        _ingresando.value = true
        viewModelScope.launch {
            try {
                repositorio.asegurarDatosIniciales()
                // El modo se lee ahora (cada operación) y vale para toda esta operación.
                val local = repositorio.esLocal
                when (val resultado = repositorio.iniciarSesion(correo, clave)) {
                    is ApiResult.Success -> {
                        if (local) sesionApi.cerrar() else sesionApi.iniciar(resultado.datos.token)
                        _sinSesionRemota.value = false
                        _estadoDatos.value = EstadoDatos.Inactivo
                        _usuarioActivo.value = resultado.datos.usuario
                        onExito()
                        sincronizar()
                    }
                    is ApiResult.Error -> {
                        _errorCredenciales.value =
                            if (resultado.causa == CausaError.NO_AUTORIZADO) "Correo o clave incorrectos" else resultado.mensaje
                    }
                    is ApiResult.SinConexion -> ingresarSinConexion(correo, clave, textoSinConexion(resultado), onExito)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorCredenciales.value =
                    if (repositorio.esLocal) "No se pudo acceder a los datos locales. Intenta nuevamente."
                    else "No se pudo iniciar sesión. Intenta nuevamente."
            } finally {
                _ingresando.value = false
            }
        }
    }

    private suspend fun ingresarSinConexion(correo: String, clave: String, motivo: String, onExito: () -> Unit) {
        val encontrado = repositorio.buscarUsuario(correo)
        if (encontrado != null && encontrado.claveHash.isNotEmpty() && Credenciales.coincide(clave, encontrado.claveHash)) {
            sesionApi.cerrar()
            _sinSesionRemota.value = true
            _estadoDatos.value = EstadoDatos.SinConexion(MSG_MODO_SIN_CONEXION)
            _usuarioActivo.value = encontrado
            onExito()
        } else {
            _errorCredenciales.value = motivo
        }
    }

    fun logout() {
        val token = sesionApi.token
        val fuente = repositorio.actual()
        sesionApi.cerrar()
        if (token != null) viewModelScope.launch { fuente.cerrarSesion(token) }
        _sinSesionRemota.value = false
        _estadoDatos.value = EstadoDatos.Inactivo
        pendienteRegistro = null
        _usuarioActivo.value = null
        limpiarErroresLogin()
        _envioUi.value = EstadoEnvioUi.Inactivo
        _filtros.value = FiltrosHistorial()
    }

    // =====================================================================
    // Redacción y envío (RF-02 a RF-08, RF-12, RN-01, RN-02)
    // =====================================================================

    fun tiposPermitidos(usuario: Usuario?): List<TipoComunicacion> = ReglasComunicado.tiposPermitidos(usuario)

    fun alcancesPermitidos(usuario: Usuario?): List<AlcanceEnvio> = ReglasComunicado.alcancesPermitidos(usuario)

    fun destinosPara(alcance: AlcanceEnvio?, usuario: Usuario?): List<String> =
        ReglasComunicado.destinosPara(alcance, usuario, datos.value)

    fun validarComunicado(
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion?,
        alcance: AlcanceEnvio?,
        destino: String?
    ): ErroresComunicado =
        ReglasComunicado.validar(asunto, cuerpo, tipo, alcance, destino, _usuarioActivo.value, datos.value)

    /** Cantidad de apoderados que recibirían el comunicado con el alcance y destino elegidos. */
    fun contarDestinatarios(alcance: AlcanceEnvio?, destino: String?): Int =
        if (alcance == null) 0 else ReglasComunicado.resolverApoderados(alcance, destino, datos.value).size

    /** true si falta conceder el permiso de notificaciones (Android 13+) para avisar el resultado. */
    fun necesitaPermisoNotificaciones(): Boolean = notificador.requierePermiso() && !notificador.tienePermiso()

    private val _envioUi = MutableStateFlow<EstadoEnvioUi>(EstadoEnvioUi.Inactivo)
    val envioUi: StateFlow<EstadoEnvioUi> = _envioUi.asStateFlow()

    fun reiniciarEnvio() {
        if (_envioUi.value !is EstadoEnvioUi.Enviando) {
            pendienteRegistro = null
            _envioUi.value = EstadoEnvioUi.Inactivo
        }
    }

    /** Comunicado ya despachado cuyo registro en el servidor falló (red): se puede reintentar sin volver a despachar. */
    private class PendienteRegistro(
        val comunicacion: Comunicacion,
        val destino: String?,
        val envios: List<EnvioDestinatario>
    )

    private var pendienteRegistro: PendienteRegistro? = null

    /**
     * RF-07, RF-08, RF-11, RF-12, RN-02: vuelve a validar (la pantalla no es la única barrera),
     * resuelve los destinatarios, despacha por el canal de cada uno y registra el comunicado
     * inmutable con el estado de cada envío. Modo local: servicios mock y Room (con las mismas
     * reglas del dominio). Modo servidor: POST /mensajes por destinatario y POST /comunicaciones (el
     * servidor vuelve a validar las reglas del perfil); la caché solo se actualiza cuando él
     * confirma. Las autorizaciones con firma física se informan siempre por correo.
     */
    fun enviarComunicado(solicitud: SolicitudComunicado) {
        if (_envioUi.value is EstadoEnvioUi.Enviando) return
        val autor = _usuarioActivo.value
        if (autor == null) {
            _envioUi.value = EstadoEnvioUi.Error(ReglasComunicado.MSG_SIN_SESION)
            return
        }
        if (!repositorio.esLocal && !sesionApi.activa) {
            _envioUi.value = EstadoEnvioUi.Error(MSG_REQUIERE_CONEXION)
            return
        }
        val datosActuales = datos.value
        val errores = ReglasComunicado.validar(
            solicitud.asunto, solicitud.cuerpo, solicitud.tipo, solicitud.alcance, solicitud.destino, autor, datosActuales
        )
        if (errores.hayErrores) {
            _envioUi.value = EstadoEnvioUi.Error("El comunicado no cumple las reglas de tu perfil. Revisa el formulario.")
            return
        }
        val destinatarios = ReglasComunicado
            .resolverApoderados(solicitud.alcance, solicitud.destino, datosActuales)
            .map { Destinatario(it, ReglasComunicado.canalDe(it, solicitud.requiereFirmaFisica)) }
        if (destinatarios.isEmpty()) {
            _envioUi.value = EstadoEnvioUi.Error(ReglasComunicado.MSG_SIN_DESTINATARIOS)
            return
        }
        _envioUi.value = EstadoEnvioUi.Enviando(0, destinatarios.size)
        viewModelScope.launch {
            try {
                val comunicacionId = UUID.randomUUID().toString()
                val nombreAdjunto = solicitud.adjunto?.let {
                    ArchivosPdf.guardarCopia(getApplication<Application>(), it, comunicacionId)
                        ?: throw IllegalStateException("No se pudo leer el PDF adjunto")
                }
                val comunicacion = Comunicacion(
                    id = comunicacionId,
                    asunto = solicitud.asunto.trim(),
                    cuerpo = ReglasComunicado.cuerpoFinal(solicitud.cuerpo, solicitud.requiereFirmaFisica),
                    tipo = solicitud.tipo,
                    alcance = solicitud.alcance,
                    destinoReferencia = if (solicitud.alcance == AlcanceEnvio.COLEGIO_COMPLETO) null else solicitud.destino,
                    adjuntoPdfNombre = nombreAdjunto,
                    requiereFirmaFisica = solicitud.requiereFirmaFisica,
                    autorId = autor.id,
                    autorNombre = autor.nombre,
                    fechaHora = Fechas.ahora()
                )
                // Modo servidor: el despacho va al servidor (POST /mensajes); modo local: servicios mock.
                val resultados = Despachador(repositorio.mensajeria()).despachar(comunicacion, destinatarios) { procesados, total ->
                    _envioUi.value = EstadoEnvioUi.Enviando(procesados, total)
                }
                registrar(PendienteRegistro(comunicacion, solicitud.destino, resultados))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _envioUi.value = EstadoEnvioUi.Error(
                    if (repositorio.esLocal) "No se pudo registrar el comunicado. Intenta nuevamente."
                    else "No se pudo enviar el comunicado. Intenta nuevamente."
                )
            }
        }
    }

    /** Reintenta solo el registro en el servidor de un comunicado que ya se despachó. */
    fun reintentarRegistro() {
        val pendiente = pendienteRegistro ?: return
        if (_envioUi.value is EstadoEnvioUi.Enviando) return
        _envioUi.value = EstadoEnvioUi.Enviando(pendiente.envios.size, pendiente.envios.size)
        viewModelScope.launch {
            try {
                registrar(pendiente)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _envioUi.value = EstadoEnvioUi.Error("No se pudo registrar el comunicado. Intenta nuevamente.", puedeReintentar = true)
            }
        }
    }

    private suspend fun registrar(pendiente: PendienteRegistro) {
        val comunicacion = pendiente.comunicacion
        when (val resultado = repositorio.registrarEnvio(comunicacion, pendiente.destino, pendiente.envios)) {
            is ApiResult.Success -> {
                pendienteRegistro = null
                val registrada = resultado.datos
                _envioUi.value = EstadoEnvioUi.Listo(registrada.resumen)
                notificador.notificarEnvio(registrada.comunicacion.asunto, registrada.resumen)
            }
            is ApiResult.SinConexion -> {
                pendienteRegistro = pendiente
                _envioUi.value = EstadoEnvioUi.Error(
                    "Los mensajes se despacharon, pero no se pudo registrar el comunicado en el servidor: " +
                        textoSinConexion(resultado) + " Pulsa Reintentar para registrarlo.",
                    puedeReintentar = true
                )
            }
            is ApiResult.Error -> when {
                resultado.causa == CausaError.NO_AUTORIZADO -> {
                    pendienteRegistro = null
                    sesionExpirada()
                }
                // Un reintento cuya primera copia sí llegó al servidor: ya está registrado.
                resultado.causa == CausaError.CONFLICTO && resultado.campo == "id" -> {
                    pendienteRegistro = null
                    val resumen = ResumenEnvio.de(pendiente.envios, comunicacion.requiereFirmaFisica)
                    _envioUi.value = EstadoEnvioUi.Listo(resumen)
                    sincronizar()
                }
                else -> {
                    pendienteRegistro = null
                    _envioUi.value = EstadoEnvioUi.Error(resultado.mensaje)
                }
            }
        }
    }

    // =====================================================================
    // Historial y trazabilidad (RF-09, RF-10, RF-11)
    // =====================================================================

    private val _filtros = MutableStateFlow(FiltrosHistorial())
    val filtros: StateFlow<FiltrosHistorial> = _filtros.asStateFlow()

    /** Bitácora filtrada, del comunicado más reciente al más antiguo. */
    val historial: StateFlow<List<Comunicacion>> =
        combine(comunicados, _filtros) { lista, filtros -> ReglasHistorial.aplicar(lista, filtros) }
            .comoEstado(emptyList())

    /** Error del rango de fechas del filtro (null = rango válido). */
    val errorFechas: StateFlow<String?> =
        _filtros.map { ReglasHistorial.errorFechas(it.desde, it.hasta) }.comoEstado(null)

    fun filtrarPorTipo(tipo: TipoComunicacion?) {
        _filtros.value = _filtros.value.copy(tipo = tipo)
    }

    fun filtrarPorEmisor(emisorId: String?) {
        _filtros.value = _filtros.value.copy(emisorId = emisorId)
    }

    fun filtrarDesde(fecha: String?) {
        _filtros.value = _filtros.value.copy(desde = fecha)
    }

    fun filtrarHasta(fecha: String?) {
        _filtros.value = _filtros.value.copy(hasta = fecha)
    }

    fun limpiarFiltros() {
        _filtros.value = FiltrosHistorial()
    }

    fun comunicadoPorId(id: String?): Comunicacion? = comunicados.value.find { it.id == id }

    // =====================================================================
    // Canal preferente del apoderado (RF-13)
    // =====================================================================

    fun puedeConfigurarCanal(usuario: Usuario?): Boolean = ReglasComunicado.puedeConfigurarCanal(usuario)

    fun apoderadosVisibles(usuario: Usuario?): List<Apoderado> =
        ReglasComunicado.apoderadosVisibles(usuario, datos.value)

    /**
     * Guarda el canal preferente: en Room (modo local) o en el servidor (PATCH /apoderados/{id}/canal)
     * y, si lo acepta, en Room (modo servidor). Solo Inspectoría General puede cambiarlo y solo hacia un canal para el que el apoderado
     * tenga un dato de contacto válido; el servidor vuelve a comprobarlo.
     */
    fun actualizarCanalPreferente(apoderadoId: String, canal: CanalDespacho) {
        if (!puedeConfigurarCanal(_usuarioActivo.value)) {
            _mensajes.tryEmit("Solo Inspectoría General puede cambiar el canal preferente")
            return
        }
        val apoderado = datos.value.apoderadoPorId(apoderadoId)
        if (apoderado == null) {
            _mensajes.tryEmit("El apoderado no existe")
            return
        }
        if (!ValidadorContacto.valido(apoderado, canal)) {
            _mensajes.tryEmit("${apoderado.nombreCompleto} no tiene un dato válido para ${canal.etiqueta}")
            return
        }
        if (!repositorio.esLocal && !sesionApi.activa) {
            _mensajes.tryEmit(MSG_REQUIERE_CONEXION)
            return
        }
        val actor = _usuarioActivo.value
        viewModelScope.launch {
            try {
                when (val resultado = repositorio.actualizarCanalPreferente(actor, apoderadoId, canal)) {
                    is ApiResult.Success ->
                        _mensajes.tryEmit("Canal preferente de ${apoderado.nombreCompleto}: ${canal.etiqueta}")
                    is ApiResult.SinConexion -> _mensajes.tryEmit(textoSinConexion(resultado))
                    is ApiResult.Error -> {
                        if (resultado.causa == CausaError.NO_AUTORIZADO) {
                            sesionExpirada()
                        } else {
                            _mensajes.tryEmit(resultado.mensaje)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mensajes.tryEmit("No se pudo guardar el canal")
            }
        }
    }

    private companion object {
        const val MSG_MODO_SIN_CONEXION =
            "Sin conexión con el servidor: se muestran los datos guardados en el dispositivo. " +
                "Para enviar comunicados o actualizar, cierra sesión e ingresa nuevamente con conexión."
        const val MSG_REQUIERE_CONEXION =
            "Esta acción necesita conexión con el servidor. Cierra sesión e ingresa nuevamente cuando la tengas."
    }
}
