# Contexto PPAI 2026 — Bolsines · CU 28 + Máquina de estados de Documentación

> Archivo de contexto para trabajar el proyecto (diagramas, código e interfaces).
> Fuentes: ERS v1.2, Consignas, Consignas UX, Descripción del Caso v1.1 y Modelo de Dominio v11 (cátedra DSI, UTN FRC).

---

## 1. Resumen

| Ítem | Valor |
|---|---|
| Materia | Diseño de Sistemas de Información (DSI) — UTN FRC, Ing. en Sistemas |
| Trabajo | PPAI 2026 — caso **Bolsines** |
| Asignación (grupo par) | **CU 28 – Registrar recepción de bolsín** |
| Máquina de estados | **Clase Documentación** |
| Patrón GoF aplicado (E2) | **Iterator** (con 3 agregados concretos y 3 iteradores concretos) |
| Persona UX | Encargado de Bolsines (EB) |
| Tareas del proyecto | Diagramas de las vistas · ayuda con el código · interfaces (GUI) |
| Stack elegido | Java + **JavaFX 21** + Maven, aplicación de **escritorio**, VSCode en Windows |

**Dominio:** un Organismo Controlador (sede central en CABA, +80 Comisiones Médicas Jurisdiccionales y una Comisión Médica Central) envía documentación física entre comisiones médicas por correo, dentro de **bolsines** cerrados con **precinto**. El sistema registra documentación, remitos, bolsines y su trazabilidad (estado + empleado responsable en cada cambio).

### Datos de entrega (completar)
- Curso: `3K_` · Grupo: `G__` · Docente tutor: `____`
- Fechas E1 / E2: `____`
- Nombre de archivo: `PPAI2026_3K<N>_G<NroGrupo>_<E1|E2|R1|R2>_<Analisis|Arquitectura|Diseño>.pdf` (solo PDF, lo sube 1 integrante).
- Toda entrega lleva carátula completa (integrantes con legajo y mail), encabezado/pie, páginas numeradas, títulos en cada modelo. No se aceptan entregas parciales.

---

## 2. Glosario

| Término | Significado |
|---|---|
| CM / CMJ / CMC | Comisión Médica / Jurisdiccional / Central (CABA) |
| CM origen / destino | CM que envía / CM que recibe |
| EB | Encargado de Bolsines (actor principal del CU 28) |
| Bolsín | Bolsa de tela que transporta documentación hacia **una única CM destino** |
| Precinto | Cierre numerado del bolsín; si se reabre, se cambia por uno nuevo |
| Remito | Detalle de la documentación enviada; **uno por CM destino**; un bolsín puede tener varios |
| Documentación | Nº secuencial, tipo de documento, fecha de pase, CM origen, asunto, archivos |
| Tipo Documento | Expediente, dictamen, estudio médico, carta documento, etc. |

---

## 3. CU 28 — Registrar recepción de bolsín

- **Actor principal:** Encargado de Bolsines · **Actor secundario:** no aplica (incluye CU 29 → Servidor de Correo)
- **Objetivo:** registrar la recepción de un bolsín actualizando el estado de los remitos y la documentación según la situación.

### Flujo básico
1. EB selecciona la opción para registrar la recepción de un bolsín.
2. Sistema busca y muestra la CM del usuario logueado.
3. Sistema busca bolsines en estado **Enviado** cuya CM destino es la del usuario (encuentra al menos uno).
4. Sistema muestra de cada bolsín su **CM origen** y **nº de precinto**; permite seleccionar del listado o filtrar por esos datos.
5. EB selecciona un bolsín.
6. Sistema muestra, por cada remito: nº de remito; y por cada documentación: **asunto** y **tipo**.
7. Sistema muestra las 4 opciones de recepción y pide elegir una:
   1. El contenido es igual al registrado
   2. No se recibe toda la documentación
   3. Existe documentación que no corresponde al destino
   4. La documentación se debe redirigir a otra área
8. EB elige la opción 1.
9. Sistema pide confirmación.
10. EB confirma.
11. Sistema actualiza: bolsín → **Recibido en CM destino** (+ empleado responsable); remitos → **Recibido y aceptado** (en nuestro alcance se orquesta con `aceptar()` hacia la documentación); documentación → **Recibida y aceptada** (+ empleado responsable).
12. Sistema **incluye CU 29 Notificar recepción de bolsín** (mail a cada CM origen con la situación de la documentación). Informa éxito. Fin.

