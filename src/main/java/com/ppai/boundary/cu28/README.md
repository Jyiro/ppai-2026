# 🎛️ `boundary/cu28` — Capa Boundary (CU 28)

Este paquete contiene la **interfaz de usuario** del Caso de Uso 28:
_Registrar Recepción de Bolsín_.

---

## 🏛️ ¿Qué es un Boundary en arquitectura BCE/ECB?

En el patrón **Boundary–Control–Entity**, el boundary es el "punto de contacto"
entre el actor externo (usuario) y el sistema. Sus responsabilidades son:

- Recibir las acciones del usuario.
- Mostrar información al usuario.
- Delegar **toda** la lógica de negocio al **Gestor (Control)**.

**Regla de oro:** El Boundary nunca accede directamente al dominio.
Solo habla con el Gestor.

---

## 📋 Clases de este paquete

| Clase | Lifeline en DS | Responsabilidad |
|-------|----------------|-----------------|
| [`PantallaRecepcionBolsin`](PantallaRecepcionBolsin.java) | `: PantallaRecepcionBolsin <<boundary>>` | Interfaz de consola para el EdB. Implementa todos los mensajes recibidos desde el actor y desde el gestor. |

---

## 🔗 Mensajes implementados (trazabilidad DS-CU28)

| Paso | Mensaje | Dirección |
|------|---------|-----------|
| 1 | `tomarOpcionRegistrarRecepcionBolsin()` | EdB → Pantalla |
| 2 | `habilitar()` | Pantalla → Pantalla (auto) |
| 8 | `mostrarCM(cm)` | Gestor → Pantalla |
| 3er loop | `mostrarNumeroPrecinto(p)` | Gestor → Pantalla |
| 3er loop | `mostrarCMOrigen(cm)` | Gestor → Pantalla |
| 10 | `solicitarSeleccionBolsin()` | Gestor → Pantalla |
| 11 | `tomarSeleccionBolsin()` | EdB → Pantalla |
| 12 | `tomarSeleccionOpcion(sel)` | Pantalla → Gestor |
| 15 | `mostrarDatosDeRemito(datos)` | Gestor → Pantalla |
| 16 | `mostrarOpcionesRecepcion()` | Gestor → Pantalla |
| 17 | `solicitarSeleccionOpcion()` | Gestor → Pantalla |
| 18 | `tomarSeleccionOpcionRecepcion()` | EdB → Pantalla |
| 20 | `solicitarConfirmacionSeleccion()` | Gestor → Pantalla |
| 21 | `tomarConfirmacionSeleccion()` | EdB → Pantalla |
| 33 | `informarCorrectoEnvioNotificacion()` | Gestor → Pantalla |

---

## 🔮 Punto de extensión — Patrón Adapter

Si en el futuro la interfaz cambia de **consola** a **GUI** (Swing, JavaFX) o **Web**:

1. Extraer una interfaz `IBoundaryRecepcionBolsin`.
2. Crear la implementación nueva (ej: `PantallaRecepcionBolsinWeb`).
3. El Gestor solo conoce la interfaz → cero cambios en él.
