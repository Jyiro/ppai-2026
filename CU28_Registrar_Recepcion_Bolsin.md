# Diagrama de Secuencia – CU 28: Registrar Recepción de Bolsín

## Participantes (de izquierda a derecha)

| # | Lifeline | Estereotipo / Tipo |
|---|----------|--------------------|
| 1 | `: Encargado de Bolsines` (EdB) | Actor |
| 2 | `: PantallaRecepcionBolsin` | `<<boundary>>` |
| 3 | `: GestorRecepcionBolsin` | `<<control>>` |
| 4 | `actual : Sesion` | Entidad |
| 5 | `actual : Usuario` | Entidad |
| 6 | `Logueado : Empleado` | Entidad |
| 7 | `: Bolsin` | Entidad |
| 8 | `: CambioEstadoBolsin` | Entidad |
| 9 | `actual : CambioEstadoBolsin` | Entidad |
| 10 | `: Estado` | Entidad (estado de Bolsín) |
| 11 | `: ComisionMedica` | Entidad |
| 12 | `Seleccionado : Bolsin` | Entidad |
| 13 | `: Remito` | Entidad |
| 14 | `: DetalleRemito` | Entidad |
| 15 | `: Documentacion` | Entidad |
| 16 | `: Estado` | Entidad (estados de Bolsín / Remito / Documentación) |
| 17 | `: TipoDocumento` | Entidad |
| 18 | `: CambioEstadoDocumentacion` | Entidad |
| 19 | `actual : CambioEstadoDocumentacion` | Entidad |
| — | `nuevo : CambioEstadoBolsin` | Creado en el flujo (`new()`) |
| — | `nuevo : CambioEstadoDocumentacion` | Creado en el flujo (`new()`) |
| — | `C.U. 29` | Caso de uso incluido (`<<includes>>`) |

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

### 2. Búsqueda de bolsines en estado Enviado

9. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `buscarBolsinEstadoEnviado()`

**loop** `[Mientras haya bolsines]`
- `GestorRecepcionBolsin → Bolsin`: `sosEnviado()`
- `Bolsin → ComisionMedica`: `esTuCMDestino()`
- `Bolsin → CambioEstadoBolsin`: `sosActual()`
- `Bolsin → actual : CambioEstadoBolsin`: `sosEnviado()`
- `actual : CambioEstadoBolsin → Estado`: `esEnviado()`

**end loop**

### 3. Mostrar bolsines enviados

**loop** `[Mientras haya bolsines enviados]`
- `GestorRecepcionBolsin → Bolsin`: `getNumeroPrecinto()`
- `GestorRecepcionBolsin → Bolsin`: `getCMOrigen()`
- `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `mostrarNumeroPrecinto()`
- `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `mostrarCMOrigen()`
<<CORRECCION que nos hizo el profe: los mostrar van afuera de los loops, muestra todo de una vez..>>
**end loop**

### 4. Selección del bolsín y obtención del remito

10. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `solicitarSeleccionBolsin()`
11. `EdB → PantallaRecepcionBolsin`: `tomarSeleccionBolsin()`
12. `PantallaRecepcionBolsin → GestorRecepcionBolsin`: `tomarSeleccionOpcion()`
13. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `obtenerInformacionRemito()`
14. `GestorRecepcionBolsin → Seleccionado : Bolsin`: `obtenerInformacionRemito()`
<acá en la secuencia el 14 está en un loop... o sea, se mete a un loop, acá claude no lo hizo, no estoy seguro...>

**loop** `[Mientras haya documentaciones]`
- `Seleccionado : Bolsin → Remito`: `getNumero()`
- `Seleccionado : Bolsin → Remito`: `buscarDocumentacion()`
- `Remito → DetalleRemito`: `getDocumentacion()`
<acá otra vez es al loop, y no está reflejado acá..>

  **loop** `[Mientras haya Remitos]`
  - `DetalleRemito → Documentacion`: `getAsunto()`
  - `DetalleRemito → Documentacion`: `mostrarTipoDocumentacion()`
  - `Documentacion → TipoDocumento`: `getNombre()`

  **end loop**