### Flujos alternativos
| Id | Situación |
|---|---|
| A1 | No se encuentra bolsín con el nº de precinto ingresado |
| A2 | No se encuentra bolsín con la CM origen ingresada |
| A3 | EB elige "No se recibe toda la documentación" |
| A4 | EB elige "Existe documentación que no corresponde al destino" |
| A5 | EB elige "La documentación se debe redirigir a otra área" |
| A6 | EB no confirma la opción elegida |

**Obs. 1:** el EB puede cancelar el CU en cualquier momento.
**Obs. 2:** la situación de la documentación se rige por la regla de negocio "Recepción de un Bolsín en una CM Destino" (sección 4).

---

## 4. Regla de negocio — Recepción en CM destino (efecto por opción)

En todos los casos el **bolsín** pasa a **Recibido en CM destino**.

| Opción | Remito | Documentación |
|---|---|---|
| 1 · Contenido igual (flujo básico) | Recibido y aceptado | Recibida y aceptada |
| 2 · Falta documentación (A3) | **Recibido y aceptado parcial** | La que llegó → Recibida y aceptada · la que no llegó → **No recibida** (se notifica a la CM origen) |
| 3 · No corresponde al destino (A4) | *No especificado en la ERS* | La que no corresponde → **Recibida y rechazada** (vuelve al origen para asociarse a un nuevo remito) |
| 4 · Redirigir a otra área (A5) | *No especificado en la ERS* | **Para redirigir** (solo aplica si la CM destino es la **CMC**; el pase al área es interno, sin bolsín) |

**Trazabilidad:** cada cambio de estado registra fecha/hora y **empleado responsable**.

---

## 5. Máquina de estados — Documentación

### Estados (nombres según ERS)
`Registrada` · `En remito` · `En bolsín saliente` · `En bolsín enviado` · `Recibida y aceptada` · `No recibida` · `Recibida y rechazada` · `Para redirigir` · `De baja`

- **Inicial:** Registrada · **Finales:** Recibida y aceptada, De baja

### Transiciones

| Desde | Hacia | CU | Condición / nota | Método sugerido |
|---|---|---|---|---|
| (inicio) | Registrada | 7 Registrar Documentación | — | `new()` / `registrar()` |
| Registrada | En remito | 15 Generar Remito · 16 Modificar Remito (agregar) | — | `remitar()` |
| En remito | Registrada | 17 Cancelar Remito · 16 Modificar Remito (quitar) | [remito en estado creado / no asociado a bolsín] | `volverARegistrada()` |
| Registrada | De baja | 9 Eliminar Documentación | [no está en remito ni en bolsín] | `darDeBaja()` |
| En remito | En bolsín saliente | 19 Generar Bolsín · 20 Modificar Bolsín (agregar remito) | — | `incluirEnBolsin()` |
| En bolsín saliente | En remito | 20 Modificar Bolsín (quitar remito) · 21 Eliminar Bolsín | [bolsín en CM origen, no enviado] | `quitarDeBolsin()` |
| En bolsín saliente | En bolsín enviado | 27 Registrar retiro de bolsines | [bolsín cerrado → enviado] | `enviar()` |
| En bolsín enviado | Recibida y aceptada | **28** (opción 1; y doc. recibida en A3/A4) | — | `aceptar()` |
| En bolsín enviado | No recibida | **28** (A3) | [la documentación no llegó] | `marcarNoRecibida()` |
| En bolsín enviado | Recibida y rechazada | **28** (A4) | [no corresponde a la CM destino] | `rechazar()` |
| En bolsín enviado | Para redirigir | **28** (A5) | [CM destino es la CMC] | `paraRedirigir()` |
| Para redirigir | Recibida y aceptada | Recepción interna del área (sin CU numerado) | [corresponde al área] | `aceptar()` |
| Para redirigir | Recibida y rechazada | Recepción interna del área (sin CU numerado) | [no corresponde al área] | `rechazar()` |
| No recibida | Registrada | 31 Registrar Revisión de Documentación | [se reenvía] | `volverARegistrada()` |
| No recibida | De baja | 31 Registrar Revisión de Documentación | [ya no es relevante] | `darDeBaja()` |
| Recibida y rechazada | Registrada | 31 Registrar Revisión de Documentación | [se reenvía a la CM correcta] | `volverARegistrada()` |
| Recibida y rechazada | De baja | 31 Registrar Revisión de Documentación | [se da de baja] | `darDeBaja()` |

