# 📦 `domain/model/cu28` — Entidades del Dominio (CU 28)

Este paquete contiene todas las **entidades del dominio** del Caso de Uso 28:
_Registrar Recepción de Bolsín_.

---

## 🏛️ ¿Qué es una entidad en arquitectura BCE/ECB?

En el patrón **Boundary–Control–Entity**, las entidades representan los datos y
las reglas de negocio del sistema. Son las "cosas" con las que opera el negocio.
**No conocen** ni a la Pantalla ni al Gestor.

---

## 📋 Entidades de este paquete

| Clase | Lifeline en DS | Responsabilidad |
|-------|----------------|-----------------|
| [`Estado`](Estado.java) | `: Estado` (col. 10 y 16) | Estado genérico de Bolsín, Remito o Documentación. Contiene `esEnviado()`, `esAmbitoBolsin()`, etc. |
| [`Sesion`](Sesion.java) | `actual : Sesion` | Sesión activa del sistema. Provee `getUsuario()`. |
| [`Usuario`](Usuario.java) | `actual : Usuario` | Usuario autenticado. Provee `getCMEmpleado()`. |
| [`Empleado`](Empleado.java) | `Logueado : Empleado` | Empleado logueado. Provee `getCM()`. |
| [`ComisionMedica`](ComisionMedica.java) | `: ComisionMedica` | CM del sistema. Provee `esTuCMDestino()`. |
| [`CambioEstadoBolsin`](CambioEstadoBolsin.java) | `: CambioEstadoBolsin`, `actual :`, `nuevo :` | Registro histórico de estados de un Bolsín. |
| [`CambioEstadoDocumentacion`](CambioEstadoDocumentacion.java) | `: CambioEstadoDocumentacion`, `actual :`, `nuevo :` | Registro histórico de estados de una Documentación. |
| [`TipoDocumento`](TipoDocumento.java) | `: TipoDocumento` | Categoría de una Documentación. Provee `getNombre()`. |
| [`Documentacion`](Documentacion.java) | `: Documentacion` | Documento adjunto. Provee `getAsunto()`, `mostrarTipoDocumentacion()`, `aceptar()`. |
| [`DetalleRemito`](DetalleRemito.java) | `: DetalleRemito` | Línea del remito con una documentación. Provee `getDocumentacion()`, `aceptar()`. |
| [`Remito`](Remito.java) | `: Remito` | Remito del bolsín. Provee `getNumero()`, `buscarDocumentacion()`, `aceptar()`. |
| [`Bolsin`](Bolsin.java) | `: Bolsin`, `Seleccionado : Bolsin` | Entidad principal. Agrupa remito, cambios de estado y CM. |

---

## 🔗 Trazabilidad con el diagrama de secuencia

Cada método público lleva en su Javadoc el paso exacto del DS-CU28 al que corresponde.
Ejemplo: `// DS-CU28 paso 5` indica que ese mensaje corresponde al paso 5 del diagrama.

---

## 🔮 Puntos de extensión para patrones de diseño futuros

| Patrón | Dónde aplicarlo |
|--------|----------------|
| **State** | `Bolsin`, `Documentacion` y sus `CambioEstado*`. Los métodos `esEnviado()`, `esRecibidoYAceptado()`, etc., son candidatos a polimorfismo. Subclases en `domain/state/cu28/`. |
| **Iterator** | Las listas `cambiosDeEstado`, `detalles` en `Remito`. |
| **Observer** | `Bolsin.marcarRecibido()` puede notificar observers al cambiar de estado. |