**end loop**

### 5. Mostrar datos y confirmar opción de recepción

15. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `mostrarDatosDeRemito()`
16. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `mostrarOpcionesRecepcion()`
17. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `solicitarSeleccionOpcion()`
18. `EdB → PantallaRecepcionBolsin`: `tomarSeleccionOpcion()`
19. `PantallaRecepcionBolsin → GestorRecepcionBolsin`: `tomarSeleccionOpcion()`
20. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `solicitarConfirmacionSeleccion()`
21. `EdB → PantallaRecepcionBolsin`: `tomarConfirmacionSeleccion()`
22. `PantallaRecepcionBolsin → GestorRecepcionBolsin`: `tomarConfirmacionSeleccion()`

### 6. Obtención de los estados

23. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `obtenerEstado()`

**loop** `[Mientras haya Estados Bolsin]`
- `GestorRecepcionBolsin → Estado`: `esAmbitoBolsin()`
- `GestorRecepcionBolsin → Estado`: `esEstadoRecibidoEnCMDestino()`

**loop** `[Mientras haya Estados Remito]`
- `GestorRecepcionBolsin → Estado`: `esRecibidoYAceptado()`
- `GestorRecepcionBolsin → Estado`: `esAmbitoRemito()`

**loop** `[Mientras haya Estados Documentacion]`
- `GestorRecepcionBolsin → Estado`: `esAmbitoDocumentacion()`
- `GestorRecepcionBolsin → Estado`: `esRecibidoYAceptado()`

### 7. Actualización del estado del bolsín

24. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `getFechaHoraActual()`
25. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `actualizarEstadoBolsin()`
26. `GestorRecepcionBolsin → Seleccionado : Bolsin`: `marcarRecibido()`
27. `Seleccionado : Bolsin → CambioEstadoBolsin`: `sosActual()`
28. `Seleccionado : Bolsin → actual : CambioEstadoBolsin`: `setFechaHoraFin()`
29. `Seleccionado : Bolsin → Seleccionado : Bolsin`: `crearNuevoCambioDeEstado()`
30. `Seleccionado : Bolsin → nuevo : CambioEstadoBolsin`: `new()` *(creación)*

### 8. Actualización de remitos, detalles y documentación

**loop** `[Mientras haya Remitos]`
- `Seleccionado : Bolsin → Remito`: `marcarRecibidoYAceptado()`
- `Seleccionado : Bolsin → Remito`: `aceptar()`

  **loop** `[Mientras haya DetalleRemitos]`
  - `Remito → DetalleRemito`: `aceptar()`
  - `DetalleRemito → Documentacion`: `aceptar()`
  - `Documentacion → CambioEstadoDocumentacion`: `sosActual()`
  - `Documentacion → actual : CambioEstadoDocumentacion`: `setFechaHoraFin()`
  - `Documentacion → Documentacion`: `newCambioEstadoDocumentacion()`
  - `Documentacion → nuevo : CambioEstadoDocumentacion`: `new()` *(creación)*

  **end loop**

**end loop**

### 9. Notificación y fin del CU

31. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `notificarRecepcion()`
32. `GestorRecepcionBolsin → C.U. 29`: `<<includes>>`
33. `GestorRecepcionBolsin → PantallaRecepcionBolsin`: `informarCorrectoEnvioNotificacion()`
34. `GestorRecepcionBolsin → GestorRecepcionBolsin`: `finCU()`

---

## Versión Mermaid