---

## 6. Entrega 1 — Análisis

1. **Vista de clases de análisis:** `PantallaRecepcionBolsin` (`<<boundary>>`), `GestorRecepcionBolsin` (`<<control>>`), `Sesion`, `Usuario`, `Empleado`, `ComisionMedica`, `Bolsin`, `CambioEstadoBolsin`, `Remito`, `DetalleRemito`, `Documentacion`, `CambioEstadoDocumentacion`, `TipoDocumento`, `Estado`.
2. **Vista de interacción:** Diagrama de secuencia del flujo básico aplicando GRASP (ver `CU28_Registrar_Recepcion_Bolsin.md`).
3. **Máquina de estados de Documentación** (sección 5).

---

## 7. Entrega 2 — Diseño e implementación (Patrón Iterator)

### 7.1. Problema que resuelve y justificación del Patrón Iterator
En el Caso de Uso 28 es necesario realizar tres recorridos sobre colecciones de distinta naturaleza y con diferentes criterios de filtrado:
1. Recorrer la colección de **Bolsines** del sistema filtrando aquellos que estén en estado *Enviado* y cuya CM destino coincida con la CM del usuario logueado (`cmUsuario`).
2. Recorrer la colección de **Remitos** asociados al bolsín seleccionado.
3. Recorrer la colección de **Detalles de Remito** (`DetalleRemito`) de cada remito para obtener el asunto y tipo de cada documentación.

Aplicando el patrón **Iterator (GoF)** se desacopla a los agregados (`GestorRegistrarRecepcionBolsin`, `Bolsin` y `Remito`) de la estructura interna de las colecciones y de la responsabilidad de mantener el puntero de recorrido y la evaluación de filtros, delegando estas responsabilidades en clases de fabricación pura (`IteradorBolsin`, `IteradorRemito`, `IteradorDetalleRemito`) que implementan una interfaz común `IIterador`.

### 7.2. Estructura del Patrón Iterator (Diagrama de Clases de Diseño)

#### Interfaces del Patrón
* **`<<interface>> IAgregado`**
  * `+ crearIterador(elementos: Object[]): IIterador`
  * *Relaciones:* Dependencia (`..>`) hacia `IIterador` (por el tipo de retorno de `crearIterador()`).
* **`<<interface>> IIterador`**
  * `+ elementoActual(): Object`
  * `+ haFinalizado(): boolean`
  * `+ primero(): void`
  * `+ siguiente(): void`

#### Iteradores Concretos (implementan `IIterador`)
* **`IteradorBolsin`**
  * **Atributos:**
    * `- lista: Bolsin[]`
    * `- posicionActual: int`
    * `- filtros: Object[]` *(almacena `cmUsuario`)*
  * **Métodos:**
    * `+ comprobarFiltro(filtro: Object[]): boolean`
    * `+ elementoActual(): Object`
    * `+ haFinalizado(): boolean`
    * `+ primero(): void`
    * `+ siguiente(): void`
    * `+ new(elementos: Object[], filtros: Object[]): IteradorBolsin`
  * *Relaciones:* Realiza `IIterador`. Dependencia (`..>`) hacia `Bolsin`.
* **`IteradorRemito`**
  * **Atributos:**
    * `- lista: Remito[]`
    * `- posicionActual: int`
  * **Métodos:**
    * `+ elementoActual(): Object`
    * `+ haFinalizado(): boolean`
    * `+ primero(): void`
    * `+ siguiente(): void`
    * `+ new(elementos: Object[]): IteradorRemito`
  * *Relaciones:* Realiza `IIterador`. Dependencia (`..>`) hacia `Remito`.
* **`IteradorDetalleRemito`**
  * **Atributos:**
    * `- lista: DetalleRemito[]`
    * `- posicionActual: int`
  * **Métodos:**
    * `+ elementoActual(): Object`
    * `+ haFinalizado(): boolean`
    * `+ primero(): void`
    * `+ siguiente(): void`
    * `+ new(elementos: Object[]): IteradorDetalleRemito`
  * *Relaciones:* Realiza `IIterador`. Dependencia (`..>`) hacia `DetalleRemito`.

