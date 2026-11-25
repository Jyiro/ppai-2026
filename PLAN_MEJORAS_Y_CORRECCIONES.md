# Roadmap de Correcciones y Refactorización Íntegra — CU 28: Recepción de Bolsines

> **Documento vivo y contrato maestro de ejecución.**  
> Ninguna fase avanza ni se da por cerrada con soluciones parciales o parches temporales: cada etapa concluye únicamente al aprobar su **Gate de Verificación Íntegra**.

---

## 1. Principios Rectores (Política Cero Fixes Temporales)

1. **Correspondencia 1:1 con el Diagrama de Secuencia ([`message.md`](message.md)):**
   - Todos los mensajes, emisores, receptores, firmas y tipos de retorno del código deben coincidir exactamente con el diagrama validado.
   - El flujo básico sigue estrictamente el Diagrama de Secuencia sin inventar pasos extra; únicamente se incorporan operaciones adicionales cuando responden a los flujos alternativos del CU (`A1..A6`, `Obs. 1`).
   - **Prohibidos** los alias de compatibilidad (por ejemplo mantener `actual()` junto a `elementoActual()`), las sobrecargas huérfanas y las llamadas "huecas" cuyo valor de retorno se descarta sin uso real.
2. **Arquitectura BCE (Boundary – Control – Entity) estricta:**
   - La capa `com.ppai.boundary.**` (`PantallaRecepcionBolsin`, `DocumentacionRow`, etc.) tiene **0 imports** y **0 invocaciones** hacia `com.ppai.domain.**`, `com.ppai.iterator.**` o `com.ppai.repository.**`, y **nada de lógica de negocio en la pantalla**.
   - Toda interacción desde la interfaz gráfica se relaciona **únicamente con el Gestor (`<<control>>`)** y recibe/envía **exclusivamente tipos primitivos, `String` y arreglos/listas de primitivos (`int`, `boolean`, `String`, `String[]`, `String[][]`, `int[]`)** — cero entidades y cero clases DTO externas en la frontera.
3. **Dominio Orientado a Objetos 100% puro:**
   - Las clases de `com.ppai.domain.model.**` son **POJOs puros**: **0 imports y 0 anotaciones** de JPA/Hibernate (`jakarta.persistence.*`) o de JavaFX.
4. **Avance por Gates verificables:**
   - Cada fase incluye verificaciones automatizadas (tests unitarios, de integración, de persistencia y de arquitectura por reflexión/análisis estático) más revisión explícita contra el contrato congelado.
5. **Cero contaminación por tests ni sobrecargas de conveniencia:**
   - Nunca se agregan constructores duplicados, getters internos que rompan encapsulamiento ni sobrecargas fuera del contrato de las interfaces (`IAgregado`, `IIterador`) para hacer pasar un test. Los tests se escriben contra el diseño limpio, no al revés.

---

## 2. Registro Vivo de Correcciones Confirmadas del Equipo

> Este registro captura cada corrección oficial sobre la secuencia o el diseño conforme se van revisando en **Fase 0**. Todo cambio se refleja primero en los `.md` y luego se ejecuta en bloque a partir de **Fase 1**.

- **Corrección #1 (Confirmada — 22/11/2026):** En el Diagrama de Secuencia oficial el método de `IIterador` se llama **`elementoActual(): Object`** (y **NO** `actual()`).
  - **Documentación actualizada:** [`message.md`](message.md), [`contexto-ppai-cu28.md`](contexto-ppai-cu28.md), [`SDD_CU28_Patron_Iterator.md`](SDD_CU28_Patron_Iterator.md), [`GUIA_ESTUDIO_DEFENSA_CU28.md`](GUIA_ESTUDIO_DEFENSA_CU28.md) y este roadmap.
  - **Acción en código (Fase 1):** Eliminar el alias `actual()` de `IIterador`, `IteradorBolsin`, `IteradorRemito`, `IteradorDetalleRemito` y migrar todas las llamadas en `GestorRecepcionBolsin`, `Bolsin`, `Remito` y `IteratorPatternTest.java` a `elementoActual()`.

- **Corrección #2 (Confirmada — 22/11/2026 — Filtros reales y semántica exacta de `IteradorBolsin` según capturas oficiales):**
  - `primero()` posiciona únicamente `posicionActual = 0` y `siguiente()` incrementa `posicionActual++` (se elimina el look-ahead `while (!comprobarFiltro)` que contradecía el DS y evaluaba el filtro dos veces).
  - `elementoActual()` es el **único** punto que ejecuta `comprobarFiltro(this.filtros)` y **respeta su retorno booleano**: devuelve `lista[posicionActual]` solo si cumple y `null` en caso contrario (`Gestor` procesa solo si `bolsin != null`).
  - Dentro de `comprobarFiltro(Object[] filtro)`: `IteradorBolsin` envía ambos mensajes directamente a `Bolsin`: `sosEnviado(): boolean` (que delega en `CambioEstadoBolsin` → `Estado.esEnviado()`) y `esTuCMDestino(cmUsuario: String): boolean` (que delega en `ComisionMedica.esTuCMDestino(String)` usando comparación exacta, sin `contains()` bidireccional).
  - [`message.md`](message.md) actualizado para corregir el anidamiento erróneo de `esTuCMDestino` y reflejar ambas capturas oficiales (Bloque 2 y Bloque 4).

- **Corrección #3 (Confirmada — 22/11/2026 — Erradicación total de firmas duplicadas, constructores redundantes y métodos extra fuera de interfaz):**
  - **Regla sin excepciones:** se eliminan **todas** las firmas duplicadas en todo el proyecto, sin mantener ninguna por compatibilidad con tests (los tests se adaptan al diseño limpio).
  - **Contratos `IAgregado` / `IIterador` (cero métodos agregados fuera de interfaz y cero ruptura de encapsulamiento):**
    - `GestorRecepcionBolsin`, `Bolsin` y `Remito`: eliminar las sobrecargas duplicadas `crearIterador(Bolsin[])`, `crearIterador(Remito[])` y `crearIterador(DetalleRemito[])`; dejar **únicamente** `@Override public IIterador crearIterador(Object[] elementos)`.
    - `IteradorBolsin`: mantener **únicamente** `public boolean comprobarFiltro(Object[] filtro)` (coincidiendo con el `+` del Diagrama de Clases, sin subirlo a `IIterador` y sin sobrecarga `comprobarFiltro()` sin parámetros).
    - `IteradorBolsin`, `IteradorRemito` e `IteradorDetalleRemito`: dejar **1 solo constructor** genérico fiel al Diagrama de Clases y compatible por covarianza con el DS (`(Object[], Object[])`, `(Object[])`, `(Object[])`), eliminar los constructores duplicados, eliminar `actual()`, y eliminar los getters que rompen encapsulamiento (`getLista()`, `getPosicionActual()`, `getFiltros()`).
  - **Limpieza de sobrecargas duplicadas / solo-para-tests en Dominio, Control y Boundary:**
    - `Bolsin`: quitar `sosEnviado(ComisionMedica)`, `sosEnviado(String)`, `esTuCMDestino(ComisionMedica)`, las 2 sobrecargas no usadas de `marcarRecibido(...)`, `crearNuevoCambioDeEstado` sin empleado, y el atributo redundante `Remito remito` / `getRemito()`.
    - `ComisionMedica`: quitar `esTuCMDestino(ComisionMedica)` (dejar `esTuCMDestino(String cmUsuario)` con igualdad exacta).
    - `Remito`: quitar `aceptar` sin `Empleado` y unificar `marcarRecibidoYAceptado` / `marcarRecibidoYAceptadoParcial` / `setEstado`.
    - `DetalleRemito`: quitar el duplicado `getDocumentacionAsociada()` y `aceptar` sin `Empleado`.
    - `Documentacion`: quitar `aceptar` y `newCambioEstadoDocumentacion` sin `Empleado`.
    - `GestorRecepcionBolsin`: quitar sobrecargas legacy/test (`tomarSeleccionOpcion(String precinto)`, `tomarConfirmacionSeleccion(String, String)`).
    - `PantallaRecepcionBolsin`: quitar todas las sobrecargas vacías o solo-para-tests (`mostrarNumeroPrecinto()`, `mostrarNumeroPrecinto(String)`, `mostrarCMOrigen()`, `mostrarCMOrigen(ComisionMedica)`, `mostrarDatosDeRemito(String)`, `solicitarConfirmacionSeleccion()`, `tomarConfirmacionSeleccion(): String`, `tomarSeleccionBolsin(): String`, etc.).