```mermaid
sequenceDiagram
    actor EdB as : Encargado de Bolsines
    participant Pant as : PantallaRecepcionBolsin <<boundary>>
    participant Gest as : GestorRecepcionBolsin <<control>>
    participant Ses as actual : Sesion
    participant Usu as actual : Usuario
    participant Emp as Logueado : Empleado
    participant Bol as : Bolsin
    participant CEB as : CambioEstadoBolsin
    participant CEBa as actual : CambioEstadoBolsin
    participant Est as : Estado
    participant CM as : ComisionMedica
    participant BolSel as Seleccionado : Bolsin
    participant Rem as : Remito
    participant Det as : DetalleRemito
    participant Doc as : Documentacion
    participant Est2 as : Estado
    participant TD as : TipoDocumento
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
    Gest->>Gest: buscarBolsinEstadoEnviado()

    loop Mientras haya bolsines
        Gest->>Bol: sosEnviado()
        Bol->>CM: esTuCMDestino()
        Bol->>CEB: sosActual()
        Bol->>CEBa: sosEnviado()
        CEBa->>Est: esEnviado()
    end

    loop Mientras haya bolsines enviados
        Gest->>Bol: getNumeroPrecinto()
        Gest->>Bol: getCMOrigen()
        Gest->>Pant: mostrarNumeroPrecinto()
        Gest->>Pant: mostrarCMOrigen()
    end

    Gest->>Pant: solicitarSeleccionBolsin()
    EdB->>Pant: tomarSeleccionBolsin()
    Pant->>Gest: tomarSeleccionOpcion()
    Gest->>Gest: obtenerInformacionRemito()
    Gest->>BolSel: obtenerInformacionRemito()

    loop Mientras haya documentaciones
        BolSel->>Rem: getNumero()
        BolSel->>Rem: buscarDocumentacion()
        Rem->>Det: getDocumentacion()
        loop Mientras haya Remitos
            Det->>Doc: getAsunto()
            Det->>Doc: mostrarTipoDocumentacion()
            Doc->>TD: getNombre()
        end
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
        Gest->>Est2: esAmbitoBolsin()
        Gest->>Est2: esEstadoRecibidoEnCMDestino()
    end
    loop Mientras haya Estados Remito
        Gest->>Est2: esRecibidoYAceptado()
        Gest->>Est2: esAmbitoRemito()
    end
    loop Mientras haya Estados Documentacion
        Gest->>Est2: esAmbitoDocumentacion()
        Gest->>Est2: esRecibidoYAceptado()
    end

    Gest->>Gest: getFechaHoraActual()
    Gest->>Gest: actualizarEstadoBolsin()
    Gest->>BolSel: marcarRecibido()
    BolSel->>CEB: sosActual()
    BolSel->>CEBa: setFechaHoraFin()
    BolSel->>BolSel: crearNuevoCambioDeEstado()
    BolSel->>CEBn: new()

    loop Mientras haya Remitos
        BolSel->>Rem: marcarRecibidoYAceptado() 
        <<Quitamos el método marcarRecibidoYAceptado() y dejamos el aceptar() como orquestador, corrección del profesor.. ya que en nuestra entrega no es importante la máquina de estado del remito sino la de la documentación>>
        BolSel->>Rem: aceptar()
        loop Mientras haya DetalleRemitos
            Rem->>Det: aceptar()
            Det->>Doc: aceptar()
            Doc->>CED: sosActual()
            Doc->>CEDa: setFechaHoraFin()
            Doc->>Doc: newCambioEstadoDocumentacion()
            Doc->>CEDn: new()
        end
    end

    Gest->>Gest: notificarRecepcion()
    Gest->>CU29: <<includes>>
    Gest->>Pant: informarCorrectoEnvioNotificacion()
    Gest->>Gest: finCU()
```

---

## Notas de transcripción

- Los bucles `[Mientras haya bolsines]` y `[Mientras haya bolsines enviados]` aparecen superpuestos en el diagrama original; los transcribí como bucles consecutivos.
- Las flechas `sosActual()` / `sosEnviado()` / `setFechaHoraFin()` se asignaron a `CambioEstadoBolsin` / `actual : CambioEstadoBolsin` (y las equivalentes de Documentación) según la columna a la que apuntan en el dibujo.
- El bucle interno `[Mientras haya Remitos]` dentro de `[Mientras haya documentaciones]` está tal cual figura en el diagrama (puede ser un error de rotulado del original).
- Las tres secuencias `esAmbito...` / `esRecibido...` apuntan a la lifeline `: Estado` ubicada a la derecha (columna 16).

# Important!
Comentar sobre la gestión de los métodos que se está en duda si deberían ir dentro de loop o no (los que tienen comentarios <así..>)