#### Agregados Concretos (implementan `IAgregado`) y Entidades vinculadas

* **`GestorRegistrarRecepcionBolsin`** (`<<control>>` — Primer Agregado Concreto)
  * **Atributos:**
    * `- cmUsuario: String`
    * `- bolsinesEnviados: Bolsin[]`
    * `- bolsinSeleccionado: Bolsin`
    * `- opcionSeleccionada: String`
    * `- empleado: Empleado`
    * `- fechaHoraActual: DateTime`
    * `- estadoRecibidoEnCMDestino: Estado`
    * `- estadoRecibidoYAceptado: Estado`
    * `- estadoRecibidaYAceptada: Estado`
  * **Métodos (foco en Iterator y CU 28):**
    * `+ buscarBolsinEstadoEnviado(): String[][]` (o `String[]` / `void`)
    * `+ obtenerInformacionRemito(): void`
    * `+ registrarRecepcionBolsin(): void`
    * `+ crearIterador(elementos: Bolsin[]): IIterador`
  * *Relaciones:* Realiza `IAgregado`. Dependencia de creación hacia `IteradorBolsin`. Asociaciones/dependencias hacia `Bolsin`, `Estado`, `ComisionMedica`, `Empleado`, `Sesion`, `PantallaRecepcionBolsin`.

* **`Bolsin`** (Segundo Agregado Concreto)
  * **Atributos:**
    * `- fecha: DateTime`
    * `- numeroBolsin: int`
    * `- numeroPrecinto: int`
    * `- peso: double`
  * **Asociaciones (Roles):**
    * `remito: Remito[1..*]` (Agregación)
    * `cambioEstadoTexto` / `cambioEstadoBolsin: CambioEstadoBolsin[1..*]`
    * `cmOrigen: ComisionMedica[1]`
    * `cmDestino: ComisionMedica[1]`
  * **Métodos:**
    * `+ getNroPrecinto(): int`
    * `+ sosEnviado(): boolean`
    * `+ obtenerInformacionRemito(): String[][]`
    * `+ getNombreCMOrigen(): String` *(nuevo método de diseño)*
    * `+ crearIterador(elementos: Remito[]): IIterador`
    * `+ esTuCMDestino(cmUsuario: String): boolean`
    * `+ marcarRecibido(...)` / `+ crearNuevoCambioDeEstado(...)` / `+ actualizarRemitoYDocumentacion(...)`
  * *Relaciones:* Realiza `IAgregado`. Dependencia de creación hacia `IteradorRemito`.

* **`Remito`** (Tercer Agregado Concreto)
  * **Atributos:**
    * `- fecha: DateTime`
    * `- numero: int`
  * **Asociaciones (Roles):**
    * `detalleRemito: DetalleRemito[1..*]` (Agregación)
    * `cmOrigen: ComisionMedica[1]`
    * `cmDestino: ComisionMedica[1]`
    * `estadoRemito: Estado[1]`
  * **Métodos:**
    * `+ getNumero(): int`
    * `+ buscarDocumentacion(): String[][]`
    * `+ crearIterador(elementos: DetalleRemito[]): IIterador`
    * `+ aceptar(...)`
  * *Relaciones:* Realiza `IAgregado`. Dependencia de creación hacia `IteradorDetalleRemito`.

* **`DetalleRemito`**
  * **Atributos:**
    * `- areaCMCDestino: String`
  * **Asociaciones (Roles):**
    * `documentacion: Documentacion[1]`
  * **Métodos:**
    * `+ obtenerAsuntoYTipoDoc(): String[]` *(nuevo método de diseño que reemplaza a `getDocumentacion()`)*
    * `+ aceptar(...)` / `+ actualizarEstadoDoc(...)`

* **`Documentacion`**
  * **Atributos:**
    * `- asunto: String`
    * `- fechaPase: DateTime`
    * `- numero: int`
  * **Asociaciones (Roles):**
    * `tipoDoc: TipoDocumento[1]`
    * `cambioEstadoDoc: CambioEstadoDocumentacion[1..*]`
    * `cmOrigen: ComisionMedica[1]`
  * **Métodos:**
    * `+ getAsunto(): String`
    * `+ mostrarTipoDocumentacion(): String`
    * `+ aceptar(...)` / `+ newCambioEstadoDocumentacion(...)`

