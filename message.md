--- START OF FILE CU28_Registrar_Recepcion_Bolsin.md ---

# Diagrama de Secuencia de Diseño (con Patrón Iterator) – CU 28: Registrar Recepción de Bolsín

## Participantes (de izquierda a derecha)

| # | Lifeline | Estereotipo / Tipo |
|---|----------|--------------------|
| 1 | `: Encargado de Bolsines` (EdB) | Actor |
| 2 | `: PantallaRecepcionBolsin` | `<<boundary>>` |
| 3 | `: GestorRecepcionBolsin` | `<<control>>` / `IAgregado` |
| 4 | `actual : Sesion` | Entidad |
| 5 | `actual : Usuario` | Entidad |
| 6 | `Logueado : Empleado` | Entidad |
| 7 | `: IteradorBolsin` | `<<IIterador>>` (Creado en el flujo) |
| 8 | `: Bolsin` | Entidad |
| 9 | `: CambioEstadoBolsin` | Entidad |
| 10 | `actual : CambioEstadoBolsin` | Entidad |
| 11 | `: Estado` | Entidad (estado de Bolsín) |
| 12 | `: ComisionMedica` | Entidad |
| 13 | `Seleccionado : Bolsin` | Entidad / `IAgregado` |
| 14 | `: IteradorRemito` | `<<IIterador>>` (Creado en el flujo) |
| 15 | `: Remito` | Entidad / `IAgregado` |
| 16 | `: IteradorDetalleRemito` | `<<IIterador>>` (Creado en el flujo) |
| 17 | `: DetalleRemito` | Entidad |
| 18 | `: Documentacion` | Entidad |
| 19 | `: TipoDocumento` | Entidad |
| 20 | `: Estado` | Entidad (estados de Bolsín / Documentación) |
| 21 | `: CambioEstadoDocumentacion` | Entidad |
| 22 | `actual : CambioEstadoDocumentacion` | Entidad |
| — | `nuevo : CambioEstadoBolsin` | Creado en el flujo (`new()`) |
| — | `nuevo : CambioEstadoDocumentacion` | Creado en el flujo (`new()`) |
| — | `C.U. 29` | Caso de uso incluido (`<<includes>>`) |

---

## Secuencia de mensajes (en orden)

### 1. Inicio e identificación de la Comisión Médica

1. `EdB → PantallaRecepcionBolsin`: `tomarOpcionRegistrarRecepcionBolsin()`
2. `PantallaRecepcionBolsin → PantallaRecepcionBolsin`: `habilitar()`
3. `PantallaRecepcionBolsin → GestorRecepcionBolsin`: `tomarOpcionRegistrarRecepcionBolsin()`
4. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `buscarUsuario()`
5. `GestorRecepcionBolsin → actual : Sesion`: `getUsuario()`
6. `actual : Sesion → actual : Usuario`: `getCMEmpleado()`
7. `actual : Usuario → Logueado : Empleado`: `getCM()`
8. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `mostrarCM()`

### 2. Búsqueda de bolsines en estado Enviado (Patrón Iterator - IteradorBolsin)

9. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `buscarBolsinEstadoEnviado(): void` (o `String[][]`)
10. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `crearIterador(elementos: Bolsin[]): IIterador`
11. `GestorRecepcionBolsin --> IteradorBolsin`: `new(elementos: Bolsin[], filtros: Object[]): IteradorBolsin` *(el filtro contiene `cmUsuario`)*
12. `GestorRecepcionBolsin → IteradorBolsin`: `primero(): void`

**loop de bolsines** `[Mientras haFinalizado() == false]`
- `GestorRecepcionBolsin → IteradorBolsin`: `haFinalizado(): boolean`
- `GestorRecepcionBolsin → IteradorBolsin`: `actual(): Object`
  - `IteradorBolsin → IteradorBolsin`: `comprobarFiltro(filtro: Object[]): boolean`
  - `IteradorBolsin → Bolsin`: `sosEnviado(): boolean`
    - `Bolsin → ComisionMedica`: `esTuCMDestino(cmUsuario: String): boolean`
    - `Bolsin → CambioEstadoBolsin`: `*sosActual(): boolean`
    - `Bolsin → actual : CambioEstadoBolsin`: `sosEnviado(): boolean`
    - `actual : CambioEstadoBolsin → Estado`: `esEnviado(): boolean`
