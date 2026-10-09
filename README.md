# PPAI 2026 — Sistema de Bolsines Médicos

Proyecto base para la materia **Diseño de Sistemas de Información (DSI) — PPAI 2026**.
Implementa casos de uso de un sistema de gestión de bolsines de documentación médica
entre Comisiones Médicas.

---

## 🏗️ Arquitectura del Proyecto

El proyecto sigue el patrón **BCE/ECB** (Boundary–Control–Entity):

```
src/main/java/com/ppai/
│
├── boundary/              ← <<boundary>>: Interfaces de usuario (Pantallas)
│   ├── PantallaEjemplo.java   (demo base)
│   └── cu28/
│       └── PantallaRecepcionBolsin.java
│
├── controller/            ← <<control>>: Gestores de casos de uso
│   ├── GestorEjemplo.java     (demo base)
│   └── cu28/
│       └── GestorRecepcionBolsin.java
│
├── domain/
│   ├── model/             ← <<entity>>: Entidades del dominio
│   │   ├── EjemploEntidad.java    (demo base)
│   │   └── cu28/          ← Entidades del CU28 (12 clases)
│   └── state/             ← Preparado para patrón State
│       └── cu28/          ← Reservado (ver README interno)
│
├── repository/            ← Infraestructura de persistencia (JPA/Hibernate)
│   └── JpaUtil.java
│
└── Main.java              ← Punto de entrada
```

---

## 📖 Casos de Uso Implementados

| CU | Nombre | Estado | README |
|----|--------|--------|--------|
| CU-28 | Registrar Recepción de Bolsín | ✅ Completo | [Ver diagrama](CU28_Registrar_Recepcion_Bolsin.md) |
| CU-29 | Notificación de Recepción | 🔲 Stub | Pendiente |

---

## 🚀 Cómo ejecutar la Interfaz Visual JavaFX

```bash
# Opción 1: Ejecutar directamente con Maven Exec
mvn clean compile exec:java

# Opción 2: Ejecutar con el plugin específico de JavaFX
mvn javafx:run

# Opción 3: Ejecutar pruebas automatizadas (Flujo Básico y Alternativos A1..A6)
mvn test
```

---

## 🔮 Patrones de diseño preparados (no implementados aún)

El código está estructurado para incorporar estos patrones sin refactorings invasivos:

| Patrón | Dónde | Señal en código |
|--------|-------|----------------|
| **State** | `domain/state/cu28/` | Métodos `esEnviado()`, `esAmbitoBolsin()` en `Estado.java` |
| **Strategy** | `GestorRecepcionBolsin.notificarRecepcion()` | Comentario `// TODO: EstrategiaNotificacion` |
| **Observer** | Finalización del CU28 | Comentario en `notificarRecepcion()` |
| **Iterator** | Listas de bolsines, estados, detalles | Uso de `List<>` con `for-each` reemplazable |
| **Adapter** | `boundary/cu28/` y CU29 | Estructura de interfaz única entre Pantalla y Gestor |

---

## 📐 Convenciones

- **Nombres de clases/métodos**: exactamente como figuran en el diagrama de secuencia.
- **Comentarios**: `// DS-CU28 paso N` para trazabilidad directa al diagrama.
- **Javadoc**: en toda clase y método público.
- **Paquetes CU**: `com.ppai.{capa}.cu28` para todo lo perteneciente al CU28.

---

## 🛠️ Stack tecnológico

| Tecnología | Versión | Uso |
|-----------|---------|-----|
| Java | 21 | Lenguaje principal |
| Maven | 3.x | Build y dependencias |
| Hibernate ORM | 6.5.2 | Persistencia (JPA) |
| H2 Database | 2.2.224 | Base de datos en memoria |
| JUnit 5 | 5.10.2 | Testing |
| SLF4J | 2.0.13 | Logging |
