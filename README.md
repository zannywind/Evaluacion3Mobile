# MPAZ Notifica — Evaluación Parcial 3 (DSY1105)

Aplicación móvil Android desarrollada en **Kotlin** con **Jetpack Compose**, **Material Design 3**, arquitectura **MVVM** y persistencia local con **Room** para la Escuela Escritora Marcela Paz. Permite gestionar la emisión, segmentación y trazabilidad de comunicados escolares hacia apoderados con permisos diferenciados por perfil.

## Desarrollo y autoría

* **Desarrollador:** Damián Esparza
* **Nota de participación:** La configuración del proyecto, resolución de errores de compilación, verificación de flujos con base de datos local (Room), ejecución de pruebas unitarias, generación de llave criptográfica (`.jks`), compilación del APK firmado y documentación fueron realizados íntegramente por Damián Esparza, según consta en el historial de commits del repositorio.

---

## Modo de operación y persistencia

De acuerdo con las indicaciones para la Experiencia 3, la aplicación opera en **Modo Local (Room)** sin dependencia de servidor externo (`USAR_API=false`). La base de datos SQLite local (`mpaz_local.db`) se crea y siembra automáticamente en el primer arranque, soportando los flujos completos de lectura, creación y actualización según las reglas de negocio.

---

## Funcionalidades implementadas

* **Autenticación por perfil (RF-01):** Inicio de sesión con validación de credenciales mediante hash SHA-256 local (`domain/Credenciales.kt`).
* **Emisión diferenciada por rol (RF-02, RF-05, RF-06, RN-01):**
    * *Inspectoría General:* emite todos los tipos de comunicado hacia los cuatro alcances (colegio completo, curso, taller o apoderado) y puede modificar el canal preferente de contacto.
    * *Profesor/a Jefe:* emite comunicados pedagógicos, citaciones y salidas pedagógicas únicamente hacia su curso o un apoderado de su curso.
* **Redacción y adjuntos PDF (RF-03, RF-12):** Formulario con validaciones, adjunto PDF opcional (hasta 5 MB vía SAF) y opción informativa de autorización con firma física (fuerza envío por correo).
* **Despacho simulado y trazabilidad (RF-07, RF-08, RF-11, RN-02):** Registro inmutable de autor, fecha/hora, contenido, destinatarios, canal y estado de despacho (despachado/fallido), con notificación local y vibración al finalizar.
* **Bitácora y filtros (RF-09, RF-10):** Historial de comunicados con filtros por fecha, tipo y emisor.
* **Canal preferente del apoderado (RF-13):** Configuración de canal (Correo, SMS o WhatsApp) editable exclusivamente por Inspectoría General, validando que exista un dato de contacto válido.

---

## Usuarios de prueba

| Perfil | Correo | Clave |
| :--- | :--- | :--- |
| Inspectoría General | `inspectoria@mpaznotifica.cl` | `inspectoria123` |
| Profesor/a Jefe 4° Básico A | `profesor.4basicoA@mpaznotifica.cl` | `profesor123` |
| Profesor/a Jefe 2° Básico A | `profesor.2basicoA@mpaznotifica.cl` | `profesor123` |

---

## Cómo ejecutar el proyecto

1. Clonar el repositorio y abrir la carpeta raíz en **Android Studio** (JDK 17 recomendado).
2. Verificar que `gradle.properties` mantenga `USAR_API=false` (modo local por defecto).
3. Sincronizar Gradle (*Sync Project with Gradle Files*).
4. Ejecutar el módulo `app` en un emulador o dispositivo físico con Android 7.0 (API 24) o superior.
5. Iniciar sesión con cualquiera de las credenciales de prueba indicadas arriba.

---

## Pruebas unitarias

Las pruebas unitarias (JUnit 4, MockK y Coroutines Test) cubren las reglas de negocio puras en `domain/` (credenciales, reglas de comunicado, historial, filtros, fechas y despacho simulado) y el selector de modo local.

Para ejecutarlas desde la terminal en la raíz del proyecto:

```bash
./gradlew testDebugUnitTest