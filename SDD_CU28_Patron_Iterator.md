# SDD — Rediseño CU 28: Patrón Iterator Propio (`Registrar Recepción de Bolsín`)

## 1. Propósito y Alcance
Este documento de diseño de software (SDD) registra la implementación del patrón de diseño **Iterator (GoF)** construido desde cero en Java —sin utilizar `java.util.Iterator` ni `java.util.Iterable`— para el **Caso de Uso 28: Registrar Recepción de Bolsín**, siguiendo el modelo estático de `contexto-ppai-cu28.md`, la secuencia de interacción de `message.md` y preservando intacta la lógica original y flujos alternativos de `CU28_Registrar_Recepcion_Bolsin.md`.

---

## 2. Decisiones de Diseño y Restricciones
1. **Patrón Iterator 100% propio**: Ninguna clase ni interfaz del patrón importa, implementa ni extiende `java.util.Iterator` o `java.util.Iterable`.
2. **Contratos uniformes (`com.ppai.iterator`)**:
   - `IAgregado` define la operación de fabricación `IIterador crearIterador(Object[] elementos)`.
   - `IIterador` define `primero()`, `siguiente()`, `haFinalizado()`, `elementoActual()` y el alias `actual()`.
3. **Ubicación de la visualización en Pantalla (Corrección docente)**: Las invocaciones a `PantallaRecepcionBolsin` (`mostrarBolsines`, `mostrarNumeroPrecinto()`, `mostrarCMOrigen()`, `solicitarSeleccionBolsin()`) se ejecutan **fuera del bucle** de iteración de bolsines en `GestorRecepcionBolsin.tomarOpcionRegistrarRecepcionBolsin()`.
4. **Preservación de la secuencia original del CU 28**: Se mantienen sin alteraciones los flujos alternativos **A1** (filtro por precinto inexistente), **A2** (filtro por CM origen inexistente), **A3** (falta documentación), **A4** (documentación que no corresponde), **A5** (redirigir a otra área en CMC / bloqueo en CM regional), **A6** (no confirmación) y cancelación (`cancelarRecepcion` / `finCU`).

---

## 3. Vista Estática Implementada

### 3.1 Paquete `com.ppai.iterator`
| Tipo | Rol GoF | Miembros principales | Archivo |
| :--- | :--- | :--- | :--- |
| `IAgregado` | Aggregate | `crearIterador(Object[] elementos)` | `src/main/java/com/ppai/iterator/IAgregado.java` |
| `IIterador` | Iterator | `primero()`, `siguiente()`, `haFinalizado()`, `elementoActual()`, `actual()` | `src/main/java/com/ppai/iterator/IIterador.java` |
| `IteradorBolsin` | ConcreteIterator | `lista` (`Bolsin[]`), `posicionActual`, `filtros` (`Object[]`), `comprobarFiltro(...)` | `src/main/java/com/ppai/iterator/IteradorBolsin.java` |
| `IteradorRemito` | ConcreteIterator | `lista` (`Remito[]`), `posicionActual` | `src/main/java/com/ppai/iterator/IteradorRemito.java` |
| `IteradorDetalleRemito` | ConcreteIterator | `lista` (`DetalleRemito[]`), `posicionActual` | `src/main/java/com/ppai/iterator/IteradorDetalleRemito.java` |

### 3.2 Agregados Concretos y Colaboradores
- **`GestorRecepcionBolsin`** (`src/main/java/com/ppai/controller/cu28/GestorRecepcionBolsin.java`):
  - Implementa `IAgregado`.
  - `crearIterador(Object[])` / `crearIterador(Bolsin[])` crea `IteradorBolsin` con filtro `{ this.comisionMedicaEmpleado }`.
  - `buscarBolsinEstadoEnviado()` instancia el iterador, ejecuta `primero()`, consulta `haFinalizado()`, `actual()` y `siguiente()`, invocando `getNombreCMOrigen()` y `getNroPrecinto()` sobre cada bolsín filtrado.
  - Reinicia de forma segura las listas y referencias transitorias (`documentacionesNoConformes = new ArrayList<>()`, limpieza en `finCU()`, `tomarOpcionRegistrarRecepcionBolsin()` y `cancelarRecepcion()`) para soportar listas inmutables en flujos A3/A4 sin errores.