- **Corrección #4 (Confirmada — 22/11/2026 — Soporte íntegro de múltiples remitos con múltiples documentaciones por bolsín):**
  - **Problema actual detectado en toda la pila:**
    - `Bolsin`: mantiene un atributo duplicado `Remito remito` y `obtenerInformacionRemito()` recorre con `IteradorRemito` pero descarta los resultados y retorna solo `primerRemito`.
    - `GestorRecepcionBolsin`: almacena un único `remitoSeleccionado`, por lo que en A3/A4/A5 y al actualizar el estado de remito en Opción 1 ignora los remitos `2..N` del bolsín y sus documentaciones; además en `notificarRecepcion()` informa un solo remito.
    - `PantallaRecepcionBolsin`: Paso 1 (`tblBolsines`), Paso 2 (`mostrarDatosDeRemito`), Paso 3 (Confirmación) y Paso 4 (Resultado/CU 29) están codificados para mostrar un único remito.
    - Datos semilla (`RepositorioJpaCU28`): los 7 bolsines iniciales tienen un solo remito cada uno (ninguno ejercita realmente `IteradorRemito` con `2+` remitos).
  - **Solución integral (sin parches, distribuida en Fases 1–4):**
    - **Fase 1 (Dominio + Iterator):** eliminar `Bolsin.remito` / `getRemito()`, hacer que `Bolsin.obtenerInformacionRemito()` y `Remito.buscarDocumentacion()` recorran y devuelvan realmente la información de **todos** los remitos y **todas** sus documentaciones (`String[][]` / estructura fiel al DS, cero retornos descartados), y soportar actualización de estado en **todos** los remitos del bolsín (Opción 1, A3, A4 y A5).
    - **Fase 2 (Persistencia + Semilla):** persistir y rehidratar todos los remitos y sus detalles en BD; incluir al menos un bolsín semilla con **múltiples remitos (`2+`) y múltiples documentaciones por remito** para demostrar `IteradorRemito` $\rightarrow$ `IteradorDetalleRemito` en vivo y en tests.
    - **Fase 3 (Gestor CU 28 + CU 29):** eliminar `remitoSeleccionado` singular del Gestor; operar sobre la totalidad de remitos del bolsín seleccionado, propagando todos los remitos y sus documentaciones en primitivos/`String[][]` hacia la vista y hacia `GestorNotificarRecepcionBolsin` (CU 29).
    - **Fase 4 (Boundary):** adaptar Paso 1 (mostrar todos los números de remito del bolsín), Paso 2 (visualizar cada remito con su número y todas sus documentaciones con selección `¿Conforme?` para A3/A4), Paso 3 (confirmación con desglose de todos los remitos y sus documentos) y Paso 4 (comprobante de correo CU 29 con todos los remitos).