- `GestorRecepcionBolsin → Bolsin`: `getNombreCMOrigen(): String` *(método nuevo de diseño)*
  - `Bolsin → ComisionMedica`: `getNombre(): String` *(método nuevo de diseño)*
- `GestorRecepcionBolsin → Bolsin`: `getNroPrecinto(): int`
- `GestorRecepcionBolsin → IteradorBolsin`: `siguiente(): void`

**end loop**

### 3. Mostrar bolsines enviados (Fuera del loop según corrección del profesor)

13. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `mostrarNumeroPrecinto()`
14. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `mostrarCMOrigen()`
15. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `solicitarSeleccionBolsin()`

### 4. Selección del bolsín y obtención de remitos y documentación (Patrón Iterator - IteradorRemito e IteradorDetalleRemito)

16. `EdB → PantallaRecepcionBolsin`: `tomarSeleccionBolsin()`
17. `PantallaRecepcionBolsin → GestorRecepcionBolsin`: `tomarSeleccionBolsin()`
18. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `obtenerInformacionRemito(): void`
19. `GestorRecepcionBolsin → Seleccionado : Bolsin`: `obtenerInformacionRemito(): String[][]`
20. `Seleccionado : Bolsin → Seleccionado : Bolsin`: `crearIterador(elementos: Remito[]): IIterador`
21. `Seleccionado : Bolsin --> IteradorRemito`: `new(elementos: Remito[]): IteradorRemito`
22. `Seleccionado : Bolsin → IteradorRemito`: `primero(): void`

**loop de remitos** `[Mientras haFinalizado() == false]`
- `Seleccionado : Bolsin → IteradorRemito`: `haFinalizado(): boolean`
- `Seleccionado : Bolsin → IteradorRemito`: `actual(): Object`
- `Seleccionado : Bolsin → Remito`: `getNumero(): int`
- `Seleccionado : Bolsin → Remito`: `buscarDocumentacion(): String[][]`
  - `Remito → Remito`: `crearIterador(elementos: DetalleRemito[]): IIterador`
  - `Remito --> IteradorDetalleRemito`: `new(elementos: DetalleRemito[]): IteradorDetalleRemito`
  - `Remito → IteradorDetalleRemito`: `primero(): void`

  **loop de detalle remitos** `[Mientras haFinalizado() == false]`
  - `Remito → IteradorDetalleRemito`: `haFinalizado(): boolean`
  - `Remito → IteradorDetalleRemito`: `actual(): Object`
  - `Remito → DetalleRemito`: `obtenerAsuntoYTipoDoc(): String[]` *(método nuevo de diseño)*
    - `DetalleRemito → Documentacion`: `getAsunto(): String`
    - `DetalleRemito → Documentacion`: `mostrarTipoDocumentacion(): String`
      - `Documentacion → TipoDocumento`: `getNombre(): String`
  - `Remito → IteradorDetalleRemito`: `siguiente(): void`
  **end loop**

- `Seleccionado : Bolsin → IteradorRemito`: `siguiente(): void`
**end loop**

### 5. Mostrar datos y confirmar opción de recepción

23. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `mostrarDatosDeRemito()`
24. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `mostrarOpcionesRecepcion()`
25. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `solicitarSeleccionOpcion()`
26. `EdB → PantallaRecepcionBolsin`: `tomarSeleccionOpcion()`
27. `PantallaRecepcionBolsin → GestorRecepcionBolsin`: `tomarSeleccionOpcion()`
28. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `solicitarConfirmacionSeleccion()`
29. `EdB → PantallaRecepcionBolsin`: `tomarConfirmacionSeleccion()`
30. `PantallaRecepcionBolsin → GestorRecepcionBolsin`: `tomarConfirmacionSeleccion()`

### 6. Obtención de los estados

31. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `obtenerEstado()`

**loop** `[Mientras haya Estados Bolsin]`
- `GestorRecepcionBolsin → Estado`: `esAmbitoBolsin(): boolean`
- `GestorRecepcionBolsin → Estado`: `esEstadoRecibidoEnCMDestino(): boolean`
**end loop**

**loop** `[Mientras haya Estados Documentacion]`
- `GestorRecepcionBolsin → Estado`: `esAmbitoDocumentacion(): boolean`
- `GestorRecepcionBolsin → Estado`: `esRecibidoYAceptado(): boolean`
**end loop**

### 7. Actualización del estado del bolsín

32. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `getFechaHoraActual()`
33. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `actualizarEstadoBolsin()`
34. `GestorRecepcionBolsin → Seleccionado : Bolsin`: `marcarRecibido(fechaHoraActual, estadoRecibidoEnCMDestino, empleado)`
35. `Seleccionado : Bolsin → CambioEstadoBolsin`: `*sosActual(): boolean`
36. `Seleccionado : Bolsin → actual : CambioEstadoBolsin`: `setFechaHoraFin(fechaHoraActual)`
37. `Seleccionado : Bolsin → Seleccionado : Bolsin`: `crearNuevoCambioDeEstado()`
38. `Seleccionado : Bolsin --> nuevo : CambioEstadoBolsin`: `new(fechaHoraInicio, estadoRecibidoEnCMDestino, empleado)` *(creación)*

### 8. Actualización de remitos, detalles y documentación (Corregido según indicación del profesor)

39. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `actualizarRemitoYDocumentacion()`
40. `GestorRecepcionBolsin → Seleccionado : Bolsin`: `actualizarRemitoYDocumentacion(fechaHoraActual, estadoRecibidaYAceptada, empleado)`

**loop** `[Mientras haya Remitos]`
- `Seleccionado : Bolsin → Remito`: `aceptar(fechaHoraActual, estadoRecibidaYAceptada, empleado)` *(orquestador único en Remito)*

  **loop** `[Mientras haya DetalleRemitos]`
  - `Remito → DetalleRemito`: `aceptar(fechaHoraActual, estadoRecibidaYAceptada, empleado)` *(o `actualizarEstadoDoc()`)*
  - `DetalleRemito → Documentacion`: `aceptar(fechaHoraActual, estadoRecibidaYAceptada, empleado)`
  - `Documentacion → CambioEstadoDocumentacion`: `*sosActual(): boolean`
  - `Documentacion → actual : CambioEstadoDocumentacion`: `setFechaHoraFin(fechaHoraActual)`
  - `Documentacion → Documentacion`: `newCambioEstadoDocumentacion()`
  - `Documentacion --> nuevo : CambioEstadoDocumentacion`: `new(fechaHoraInicio, estadoRecibidaYAceptada, empleado)` *(creación)*
  **end loop**

**end loop**

### 9. Notificación y fin del CU

41. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `notificarRecepcion()`
42. `GestorRecepcionBolsin → C.U. 29`: `<<includes>>`
43. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `informarCorrectoEnvioNotificacion()`
44. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `finCU()`

---

## Versión Mermaid (Actualizada con Patrón Iterator)