* **`TipoDocumento`**
  * **Atributos:** `- descripcion: String`, `- nombre: String`
  * **Métodos:** `+ getNombre(): String`

* **`ComisionMedica`**
  * **Atributos:** `- codigo: int`, `- direccion: String`, `- email: String`, `- nombre: String`, `- telefono: String`
  * **Métodos:** `+ esTuCMDestino(cmUsuario: String): boolean`, `+ getNombre(): String`

* **`CambioEstadoBolsin`**
  * **Atributos:** `- fechaHoraFin: DateTime`, `- fechaHoraInicio: DateTime`
  * **Asociaciones (Roles):** `estado: Estado[1]`
  * **Métodos:** `+ sosActual(): boolean`, `+ sosEnviado(): boolean`, `+ setFechaHoraFin(...)`

* **`CambioEstadoDocumentacion`**
  * **Atributos:** `- fechaHoraFin: DateTime`, `- fechaHoraInicio: DateTime`
  * **Asociaciones (Roles):** `estado: Estado[1]`, `empleado: Empleado[1]`
  * **Métodos:** `+ sosActual(): boolean`, `+ setFechaHoraFin(...)`

* **`Estado`**
  * **Atributos:** `- nombre: String`, `- descripcion: String`, `- ambito: String`
  * **Métodos:** `+ esEnviado(): boolean`, `+ esAmbitoBolsin(): boolean`, `+ esEstadoRecibidoEnCMDestino(): boolean`, `+ esAmbitoDocumentacion(): boolean`, `+ esRecibidoYAceptado(): boolean`

#### Diagrama de Clases de Diseño en Mermaid