- **Corrección #5 (Confirmada — 22/11/2026 — Frontera 100% primitivos entre Pantalla y Gestor + `Bolsin.numeroPrecinto` como `int` + criterio A1/A2):**
  - **Pantalla $\leftrightarrow$ Gestor solo con primitivos (BCE ultra-estricto):** `PantallaRecepcionBolsin` solo conoce a `GestorRecepcionBolsin`, `java.*` y `javafx.*`. No recibe ni devuelve ninguna clase de entidad ni DTOs externos: toda comunicación usa únicamente primitivos, `String`, `String[]`, `String[][]`, `int[]`, `boolean` (coincidiendo con `String[][]` / `String[]` / `int` del Diagrama de Secuencia). Toda lógica de negocio (incluyendo flujos alternativos) vive exclusivamente en el Gestor/Dominio.
  - **`Bolsin.numeroPrecinto` como `int`:** en el dominio (`Bolsin`) y en la BD, `numeroPrecinto` es `int` y expone un único getter `public int getNroPrecinto()` (se elimina `getNumeroPrecinto(): String` y el parseo por regex). Se muestra como número limpio en la interfaz gráfica (sin prefijos ni parseos inventados, conforme a **Corrección #6**).
  - **Filtros A1 / A2:** `IteradorBolsin` se mantiene estricto al flujo básico de la secuencia (`ENVIADO` + `cmDestino`); los flujos alternativos A1 (precinto) y A2 (CM origen) se resuelven en el Gestor recibiendo únicamente `String` desde la Pantalla y devolviendo filas de `String` (cero entidades en la frontera).

- **Corrección #6 (Confirmada — 22/11/2026 — Números `int` limpios sin prefijos + Blindaje final anti-acoplamiento en Pantalla):**
  - **`Bolsin.numeroPrecinto` y `Remito.numero` como `int` limpios:** ambos son `int` en Dominio, BD y UI (ej. `7041`, `458`), con únicos getters `Bolsin.getNroPrecinto(): int` y `Remito.getNumero(): int`. Cero prefijos inventados (`"PREC-"`, `"REM-"`) y cero parseo con regex ni acoplamiento implícito con A1.
  - **Cero inspección de nombres de estados en la Pantalla:** eliminar los `if (item.contains("No recibida" / "rechazada" / "parcial" / "aceptada"))` en `PantallaRecepcionBolsin`; usar estilos neutros o códigos puramente visuales (`OK`, `WARN`, `ERROR`, `INFO`) entregados por el Gestor.
  - **Cero acoplamientos sutiles restantes en `PantallaRecepcionBolsin`:**
    1. Cero nombres de estados, asuntos o remitentes de correo hardcodeados en la vista (ej. `"Recibido en CM Destino"`, asunto o remitente del correo): todos esos `String` los entrega el Gestor (o CU 29 vía el Gestor).
    2. Cero `LocalDateTime.now()` ni formateo de fechas (`DateTimeFormatter`) en la vista: las fechas ya llegan formateadas como `String` desde el Gestor.
    3. Cero llamadas "pull" a getters de datos del Gestor desde eventos UI (ej. el botón `Limpiar` notifica al Gestor en vez de llamar `gestor.getBolsinesEnviados()`).

- **Corrección #7 (Confirmada — 22/11/2026 — Persistencia real en disco, Patrón Data Mapper y Seeder en script separado):**
  - **Base de datos real en disco (cero BD en memoria en ejecución normal):** migrar `persistence.xml` de `jdbc:h2:mem:ppaidb` a persistencia real en archivo (`jdbc:h2:file:./data/ppaidb`, con `data/` en `.gitignore`). Todo cambio confirmado sobre bolsines, remitos, documentaciones e historiales queda persistido en disco al cerrar y reabrir la aplicación.
  - **Separación estricta del Seeder en script dedicado de reinicio:** eliminar toda ejecución de `DELETE FROM ...` y toda carga automática destructiva desde el constructor de `GestorRecepcionBolsin` o `RepositorioJpaCU28`. Extraer el seeder a una herramienta/script ejecutable independiente (ej. `ReiniciarEscenarioBD` + `scripts/reiniciar-escenario.ps1`) para restablecer el escenario inicial (incluyendo bolsín multi-remito) únicamente cuando el equipo/usuario lo ejecute de forma explícita. Garantizar que ni `finCU()`, ni abrir/cerrar la app, ni cambiar de usuario borren datos persistidos.
  - **Desacople Dominio $\leftrightarrow$ BD mediante Patrón Data Mapper (clase mapeadora dedicada):** quitar el 100% de las anotaciones JPA y constructores vacíos de `com.ppai.domain.model.cu28` (POJOs puros); crear entidades JPA dedicadas (`*JpaEntity`) dentro de `com.ppai.repository.cu28.jpa` y delegar la traducción bidireccional Dominio $\leftrightarrow$ BD en una **clase mapeadora dedicada** (ej. `MapeadorPersistenciaCU28`).

- **Corrección #8 (Confirmada — 23/11/2026 — Eliminación completa del selector `"Mock:"` y de cambio de escenario/usuario del CU 28):**
  - El cambio de usuario no forma parte del flujo básico ni de los flujos alternativos (A1–A6 / Obs. 1) del CU 28; la sesión activa es una precondición proveniente de otro caso de uso.
  - Se eliminan por completo el `ComboBox` `"Mock:"` de `PantallaRecepcionBolsin` y el método `cargarEscenario(int)` de `GestorRecepcionBolsin`.
  - La aplicación arranca leyendo la `Sesion` activa ya persistida en la BD (ej. operador de CMC por defecto). El caso negativo de A5 (operador de CM Regional) queda cubierto en `CU28Test` y, si se requiere probar en vivo, seleccionando el usuario activo al correr el script independiente `ReiniciarEscenarioBD`.

- **Corrección #9 (Confirmada — 23/11/2026 — 7 ajustes del escaneo profundo BCE + Secuencia 1:1):**
  1. **Boundary externo (`InterfazServidorCorreo`, mover a `com.ppai.boundary.cu29`):** recibe exclusivamente primitivos `String` (`emailDestino`, `asunto`, `cuerpo`), eliminando el `import` de `NotificacionRecepcionBolsin` para garantizar **0 imports de dominio en todo `com.ppai.boundary.**`**.
  2. **Bloque 1 del DS (`Sesion → Usuario → Empleado`):** `Sesion` delega realmente en `usuario.getCMEmpleado()` → `empleado.getCM()` conforme a [`message.md`](message.md#L45-L47) (exponiendo además el `Empleado` logueado para la auditoría de `CambioEstado*` en Bloques 7 y 8).
  3. **Bloque 2 del DS (`IteradorBolsin`):** `GestorRecepcionBolsin.crearIterador(...)` pasa `new Object[]{ comisionMedicaEmpleado.getNombre() }` (`String`) en lugar de `ComisionMedica`, eliminando el `import ComisionMedica` de `IteradorBolsin`.
  4. **Bloque 3 del DS (Boundary):** eliminar `mostrarBolsines(List<Bolsin>)`; usar realmente `mostrarNumeroPrecinto(...)` y `mostrarCMOrigen(...)` con los arreglos de primitivos recolectados en el loop del Bloque 2 (cero cáscaras vacías).
  5. **Bloque 4 del DS (Dominio + Iterator):** `Bolsin.obtenerInformacionRemito(): String[][]` y `Remito.buscarDocumentacion(): String[][]` respetan exactamente los tipos de retorno de [`message.md`](message.md#L85-L106) y consumen sin descartar `r.getNumero()`, `r.buscarDocumentacion()` y `det.obtenerAsuntoYTipoDoc()`.
  6. **Bloque 8 del DS (Multi-remito y encapsulamiento):** delegar también A3, A4 y A5 a través de `Bolsin → Remito → DetalleRemito → Documentacion` sobre **todos** los remitos del bolsín (sin mutar `Documentacion` directamente desde el Gestor) y alinear el orden de parámetros de `marcarRecibido(...)` y `actualizarRemitoYDocumentacion(...)` con [`message.md`](message.md#L141-L157).
  7. **Arranque limpio (`JavaFxApp` / `Main` / seeder):** `JavaFxApp` lee la `Sesion` activa persistida en BD vía repositorio (sin hardcodear sesión en memoria), limpiar imports sin uso de `Main.java` y reemplazar `RepositorioMockBolsines` en `src/main/java` por el script independiente `ReiniciarEscenarioBD`.

- **Corrección #10 (Confirmada — 23/11/2026 — Modelo de Dominio 100% fiel a E1+E2, 0 `Long id` artificiales, `Sesion` con vigencia, constructores genéricos de Iteradores, Opción B y limpieza de clases fuera del diagrama):**
  1. **Cero `Long id` artificiales de BD y solapamiento exacto E1 (Análisis) + E2 (Diseño):** se eliminan todos los `private Long id;` inventados para PKs en las 12 clases reales del dominio y se alinean sus atributos y asociaciones reales:
     - `Bolsin`: `fecha: LocalDateTime`, `numeroBolsin: int` *(clave de identidad natural; `numeroPrecinto` cambia si se reabre)*, `numeroPrecinto: int`, `peso: double`, `cmOrigen`, `cmDestino`, `remitos`, `cambiosDeEstado`.
     - `Remito`: `fecha: LocalDateTime`, `numero: int`, `cmOrigen`, `cmDestino`, `estadoRemito`, `detalles`.
     - `DetalleRemito`: `areaCMCDestino: String`, `documentacion`.
     - `Documentacion`: `numero: int`, `asunto: String`, `fechaPase: LocalDateTime`, `tipoDocumento`, `cmOrigen`, `cambiosDeEstado`; métodos exactos de máquina de estados y DS: `aceptar`, `marcarNoRecibida`, `marcarRecibidaRechazada`, `marcarParaRedirigir`, `newCambioEstadoDocumentacion`.
     - `TipoDocumento`: `nombre: String`, `descripcion: String`.
     - `ComisionMedica`: `codigo: int`, `nombre: String`, `direccion: String`, `email: String`, `telefono: String` + `esCentral()`.
     - `Estado`: `nombre: String`, `descripcion: String`, `ambito: String`.
     - `CambioEstadoBolsin` y `CambioEstadoDocumentacion`: `fechaHoraInicio: LocalDateTime`, `fechaHoraFin: LocalDateTime`, `estado`, `empleado` *(sin `Long id`)*.
     - `Empleado`: `nombre: String`, `comisionMedica`.
     - `Usuario`: `nombre: String`, `descripcion: String`, `empleado`.
     - `Sesion`: `usuario: Usuario`, `fechaHoraInicio: LocalDateTime`, `fechaHoraFin: LocalDateTime` (`estaActiva()` → `fechaHoraFin == null`; en Fase 2 se consulta `WHERE fecha_hora_fin IS NULL` con PK compuesta natural `(usuario, fecha_hora_inicio)`).
  2. **Constructores únicos genéricos en los 3 Iteradores:** `IteradorBolsin(Object[], Object[])`, `IteradorRemito(Object[])`, `IteradorDetalleRemito(Object[])` (trazabilidad directa con DC, DS por covarianza de arreglos en Java, e `IAgregado`, sin constructores duplicados).
  3. **Regla multi-remito A3/A4 *(documentada para revisitar en Fase 6)*:** cuando un bolsín tiene múltiples remitos y solo algunos tienen documentación observada (faltante en A3 o errónea en A4), solo los remitos afectados pasan a `RECIBIDO_Y_ACEPTADO_PARCIAL` y los remitos 100% conformes pasan a `RECIBIDO_Y_ACEPTADO`. *(Revisitar esta regla de negocio con el equipo antes de la entrega final.)*
  4. **Corte limpio Opción B en Fase 1:** se remueven el 100% de las anotaciones JPA, constructores `protected` y campos `Long id` ya en Fase 1.
  5. **Nombre del Gestor y pureza del paquete `domain.model.cu28`:** se mantiene `GestorRecepcionBolsin`; las 2 clases auxiliares que no existen en el Diagrama de Clases (`OpcionRecepcion` y `NotificacionRecepcionBolsin`) se retiran de `com.ppai.domain.model.cu28` en Fase 3 para que el paquete de dominio quede exclusivamente con las **12 clases reales de E1+E2**.

---

## 3. Roadmap por Fases

### Fase 0 — Congelamiento del Contrato de Secuencia y Decisiones de Diseño
**Estado:** ✅ **Cerrada** *(contrato congelado y validado con escaneo profundo BCE + Secuencia 1:1)*

* **Objetivo:** Validar íntegramente [`message.md`](message.md) frente al Diagrama de Secuencia real y cerrar todas las decisiones arquitectónicas antes de iniciar la refactorización.
* **Actividades:**
  1. Incorporar al registro de la Sección 2 todas las correcciones pendientes del diagrama de secuencia (bloques 1 a 9).
  2. Sincronizar [`message.md`](message.md), [`contexto-ppai-cu28.md`](contexto-ppai-cu28.md), [`SDD_CU28_Patron_Iterator.md`](SDD_CU28_Patron_Iterator.md) y [`GUIA_ESTUDIO_DEFENSA_CU28.md`](GUIA_ESTUDIO_DEFENSA_CU28.md) sin contradicciones entre sí.
  3. Resolver los puntos abiertos obligatorios listados a continuación.

#### Puntos Ya Resueltos en Fase 0
* ~~Semántica de `IteradorBolsin` (`primero` / `siguiente` / `elementoActual`)~~ → **Resuelto con Corrección #2** (`primero()` en `0`, `siguiente()` avanza de a `1`, solo `elementoActual()` evalúa `comprobarFiltro(filtros)` devolviendo `null` cuando no cumple).
* ~~Delegación `sosEnviado()` vs `esTuCMDestino()`~~ → **Resuelto con Corrección #2** (`IteradorBolsin.comprobarFiltro()` invoca `bolsin.sosEnviado()` y `bolsin.esTuCMDestino(String)` directamente según las capturas oficiales).
* ~~Visibilidad / contrato de `comprobarFiltro(Object[] filtro)`~~ → **Resuelto con Corrección #3** (`public boolean comprobarFiltro(Object[] filtro)` únicamente en `IteradorBolsin`, conforme al `+` del Diagrama de Clases, sin subirlo a `IIterador` y sin sobrecarga sin parámetros).
* ~~Soporte para múltiples remitos con múltiples documentaciones por bolsín~~ → **Resuelto con Corrección #4** (se elimina el soporte de un único remito en toda la pila: Dominio, Persistencia/Semilla, Control, CU 29 y Boundary).
* ~~Filtros A1/A2 y comunicación 100% primitivos en Boundary~~ → **Resuelto con Corrección #5** (`IteradorBolsin` estricto al flujo básico; A1/A2 en Gestor recibiendo `String` y devolviendo filas de `String`; cero entidades y cero DTOs externos en la frontera).
* ~~Consistencia `int` de `Bolsin.numeroPrecinto` / `Remito.numero` y blindaje final anti-acoplamiento en Pantalla~~ → **Resuelto con Corrección #6** (ambos números como `int` limpios sin prefijos `"PREC-"`/`"REM-"` en Dominio, BD y UI; cero `contains()` de estados, cero strings de estado/correo hardcodeados, cero `LocalDateTime.now()`/formateo de fechas y cero pull de getters del Gestor en `PantallaRecepcionBolsin`).
* ~~Estrategia de persistencia real y desacople de BD~~ → **Resuelto con Corrección #7** (H2 persistente en disco `jdbc:h2:file:./data/ppaidb`, Patrón Data Mapper con `*JpaEntity` + clase mapeadora dedicada, y Seeder extraído a script independiente sin borrados en arranque ni en `finCU()`).
* ~~Selector `"Mock:"` y cambio de usuario/escenario en Fase 5~~ → **Resuelto con Corrección #8** (eliminados por completo; la sesión activa viene persistida en la BD como precondición de otro CU).
* ~~Validación del resto del Diagrama de Secuencia (Bloques 1, 3, 5, 6, 7, 8 y 9) y escaneo profundo BCE~~ → **Resuelto mediante capturas oficiales de Lucidchart + 7 ajustes de Corrección #9** (`InterfazServidorCorreo` solo con `String`, cadena `Sesion → Usuario → Empleado`, filtro `String` en `IteradorBolsin`, consumo real de `mostrarNumeroPrecinto`/`mostrarCMOrigen`, `obtenerInformacionRemito(): String[][]` y `buscarDocumentacion(): String[][]`, delegación multi-remito de A3/A4/A5 en `Bolsin → Remito → DetalleRemito → Documentacion`, y carga de `Sesion` desde BD en `JavaFxApp`).
* ~~Solapamiento exacto E1+E2, eliminación de `Long id` artificiales de BD, `Sesion` con vigencia y constructores genéricos de Iteradores~~ → **Resuelto con Corrección #10** (0 `Long id` artificiales en las 12 entidades reales, atributos E1+E2 completos, `Sesion` con `fechaHoraInicio`/`fechaHoraFin`, constructores genéricos `Object[]` en iteradores, corte limpio Opción B en Fase 1, y retiro de `OpcionRecepcion`/`NotificacionRecepcionBolsin` del dominio planificado para Fase 3).

* **Gate de Salida (Fase 0):**
  - Todos los `.md` alineados al 100% con el diagrama validado (cero contradicciones).
  - Todos los puntos abiertos decididos y documentados en este archivo.

---

### Fase 1 — Patrón Iterator Exacto + Dominio OO 100% Puro (Opción B)
**Estado:** ✅ **Cerrada** *(Gate Fase 1 aprobado: `mvn clean test "-Dtest=IteratorPatternTest,DominioPuroYContratosTest"` — 18/18 tests en verde, 29 fuentes main + 4 test compilados bajo JDK 21)*

* **1.1 Patrón Iterator fiel al Diagrama de Secuencia (Correcciones #1, #2, #3 y #4):**
  * Eliminar `actual()` de `IIterador`, `IteradorBolsin`, `IteradorRemito` e `IteradorDetalleRemito`; dejar **`elementoActual()`** como único método oficial del contrato.
  * Actualizar `GestorRecepcionBolsin`, `Bolsin`, `Remito` y `IteratorPatternTest.java` para utilizar `elementoActual()`.
  * En `IteradorBolsin`:
    - `primero()` posiciona únicamente `posicionActual = 0` y `siguiente()` incrementa `posicionActual++` (sin look-ahead).
    - `elementoActual()` es el único lugar que invoca `comprobarFiltro(this.filtros)` y respeta su valor booleano:
      ```java
      if (comprobarFiltro(this.filtros)) {
          return lista[posicionActual];
      }
      return null;
      ```
    - Mantener únicamente `public boolean comprobarFiltro(Object[] filtro)` con filtro `String` (`cmUsuario`), eliminando el `import ComisionMedica` de `IteradorBolsin` y delegando directamente en `bolsin.sosEnviado()` y `bolsin.esTuCMDestino(cmUsuario)` (String exacto, sin `contains()` bidireccional ni ramas por tipo duplicadas).
  * Limpiar contratos de `IAgregado` / `IIterador`:
    - Dejar **1 solo** `@Override public IIterador crearIterador(Object[] elementos)` en `GestorRecepcionBolsin`, `Bolsin` y `Remito` (sin sobrecargas tipadas).
    - Dejar **1 solo constructor** genérico en cada iterador concreto (`(Object[], Object[])`, `(Object[])`, `(Object[])`), compatible por covarianza con `Bolsin[]`/`Remito[]`/`DetalleRemito[]`, sin constructores duplicados, sin `comprobarFiltro()` sin parámetros y sin getters internos (`getLista`, `getPosicionActual`, `getFiltros`).
  * **Soporte multi-remito y consumo real de retornos (Bloque 4 exacto):** eliminar `Bolsin.remito` / `getRemito()`; hacer que `Bolsin.obtenerInformacionRemito(): String[][]` y `Remito.buscarDocumentacion(): String[][]` recorran con `IteradorRemito` / `IteradorDetalleRemito` respetando exactamente los tipos de retorno de [`message.md`](message.md#L85-L106) y devolviendo la información de **todos** los remitos y **todas** sus documentaciones (consumiendo `r.getNumero()`, `r.buscarDocumentacion()` y `det.obtenerAsuntoYTipoDoc()`, sin descartar retornos ni limitarse a `primerRemito`).

* **1.2 Dominio OO puro (Opción B) sin JPA, sin `Long id` artificiales ni sobrecargas duplicadas:**
  * Remover todo `import jakarta.persistence.*`, todas las anotaciones JPA (`@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@ManyToOne`, `@OneToMany`, `@JoinColumn`, `@OrderColumn`, `@Transient`), todos los campos `Long id` artificiales de BD y los constructores `protected` impuestos por Hibernate de las 12 clases de `com.ppai.domain.model.cu28`:
    - `Bolsin`, `CambioEstadoBolsin`, `CambioEstadoDocumentacion`, `ComisionMedica`, `DetalleRemito`, `Documentacion`, `Empleado`, `Estado`, `Remito`, `Sesion`, `TipoDocumento`, `Usuario`.
  * **Solapamiento exacto E1+E2 y claves naturales:** alinear todos los atributos y asociaciones de las 12 clases según **Corrección #10** (`Bolsin.numeroBolsin` como identidad natural, `Sesion` con `fechaHoraInicio`/`fechaHoraFin` y `estaActiva()`, `Documentacion` con `cmOrigen` y métodos `marcarNoRecibida`, `marcarRecibidaRechazada`, `marcarParaRedirigir`).
  * **Bloque 1 fiel a [`message.md`](message.md#L45-L47):** hacer que `Sesion` delegue en `usuario.getCMEmpleado()` → `empleado.getCM()` (exponiendo además `getEmpleadoLogueado()` para la auditoría de `CambioEstado*`).
  * **Números `int` limpios en Dominio:**
    - Cambiar `Bolsin.numeroPrecinto` a `int` con un único getter `public int getNroPrecinto()`, eliminando `getNumeroPrecinto(): String` y el parseo por regex.
    - Cambiar `Remito.numero` a `int` con un único getter `public int getNumero()` (sin prefijos `"REM-"`).
  * Eliminar **todas** las sobrecargas redundantes y métodos solo-para-tests enumerados en **Corrección #3** (`Bolsin`, `ComisionMedica`, `Remito`, `DetalleRemito`, `Documentacion`), adaptando los tests al diseño limpio.
  * Garantizar que la actualización de estados en `Bolsin → Remito → DetalleRemito → Documentacion` soporte **todos** los remitos y **todas** sus documentaciones en **Opción 1, A3, A4 y A5** (aplicando la regla A3/A4 por remito según **Corrección #10**), alineando el orden de parámetros de `marcarRecibido(...)` y `actualizarRemitoYDocumentacion(...)` con [`message.md`](message.md#L141-L157).
  * Mover al dominio las reglas que hoy están duplicadas en el controlador o la pantalla: incorporar en `ComisionMedica` el atributo `email` del diagrama de clases (`getEmail()`) y la capacidad de responder `esCentral()`.

* **Gate de Verificación Íntegra (Fase 1):**
  * **Test estático + reflexión de pureza de dominio:** 0 imports y 0 anotaciones de `jakarta.persistence.*`, `org.hibernate.*` y `javafx.*`, **0 campos `id` artificiales** y 0 constructores `protected` sin parámetros en las 12 clases reales de `com.ppai.domain.model.cu28`.
  * **Test estático + reflexión de contratos estrictos del patrón Iterator y Dominio:**
    - 0 referencias a `java.util.Iterator` / `java.lang.Iterable` en `com.ppai.iterator.**` y agregados.
    - `IIterador` declara exactamente `{primero, siguiente, haFinalizado, elementoActual}` (sin `actual()`).
    - `IteradorRemito` e `IteradorDetalleRemito` declaran **exactamente 1 constructor** (`(Object[])`), **0 getters internos** (`getLista`, `getPosicionActual`) y **0 métodos públicos fuera de `IIterador`**.
    - `IteradorBolsin` declara **exactamente 1 constructor** (`(Object[], Object[])`), **0 imports de `ComisionMedica`**, **0 getters internos** (`getLista`, `getPosicionActual`, `getFiltros`) y **únicamente** los 4 métodos de `IIterador` + `public boolean comprobarFiltro(Object[] filtro)` (sin sobrecarga sin parámetros).
    - Los 3 agregados (`GestorRecepcionBolsin`, `Bolsin`, `Remito`) declaran **exactamente 1** método `crearIterador(Object[])`, sin sobrecargas duplicadas.
    - `Bolsin.numeroPrecinto` es `int` y expone únicamente `getNroPrecinto(): int` (sin `getNumeroPrecinto(): String`).
    - `Remito.numero` es `int` y expone únicamente `getNumero(): int`.
    - Cero sobrecargas duplicadas o de compatibilidad con tests en Dominio, Gestor y Pantalla.
  * **Tests unitarios de `IteradorBolsin` sin look-ahead:** `primero()` inicia en índice `0`, `elementoActual()` devuelve `null` cuando `comprobarFiltro` retorna `false` (probado tanto por estado $\neq$ `ENVIADO` como por CM destino distinta), `siguiente()` avanza secuencialmente 1 a 1, y cubre colecciones vacías, nulas y reinicio con `primero()`.
  * **Tests unitarios multi-remito en Dominio:** `Bolsin.obtenerInformacionRemito()` y la actualización de estados sobre un bolsín con **múltiples remitos (`2+`) y múltiples documentaciones por remito** verifican que **todos** los remitos y **todas** sus documentaciones se leen y actualizan sin omitir ninguno (incluyendo Opción 1, A3/A4 por remito y A5).

---

### Fase 2 — Persistencia Real en Disco, Patrón Data Mapper y Seeder Independiente
**Estado:** ⏳ **Pendiente** *(depende de Fase 1)*

* **2.1 Patrón Data Mapper en capa Repository (con PKs naturales y compuestas):**
  * Crear las entidades JPA dedicadas (`*JpaEntity`) dentro de `com.ppai.repository.cu28.jpa` utilizando **claves primarias naturales y compuestas** (sin `Long id` artificiales ni siquiera en tablas `cambio_estado_*`, `detalle_remito` o `sesion`) y delegar la traducción bidireccional Dominio puro $\leftrightarrow$ BD en una **clase mapeadora dedicada** (ej. `MapeadorPersistenciaCU28`), registrando únicamente las `*JpaEntity` en `persistence.xml`.
  * Mantener `IRepositorioCU28` trabajando exclusivamente con objetos del dominio puro, persistiendo y rehidratando `Bolsin.numeroBolsin` / `numeroPrecinto` (`int`), `Remito.numero` (`int`), la `Sesion` activa (`WHERE fecha_hora_fin IS NULL`) y la totalidad de remitos y detalles de cada bolsín.

* **2.2 Base de datos persistente en disco y Seeder extraído a script independiente:**
  * Migrar `persistence.xml` de `jdbc:h2:mem:ppaidb` a persistencia real en disco (`jdbc:h2:file:./data/ppaidb`) y agregar `data/` a `.gitignore`.
  * Eliminar `inicializarBolsinesDeEjemplo(...)` del constructor de `GestorRecepcionBolsin` y eliminar los `DELETE FROM ...` del flujo normal de la aplicación: la app simplemente lee y persiste sobre la BD existente (solo crea el esquema si no existe; los datos iniciales se cargan mediante el script dedicado o sembrado inicial no destructivo únicamente si la BD está vacía).
  * Extraer el seeder a una clase/script independiente de reinicio de escenario (ej. `ReiniciarEscenarioBD` + `scripts/reiniciar-escenario.ps1`) que incluya al menos un bolsín multi-remito (`2+` remitos y múltiples documentaciones) para cuando el equipo/usuario necesite restablecer la BD de forma explícita, reemplazando `RepositorioMockBolsines` dentro de `src/main/java`.
  * Hacer que `JavaFxApp` lea la `Sesion` activa persistida en BD vía repositorio (sin hardcodear `ComisionMedica`/`Empleado`/`Usuario`/`Sesion` en memoria) y limpiar los imports sin uso de `Main.java`.
  * Garantizar que `finCU()` solo limpie referencias transitorias del turno en memoria y jamás altere ni borre datos persistidos en la BD.
  * Configurar una BD/perfil aislado para que la suite de tests automatizados sea determinista sin contaminar `./data/ppaidb`.

* **Gate de Verificación Íntegra (Fase 2):**
  * **Test de durabilidad real:** confirmar una recepción (incluyendo un bolsín con **2+ remitos y múltiples documentaciones**), cerrar completamente `EntityManagerFactory`, reabrirlo y verificar que `Bolsin`, `CambioEstadoBolsin`, **todos** sus `Remito`, `Documentacion` y `CambioEstadoDocumentacion` mantienen su estado e historial en disco.
  * **Test de no-destrucción al iniciar y finalizar:** comprobar que instanciar `GestorRecepcionBolsin` y ejecutar `finCU()` sobre una BD ya poblada **no borra ni reinicia** recepciones confirmadas previamente en disco.
  * **Test de Data Mapper y script de reinicio:** verificar el mapeo bidireccional completo vía `MapeadorPersistenciaCU28` y comprobar que `ReiniciarEscenarioBD` deja la BD limpia y poblada con los datos semilla (incluyendo el bolsín multi-remito) y sin duplicar catálogos (`Estado`, `ComisionMedica`, `TipoDocumento`).

---

### Fase 3 — Caso de Uso Incluido (`<<includes>>` CU 29) con Gestor Propio + Saneamiento del Controlador
**Estado:** ⏳ **Pendiente** *(depende de Fase 1 y Fase 2)*

* **3.1 Gestor dedicado para el CU 29 (`<<includes>>`):**
  * Crear `GestorNotificarRecepcionBolsin` en `com.ppai.controller.cu29` y mover `InterfazServidorCorreo` a `com.ppai.boundary.cu29`, haciendo que `InterfazServidorCorreo` reciba exclusivamente primitivos `String` (`enviarNotificacion(String emailDestino, String asunto, String cuerpo)` — 0 imports de dominio).
  * Trasladar a `GestorNotificarRecepcionBolsin` la construcción de `NotificacionRecepcionBolsin`, el formato del correo (detallando **todos** los remitos del bolsín y el estado final de todas sus documentaciones, asunto y remitente) y la invocación a `InterfazServidorCorreo`.
  * Mantener `GestorRecepcionBolsin.notificarRecepcion()` como método oficial de la secuencia, delegando en `GestorNotificarRecepcionBolsin` **estrictamente post-commit** después de `persistirRecepcion()`.

* **3.2 Saneamiento del Controlador y contratos exclusivamente con primitivos:**
  * En Bloque 2 (`crearIterador(...)`), pasar `new Object[]{ this.comisionMedicaEmpleado.getNombre() }` (`String`) como filtro a `IteradorBolsin`.
  * En Bloque 3, eliminar `pantalla.mostrarBolsines(bolsinesEnviados)` e invocar realmente `mostrarNumeroPrecinto(...)` y `mostrarCMOrigen(...)` con los datos recolectados en Bloque 2 (cero cáscaras vacías).
  * **Eliminar `remitoSeleccionado` singular** de `GestorRecepcionBolsin`: delegar la actualización en Opción 1, A3, A4 y A5 a través de `Bolsin → Remito → DetalleRemito → Documentacion` sobre **todos** los remitos del bolsín (sin mutar `Documentacion` directamente desde el Gestor), y propagar todos los remitos y sus documentaciones hacia la vista y hacia CU 29.
  * Eliminar código muerto, sobrecargas legacy/test (`Corrección #3`), el método `cargarEscenario(int)` (`Corrección #8`) y lógica duplicada en `GestorRecepcionBolsin` (variable sin uso `esDestinoCMC` en `buscarOpcionesRecepcion()`, armado manual de email usando `ComisionMedica.getEmail()`, getters que exponen entidades hacia la pantalla como `getSesionActual()`, `getTodosBolsines()`, `getBolsinSeleccionado()`, `getRemitoSeleccionado()`, `getComisionMedicaEmpleado()`, `getEmpleadoActual()`).
  * Retirar `OpcionRecepcion` y `NotificacionRecepcionBolsin` de `com.ppai.domain.model.cu28` (las clases auxiliares que no existen en el Diagrama de Clases pasan al contrato primitivo del Gestor y al controlador CU 29, dejando en `domain.model.cu28` exclusivamente las **12 clases reales de E1+E2**).
  * Definir todos los contratos de entrada/salida hacia la Pantalla **exclusivamente con primitivos (`int`, `boolean`, `String`, `String[]`, `String[][]`, `int[]`)** — cero entidades y cero clases DTO externas.
  * Entregar desde el Gestor: fechas ya formateadas como `String`, todos los textos de estados/asunto/remitente del correo resueltos y códigos de severidad visual genéricos (`"OK"`, `"WARN"`, `"ERROR"`, `"INFO"`) para badges de la interfaz.
  * Resolver en el Gestor la lógica de búsqueda A1/A2 a partir de `String` (comparando directamente contra `int numeroPrecinto` sin prefijos ni regex) y devolver matrices/arreglos de `String` para la vista; ante el evento de limpiar filtros, el Gestor vuelve a invocar `mostrar...` hacia la Pantalla.

* **Gate de Verificación Íntegra (Fase 3):**
  * **Tests unitarios y de integración CU 28 $\rightarrow$ CU 29:** verificar `GestorNotificarRecepcionBolsin`, disparo post-commit exitoso y garantía de no envío en A6, Obs. 1 o ante fallo de persistencia (`rollback`).
  * **Tests de integración multi-remito:** cubrir Opción 1, A3, A4, A5 y notificación CU 29 sobre un bolsín con **2+ remitos y múltiples documentaciones**, validando la actualización de todos los remitos/documentos y el correo completo.
  * **Revisión de trazabilidad:** cada mensaje entrante y saliente del Gestor coincide 1:1 con [`message.md`](message.md) y su frontera pública con la Pantalla solo usa primitivos/`String`.

---

### Fase 4 — Desacople Estructural Total del Boundary (BCE Ultra-Estricto + Soporte Multi-Remito)
**Estado:** ⏳ **Pendiente** *(depende de Fase 3)*

* **Objetivo:** Erradicar por completo la violación crítica de arquitectura BCE en toda la capa `com.ppai.boundary.**` (`PantallaRecepcionBolsin`, `DocumentacionRow`, `InterfazServidorCorreo`), dejando la Pantalla comunicándose exclusivamente con `GestorRecepcionBolsin` mediante primitivos/`String` y con soporte funcional multi-remito en los 4 pasos *(el rediseño estético de colores, layout y textos finales queda reservado para Fase 5)*.
* **Cambios concretos:**
  1. Eliminar `import com.ppai.domain.model.cu28.*;` de [`PantallaRecepcionBolsin.java`](src/main/java/com/ppai/boundary/cu28/PantallaRecepcionBolsin.java), eliminar los imports de `Documentacion` y `OpcionRecepcion` de [`DocumentacionRow.java`](src/main/java/com/ppai/boundary/cu28/DocumentacionRow.java), y verificar **0 imports** de dominio en `InterfazServidorCorreo.java` (`com.ppai.boundary.cu29`).
  2. Eliminar todas las sobrecargas vacías o solo-para-tests en `PantallaRecepcionBolsin` (`Corrección #3`) y usar los parámetros reales de `mostrarNumeroPrecinto(...)` y `mostrarCMOrigen(...)` (sin `mostrarBolsines`).
  3. Reemplazar `TableView<Bolsin>`, `ObservableList<Bolsin>`, `Bolsin bolsinSeleccionadoEnTabla`, `ComboBox<ComisionMedica>` y `List<OpcionRecepcion>` por estructuras de primitivos/`String` (`String[][]`, `String[]`, `int`, `boolean`) y modelos visuales internos privados de JavaFX construidos únicamente desde primitivos/`String`, mostrando `Bolsin.numeroPrecinto` y `Remito.numero` como **números `int` limpios** (ej. `7041`, `458`, sin prefijos `"PREC-"`/`"REM-"` ni parseos).
  4. **Soporte multi-remito funcional en los 4 pasos de `PantallaRecepcionBolsin` (desde `String[][]` / primitivos):**
     - **Paso 1 (`tblBolsines`):** mostrar todos los números de remito asociados a cada bolsín (y cantidad de remitos/documentos).
     - **Paso 2 (`mostrarDatosDeRemito`):** visualizar cada remito del bolsín con su número y todas sus documentaciones, permitiendo marcar/desmarcar `¿Conforme?` en cualquier documento de cualquiera de los remitos para A3/A4.
     - **Paso 3 (Confirmación):** mostrar el desglose y estado resultante previsto de **todos** los remitos y todas sus documentaciones (recibidos desde el Gestor, sin calcular reglas en pantalla).
     - **Paso 4 (Resultado y correo CU 29):** mostrar todos los remitos actualizados y el comprobante del correo con el detalle completo.
  5. Eliminar de `PantallaRecepcionBolsin.mostrarDatosDeRemito(...)` las invocaciones directas `remito.buscarDocumentacion()` y `d.getDocumentacion()`: el Gestor ejecuta la lectura con los iteradores y entrega a la pantalla los datos en `String[][]` / `String[]`.
  6. Eliminar las navegaciones directas `gestor.getSesionActual().getUsuario().getEmpleado()...` y `getCMEmpleado()...`.
  7. Eliminar toda lógica de negocio y acoplamiento sutil en la Pantalla y en `DocumentacionRow`:
     - Cero `contains("CENTRAL")`, cero armado de emails, cero cálculo manual de estado resultante.
     - Cero `contains("No recibida" / "rechazada" / "parcial" / "aceptada")` en estilos de badges (usar estilos neutros o códigos visuales genéricos `"OK"`, `"WARN"`, `"ERROR"`, `"INFO"` enviados por el Gestor).
     - Cero literales de nombres de estados ni textos fijos de asunto/remitente del correo en la vista (`"Recibido en CM Destino"`, etc.): todos los textos los entrega el Gestor.
     - Cero `LocalDateTime.now()` ni `DateTimeFormatter` en la vista: todas las fechas llegan formateadas como `String` desde el Gestor.
     - Cero llamadas "pull" a getters de datos del Gestor desde eventos UI (ej. el botón `Limpiar` notifica al Gestor en vez de llamar `gestor.getBolsinesEnviados()`).
  8. Hacer que la Pantalla devuelva al Gestor únicamente primitivos y `String` (`int nroPrecinto` o `String` de búsqueda, `String nombreCMOrigen`, `int numeroOpcion`, `long[]`/`int[]` o `String[]` de identificadores de docs no conformes, `boolean confirmada`).

* **Gate de Verificación Íntegra (Fase 4 — Bloqueante):**
  * **Nuevo `ArquitecturaBCETest.java`:** escaneo estático de fuentes + inspección por reflexión de **todas las clases de `com.ppai.boundary.**`** (`PantallaRecepcionBolsin`, `DocumentacionRow`, `InterfazServidorCorreo`):
    - **0 imports** que no pertenezcan exclusivamente a `com.ppai.controller.**`, `java.*` o `javafx.*` (incluyendo **0 imports** de `com.ppai.domain.*`, `com.ppai.iterator.*`, `com.ppai.repository.*`, y **0 imports** de `java.time.LocalDateTime` / `java.time.format.DateTimeFormatter` en `PantallaRecepcionBolsin.java` y `DocumentacionRow.java`).
    - **0 campos, parámetros de métodos o tipos de retorno** pertenecientes a `com.ppai.domain.*`, `com.ppai.iterator.*`, `com.ppai.repository.*` ni a clases DTO externas en todo `com.ppai.boundary.**`.
    - **0 literales de nombres de estados del dominio** (`"Recibido en CM Destino"`, `"No recibida"`, `"Recibida y rechazada"`, `"Para redirigir"`, `"Recibido y aceptado"`, `"Recibida y aceptada"`, `"Recibido y aceptado parcial"`, `"En bolsín enviado"`) dentro de `PantallaRecepcionBolsin.java` y `DocumentacionRow.java`.

---

### Fase 5 — Diseño Visual, UX Definitiva y Pulido para Usuario Real *(Al Final, Previo a Cierre)*
**Estado:** ⏳ **Pendiente** *(depende de Fase 4; se define junto con el usuario antes de cerrar)*

* **Objetivo:** Definir junto con el usuario colores, estética, presentación visual de múltiples remitos y textos definitivos pensados para un Encargado de Bolsines real, eliminando toda exposición de nomenclatura técnica o académica.
* **Cambios concretos:**
  * Definir y ajustar paleta de colores, estilos CSS/JavaFX y disposición visual multi-remito en los 4 pasos.
  * Eliminar de `PantallaRecepcionBolsin.java`, `JavaFxApp.java` y textos visibles del correo CU 29 toda mención a `CU 28`, `CU 29`, `<<includes>>`, `Flujo Básico`, `Flujo A1..A6`, `Obs. 1`, `Iterador*`, `GoF` y `Mock:`.
  * Quitar el bloque `"RECORRIDO ITERATOR"` de la vista del Paso 2.
  * Eliminar por completo el selector `"Mock: Escenario 1 / Escenario 2"` de la cabecera (la sesión activa se lee directamente de la BD, conforme a **Corrección #8**).
  * Reescribir etiquetas, botones, alertas y badges en lenguaje claro del dominio operativo.

* **Gate de Verificación Íntegra (Fase 5):**
  * **Test estático de higiene UX:** verificar sobre `src/main/java/com/ppai/boundary/**` y `JavaFxApp.java` la ausencia de literales visibles con términos prohibidos (`CU 28`, `CU 29`, `includes`, `GoF`, `Iterador`, `Flujo Básico`, `Flujo A`, `(A1)`..`(A6)`, `Obs. 1`, `Mock:`).
  * **Recorrido visual completo en JavaFX:** probar los 4 pasos (incluyendo un bolsín con múltiples remitos y múltiples documentaciones), filtros sin coincidencia, opciones 1..4 (en CMC y CMJ Regional), cancelación y no confirmación.

---

### Fase 6 — Auditoría Integral End-to-End, Matriz 1:1 con Secuencia y Documentación Final
**Estado:** ⏳ **Pendiente** *(depende de Fase 5)*

* **Actividades:**
  1. Construir y auditar la **Matriz completa 1:1** (`message.md` $\leftrightarrow$ clase / método / línea real) sin excepciones ni métodos puente vacíos.
  2. Revisitar y confirmar explícitamente con el equipo la **regla multi-remito A3/A4** de **Corrección #10** (estado `RECIBIDO_Y_ACEPTADO_PARCIAL` solo en remitos con documentación observada vs. `RECIBIDO_Y_ACEPTADO` en los conformes) frente a la ERS.
  3. Ejecutar suite completa `mvn clean test` con JDK 21: `ArquitecturaBCETest`, `IteratorPatternTest`, `DominioPuroYContratosTest`, `CU28Test`, `CU29Test`, `PersistenciaCU28Test`.
  4. Ejecutar prueba end-to-end real en `mvn javafx:run`, confirmar una recepción (validando el caso multi-remito), cerrar completamente la aplicación, volver a abrirla y verificar que la recepción persiste en la base de datos en disco.
  5. Actualizar `SDD_CU28_Patron_Iterator.md`, `GUIA_ESTUDIO_DEFENSA_CU28.md` y READMEs reflejando la arquitectura definitiva.

* **Gate de Verificación Íntegra (Fase 6 — Cierre):**
  * `mvn clean test` en verde al 100% (cero tests omitidos o relajados).
  * Matriz 1:1 sin discrepancias y prueba de reinicio real de aplicación verificada.

---

## 4. Resumen de Fases y Gates de Salida

| Fase | Nombre | Estado | Gate de Salida (Verificación Íntegra) |
| :---: | :--- | :---: | :--- |
| **0** | Congelamiento del Contrato de Secuencia y Decisiones | ✅ Cerrada | `.md` 100% alineados con el DS real + todos los puntos abiertos resueltos |
| **1** | Patrón Iterator Exacto + Dominio OO Puro (Opción B) | ✅ Cerrada | Test pureza Dominio (0 JPA/JavaFX, 0 `id` artificiales, solapamiento E1+E2) + Test contratos estrictos por reflexión (`Object[]` en iteradores, `int` limpios) + filtro real sin look-ahead + tests multi-remito en Dominio |
| **2** | Data Mapper + H2 en disco + script seeder separado | ⏳ Pendiente | Test durabilidad en disco (bolsín 2+ remitos) tras cierre/reapertura EMF + Test no-destrucción al iniciar/`finCU()` + Test `MapeadorPersistenciaCU28` y `ReiniciarEscenarioBD` |
| **3** | CU 29 (`<<includes>>`) con Gestor Propio (Solo Primitivos) | ⏳ Pendiente | Tests `GestorNotificarRecepcionBolsin` post-commit + tests integración multi-remito (Opción 1, A3, A4, A5) + contratos hacia Pantalla solo primitivos / `String` |
| **4** | Desacople Estructural del Boundary (BCE + Multi-Remito) | ⏳ Pendiente | `ArquitecturaBCETest` bloqueante sobre `com.ppai.boundary.**` (0 imports fuera de `GestorRecepcionBolsin`/control/`java.*`/`javafx.*`; 0 fechas/`LocalDateTime`/literales de estados; frontera 100% primitivos/`String`) + UI multi-remito funcional en Pasos 1–4 |
| **5** | Diseño Visual, UX Definitiva y Usuario Real | ⏳ Pendiente | Colores/layout/textos acordados + test estático higiene UX + recorrido visual completo de los 4 pasos y alternativos |
| **6** | Auditoría Final End-to-End y Documentación | ⏳ Pendiente | Matriz 1:1 completa + `mvn clean test` 100% verde + prueba en disco tras reinicio de app |