```mermaid
sequenceDiagram
    actor EdB as : Encargado de Bolsines
    participant Pant as : PantallaRecepcionBolsin <<boundary>>
    participant Gest as : GestorRecepcionBolsin <<control>>
    participant Ses as actual : Sesion
    participant Usu as actual : Usuario
    participant Emp as Logueado : Empleado
    participant ItBol as : IteradorBolsin
    participant Bol as : Bolsin
    participant CEB as : CambioEstadoBolsin
    participant CEBa as actual : CambioEstadoBolsin
    participant Est as : Estado
    participant CM as : ComisionMedica
    participant BolSel as Seleccionado : Bolsin
    participant ItRem as : IteradorRemito
    participant Rem as : Remito
    participant ItDet as : IteradorDetalleRemito
    participant Det as : DetalleRemito
    participant Doc as : Documentacion
    participant TD as : TipoDocumento
    participant Est2 as : Estado
    participant CED as : CambioEstadoDocumentacion
    participant CEDa as actual : CambioEstadoDocumentacion
    participant CEBn as nuevo : CambioEstadoBolsin
    participant CEDn as nuevo : CambioEstadoDocumentacion
    participant CU29 as C.U. 29

    EdB->>Pant: tomarOpcionRegistrarRecepcionBolsin()
    Pant->>Pant: habilitar()
    Pant->>Gest: tomarOpcionRegistrarRecepcionBolsin()
    Gest->>Gest: buscarUsuario()
    Gest->>Ses: getUsuario()
    Ses->>Usu: getCMEmpleado()
    Usu->>Emp: getCM()
    Gest->>Pant: mostrarCM()

    Gest->>Gest: buscarBolsinEstadoEnviado(): void
    Gest->>Gest: crearIterador(elementos: Bolsin[]): IIterador
    Gest-->>ItBol: new(elementos: Bolsin[], filtros: Object[]): IteradorBolsin
    Gest->>ItBol: primero(): void

    loop loop de bolsines [Mientras haFinalizado() == false]
        Gest->>ItBol: haFinalizado(): boolean
        Gest->>ItBol: actual(): Object
        ItBol->>ItBol: comprobarFiltro(filtro: Object[]): boolean
        ItBol->>Bol: sosEnviado(): boolean
        Bol->>CM: esTuCMDestino(cmUsuario: String): boolean
        Bol->>CEB: *sosActual(): boolean
        Bol->>CEBa: sosEnviado(): boolean
        CEBa->>Est: esEnviado(): boolean
        Gest->>Bol: getNombreCMOrigen(): String
        Bol->>CM: getNombre(): String
        Gest->>Bol: getNroPrecinto(): int
        Gest->>ItBol: siguiente(): void
    end

    Gest->>Pant: mostrarNumeroPrecinto()
    Gest->>Pant: mostrarCMOrigen()
    Gest->>Pant: solicitarSeleccionBolsin()
    EdB->>Pant: tomarSeleccionBolsin()
    Pant->>Gest: tomarSeleccionBolsin()

    Gest->>Gest: obtenerInformacionRemito(): void
    Gest->>BolSel: obtenerInformacionRemito(): String[][]
    BolSel->>BolSel: crearIterador(elementos: Remito[]): IIterador
    BolSel-->>ItRem: new(elementos: Remito[]): IteradorRemito
    BolSel->>ItRem: primero(): void

    loop loop de remitos [Mientras haFinalizado() == false]
        BolSel->>ItRem: haFinalizado(): boolean
        BolSel->>ItRem: actual(): Object
        BolSel->>Rem: getNumero(): int
        BolSel->>Rem: buscarDocumentacion(): String[][]
        Rem->>Rem: crearIterador(elementos: DetalleRemito[]): IIterador
        Rem-->>ItDet: new(elementos: DetalleRemito[]): IteradorDetalleRemito
        Rem->>ItDet: primero(): void

        loop loop de detalle remitos [Mientras haFinalizado() == false]
            Rem->>ItDet: haFinalizado(): boolean
            Rem->>ItDet: actual(): Object
            Rem->>Det: obtenerAsuntoYTipoDoc(): String[]
            Det->>Doc: getAsunto(): String
            Det->>Doc: mostrarTipoDocumentacion(): String
            Doc->>TD: getNombre(): String
            Rem->>ItDet: siguiente(): void
        end

        BolSel->>ItRem: siguiente(): void
    end

    Gest->>Pant: mostrarDatosDeRemito()
    Gest->>Pant: mostrarOpcionesRecepcion()
    Gest->>Pant: solicitarSeleccionOpcion()
    EdB->>Pant: tomarSeleccionOpcion()
    Pant->>Gest: tomarSeleccionOpcion()
    Gest->>Pant: solicitarConfirmacionSeleccion()
    EdB->>Pant: tomarConfirmacionSeleccion()
    Pant->>Gest: tomarConfirmacionSeleccion()

    Gest->>Gest: obtenerEstado()
    loop Mientras haya Estados Bolsin
        Gest->>Est2: esAmbitoBolsin(): boolean
        Gest->>Est2: esEstadoRecibidoEnCMDestino(): boolean
    end
    loop Mientras haya Estados Documentacion
        Gest->>Est2: esAmbitoDocumentacion(): boolean
        Gest->>Est2: esRecibidoYAceptado(): boolean
    end

    Gest->>Gest: getFechaHoraActual()
    Gest->>Gest: actualizarEstadoBolsin()
    Gest->>BolSel: marcarRecibido()
    BolSel->>CEB: *sosActual(): boolean
    BolSel->>CEBa: setFechaHoraFin()
    BolSel->>BolSel: crearNuevoCambioDeEstado()
    BolSel-->>CEBn: new()

    Gest->>Gest: actualizarRemitoYDocumentacion()
    Gest->>BolSel: actualizarRemitoYDocumentacion()
    loop Mientras haya Remitos
        BolSel->>Rem: aceptar()
        loop Mientras haya DetalleRemitos
            Rem->>Det: aceptar()
            Det->>Doc: aceptar()
            Doc->>CED: *sosActual(): boolean
            Doc->>CEDa: setFechaHoraFin()
            Doc->>Doc: newCambioEstadoDocumentacion()
            Doc-->>CEDn: new()
        end
    end

    Gest->>Gest: notificarRecepcion()
    Gest->>CU29: <<includes>>
    Gest->>Pant: informarCorrectoEnvioNotificacion()
    Gest->>Gest: finCU()