```mermaid
classDiagram
    class IAgregado {
        <<interface>>
        +crearIterador(elementos: Object[]) IIterador
    }

    class IIterador {
        <<interface>>
        +elementoActual() Object
        +haFinalizado() boolean
        +primero() void
        +siguiente() void
    }

    IAgregado ..> IIterador : dependencia (retorno)

    class IteradorBolsin {
        -lista: Bolsin[]
        -posicionActual: int
        -filtros: Object[]
        +comprobarFiltro(filtro: Object[]) boolean
        +elementoActual() Object
        +haFinalizado() boolean
        +primero() void
        +siguiente() void
        +new(elementos: Object[], filtros: Object[]) IteradorBolsin
    }

    class IteradorRemito {
        -lista: Remito[]
        -posicionActual: int
        +elementoActual() Object
        +haFinalizado() boolean
        +primero() void
        +siguiente() void
        +new(elementos: Object[]) IteradorRemito
    }

    class IteradorDetalleRemito {
        -lista: DetalleRemito[]
        -posicionActual: int
        +elementoActual() Object
        +haFinalizado() boolean
        +primero() void
        +siguiente() void
        +new(elementos: Object[]) IteradorDetalleRemito
    }

    IIterador <|.. IteradorBolsin
    IIterador <|.. IteradorRemito
    IIterador <|.. IteradorDetalleRemito

    class GestorRegistrarRecepcionBolsin {
        -cmUsuario: String
        -bolsinesEnviados: Bolsin[]
        -bolsinSeleccionado: Bolsin
        -opcionSeleccionada: String
        -empleado: Empleado
        -fechaHoraActual: DateTime
        -estadoRecibidoEnCMDestino: Estado
        -estadoRecibidoYAceptado: Estado
        -estadoRecibidaYAceptada: Estado
        +buscarBolsinEstadoEnviado() String[][]
        +obtenerInformacionRemito() void
        +registrarRecepcionBolsin() void
        +crearIterador(elementos: Bolsin[]) IIterador
    }

    class Bolsin {
        -fecha: DateTime
        -numeroBolsin: int
        -numeroPrecinto: int
        -peso: double
        +getNroPrecinto() int
        +sosEnviado() boolean
        +obtenerInformacionRemito() String[][]
        +getNombreCMOrigen() String
        +crearIterador(elementos: Remito[]) IIterador
        +esTuCMDestino(cmUsuario: String) boolean
    }

    class Remito {
        -fecha: DateTime
        -numero: int
        +getNumero() int
        +buscarDocumentacion() String[][]
        +crearIterador(elementos: DetalleRemito[]) IIterador
    }

    class DetalleRemito {
        -areaCMCDestino: String
        +obtenerAsuntoYTipoDoc() String[]
    }

    class Documentacion {
        -asunto: String
        -fechaPase: DateTime
        -numero: int
        +getAsunto() String
        +mostrarTipoDocumentacion() String
    }

    class TipoDocumento {
        -descripcion: String
        -nombre: String
        +getNombre() String
    }

    class ComisionMedica {
        -codigo: int
        -direccion: String
        -email: String
        -nombre: String
        -telefono: String
        +esTuCMDestino(cmUsuario: String) boolean
        +getNombre() String
    }

    class CambioEstadoBolsin {
        -fechaHoraFin: DateTime
        -fechaHoraInicio: DateTime
        +sosActual() boolean
        +sosEnviado() boolean
    }

    class Estado {
        -nombre: String
        -descripcion: String
        -ambito: String
        +esEnviado() boolean
    }

    IAgregado <|.. GestorRegistrarRecepcionBolsin
    IAgregado <|.. Bolsin
    IAgregado <|.. Remito

    GestorRegistrarRecepcionBolsin ..> IteradorBolsin : <<create>>
    Bolsin ..> IteradorRemito : <<create>>
    Remito ..> IteradorDetalleRemito : <<create>>

    IteradorBolsin ..> Bolsin
    IteradorRemito ..> Remito
    IteradorDetalleRemito ..> DetalleRemito

    Bolsin o--> "1..*" Remito : remito
    Bolsin --> "1..*" CambioEstadoBolsin : cambioEstadoTexto
    Bolsin --> "1" ComisionMedica : cmOrigen
    Bolsin --> "1" ComisionMedica : cmDestino

    Remito o--> "1..*" DetalleRemito : detalleRemito
    Remito --> "1" ComisionMedica : cmOrigen
    Remito --> "1" ComisionMedica : cmDestino
    Remito --> "1" Estado : estadoRemito

    DetalleRemito --> "1" Documentacion : documentacion
    Documentacion --> "1" TipoDocumento : tipoDoc
    CambioEstadoBolsin --> "1" Estado : estado
8. UX e interfaces
Actividades en aula (persona EB): MoodBoard, Mapa de empatía, User Journey (antes / durante / después). Guardar fotos para la defensa.
Diseño de GUI aplicando patrones de diseño de interfaces; se defiende en la presentación oral.
Preferencias de estilo: estética moderna, interacciones como Drag & Drop.
Pantallas del CU 28 (propuesta)
Bolsines por recibir: CM del usuario + tabla de bolsines enviados (CM origen, nº precinto) con filtros (A1: filtro por nº precinto sin resultados; A2: filtro por CM origen sin resultados).
Detalle del bolsín: remitos → documentación (asunto, tipo).
Opción de recepción: 4 opciones; en A3/A4 marcar qué documentación no llegó / no corresponde.
Confirmación (con opción de no confirmar → A6).
Resultado + aviso de notificación enviada (CU 29). Botón Cancelar siempre visible (Obs. 1).
9. Pendientes y puntos resueltos

Patrón GoF acordado: Iterator (IAgregado, IIterador, IteradorBolsin, IteradorRemito, IteradorDetalleRemito).

Corrección del profesor en secuencia: los métodos mostrar... hacia la pantalla van fuera del loop de búsqueda de bolsines.

Corrección del profesor en actualización de estados: en Remito se omite marcarRecibidoYAceptado() y queda aceptar() como orquestador hacia DetalleRemito y Documentacion.

Curso, nº de grupo, tutor y fechas.

BD y framework de persistencia (ej. SQLite / H2 / PostgreSQL + JPA/Hibernate o JDBC).
10. Fuentes del proyecto
2026 DSI Bolsines ERS V12.pdf — reglas de negocio, CU 28, listado de CU, RNF.
2026 DSI Bolsines Consignas.pdf — contenido de E1/E2 y criterios.
2026 DSI Bolsines Consignas UX.pdf — MoodBoard, empatía, journey.
2026 DSI Bolsines Descripción del Caso V1.1.pdf — dominio y glosario.
2026 DSI Bolsines Modelo de Dominio V 11.pdf — diagrama de clases del dominio (imagen)