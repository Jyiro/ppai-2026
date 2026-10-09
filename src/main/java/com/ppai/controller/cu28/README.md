# 🧠 `controller/cu28` — Capa Control / Gestor (CU 28)

Este paquete contiene el **gestor del caso de uso** del CU 28:
_Registrar Recepción de Bolsín_.

---

## 🏛️ ¿Qué es un Control/Gestor en arquitectura BCE/ECB?

En el patrón **Boundary–Control–Entity**, el Control (o Gestor) es el
"orquestador" del caso de uso. Sus responsabilidades son:

- Mantener el **estado temporal** del CU (atributos de instancia).
- Coordinar los mensajes entre Boundary y Entidades.
- Contener la lógica de **flujo** del CU (no la lógica de negocio, que está en las entidades).

**Regla de oro:** El Gestor conoce a la Pantalla y a las Entidades.
La Pantalla solo conoce al Gestor. Las Entidades no conocen a nadie.

---

## 📋 Clases de este paquete

| Clase | Lifeline en DS | Responsabilidad |
|-------|----------------|-----------------|
| [`GestorRecepcionBolsin`](GestorRecepcionBolsin.java) | `: GestorRecepcionBolsin <<control>>` | Orquesta los 34 pasos del DS-CU28. Mantiene el estado temporal del caso de uso. |

---

## 🔗 Secuencia que implementa (DS-CU28)

```
tomarOpcionRegistrarRecepcionBolsin()  ← Paso 3
  └─ buscarUsuario()                    ← Pasos 4-7
  └─ mostrarCM(...)                     ← Paso 8 (a pantalla)
  └─ buscarBolsinEstadoEnviado()        ← Paso 9 + loop sección 2
  └─ [mostrar bolsines]                 ← Sección 3 (a pantalla)
  └─ [capturar selección EdB]          ← Pasos 10-12
  └─ tomarSeleccionOpcion()
      └─ obtenerInformacionRemito()     ← Pasos 13-14 + loops sección 4
      └─ [mostrar remito y opciones]   ← Pasos 15-17 (a pantalla)
      └─ [capturar opción y confirm.]  ← Pasos 18-22
      └─ tomarConfirmacionSeleccion()
          └─ obtenerEstado()            ← Paso 23 + loops sección 6
          └─ getFechaHoraActual()       ← Paso 24
          └─ actualizarEstadoBolsin()   ← Paso 25 → marcarRecibido() → [loops sección 7-8]
          └─ notificarRecepcion()       ← Pasos 31-32 (<<includes>> CU29)
          └─ informarCorrectoEnvio()   ← Paso 33 (a pantalla)
          └─ finCU()                    ← Paso 34
```

---

## 🔮 Puntos de extensión para patrones de diseño futuros

| Patrón | Dónde aplicarlo |
|--------|----------------|
| **Strategy** | `notificarRecepcion()` → inyectar `EstrategiaNotificacion` para desacoplar el CU29. |
| **Observer** | Al finalizar la recepción, el gestor puede notificar observers registrados. |
| **Iterator** | Los `for-each` sobre bolsines, estados y documentaciones pueden adaptarse a Iterators personalizados. |

---

## ⚠️ Notas del diagrama (correcciones del profesor)

1. **`marcarRecibidoYAceptado()` en Remito** — eliminado. Se usa `aceptar()` como único orquestador del loop de Remito (la máquina de estado del Remito no es prioritaria en esta entrega).
2. **Mostrar bolsines** — los mensajes `mostrarNumeroPrecinto()` y `mostrarCMOrigen()` van **fuera** del loop de búsqueda (se muestran todos de una vez).
3. **Loop paso 14** — `obtenerInformacionRemito()` aparece dentro de un loop en el diagrama original; el loop es interno (itera sobre los detalles del remito), no sobre múltiples llamadas al método.