- **`Bolsin`** (`src/main/java/com/ppai/domain/model/cu28/Bolsin.java`):
  - Implementa `IAgregado` y crea `IteradorRemito`.
  - `obtenerInformacionRemito()` recorre los remitos mediante `IteradorRemito` invocando `getNumero()` y `buscarDocumentacion()`.
  - Expone `getNombreCMOrigen()`, `getNroPrecinto()`, `sosEnviado(...)` y `esTuCMDestino(...)`.
- **`Remito`** (`src/main/java/com/ppai/domain/model/cu28/Remito.java`):
  - Implementa `IAgregado` y crea `IteradorDetalleRemito`.
  - `buscarDocumentacion()` recorre los `DetalleRemito` mediante `IteradorDetalleRemito`, invocando `det.obtenerAsuntoYTipoDoc()`.
  - Centraliza la aceptación uniforme en `aceptar(Estado, LocalDateTime, Empleado)`.
- **`DetalleRemito`** (`src/main/java/com/ppai/domain/model/cu28/DetalleRemito.java`):
  - Incorpora `obtenerAsuntoYTipoDoc()` retornando `[asunto, tipoDocumento]` y soporte *null-safe* en `aceptar(...)`.
- **`ComisionMedica`** (`src/main/java/com/ppai/domain/model/cu28/ComisionMedica.java`):
  - Incorpora `esTuCMDestino(ComisionMedica)` *null-safe* y sobrecarga `esTuCMDestino(String)`.
- **`PantallaRecepcionBolsin`** (`src/main/java/com/ppai/boundary/cu28/PantallaRecepcionBolsin.java`):
  - Incorpora `mostrarNumeroPrecinto()` y `mostrarCMOrigen()` sin parámetros para la invocación fuera del bucle.
  - Sincroniza `bolsinSeleccionadoEnTabla` con `gestor.getBolsinSeleccionado()` en `mostrarDatosDeRemito(Remito)` y limpia selecciones transitorias en `habilitar()` y `mostrarBolsines(...)`.

---

## 4. Secuencia de Iteración Aplicada

```text
GestorRecepcionBolsin
  ├── tomarOpcionRegistrarRecepcionBolsin()
  │     ├── buscarUsuario()
  │     ├── pantalla.mostrarCM(comisionMedicaEmpleado)
  │     ├── buscarBolsinEstadoEnviado()
  │     │     ├── itBol = crearIterador(elementos)         -> IteradorBolsin(lista, filtros)
  │     │     ├── itBol.primero()                          -> posiciona en primer Bolsin válido (comprobarFiltro)
  │     │     └── while (!itBol.haFinalizado())
  │     │           ├── bolsin = (Bolsin) itBol.actual()   -> comprobarFiltro -> Bolsin
  │     │           ├── bolsin.getNombreCMOrigen()
  │     │           ├── bolsin.getNroPrecinto()
  │     │           └── itBol.siguiente()
  │     └── [FUERA DEL LOOP] pantalla.mostrarBolsines / mostrarNumeroPrecinto / mostrarCMOrigen / solicitarSeleccionBolsin
  │
  └── obtenerInformacionRemito()
        └── bolsinSeleccionado.obtenerInformacionRemito()
              ├── itRem = crearIterador(remitos)           -> IteradorRemito
              └── while (!itRem.haFinalizado())
                    ├── r.getNumero()
                    └── r.buscarDocumentacion()
                          ├── itDet = crearIterador(detalles) -> IteradorDetalleRemito
                          └── while (!itDet.haFinalizado())
                                └── det.obtenerAsuntoYTipoDoc()
```

---

## 5. Pruebas y Verificación

- **`src/test/java/com/ppai/IteratorPatternTest.java`** (14 tests):
  - Verificación por reflexión de ausencia total de `java.util.Iterator` y `java.util.Iterable` en `com.ppai.iterator`, `GestorRecepcionBolsin`, `Bolsin` y `Remito`.
  - Contratos de `IAgregado`, `IIterador`, `IteradorBolsin` (filtrado por estado `ENVIADO` y CM destino, reinicio con `primero()`, listas vacías/nulas), `IteradorRemito` e `IteradorDetalleRemito`.
- **`src/test/java/com/ppai/CU28Test.java`** (16 tests):
  - Flujo básico completo, flujos alternativos A1–A6, cancelación (Obs. 1), listas inmutables y aislamiento entre ejecuciones consecutivas.
- **Resultado de ejecución (`mvn clean test`)**:
  - `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0` (`CU28Test`: 16, `IteratorPatternTest`: 14, `MainTest`: 2) — **`BUILD SUCCESS`**.

