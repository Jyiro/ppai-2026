# Plan de Mejoras y Correcciones Técnicas — CU 28: Recepción de Bolsines

Este documento reúne los puntos pendientes detectados para alinear el código con la corrección del Diagrama de Secuencia, limpiar el acoplamiento arquitectónico, mejorar la experiencia de usuario (UX) real y separar el dominio puro de la persistencia real en base de datos.

---

## 1. Patrón Iterator: Corrección de Filtro y Unificación de `actual()`

### 1.1 Corregir lo que devuelve `IteradorBolsin` al evaluar el filtro
* **Problema actual:** En [`IteradorBolsin.java`](src/main/java/com/ppai/iterator/IteradorBolsin.java#L114-L121), se invoca `comprobarFiltro(this.filtros)` pero se ignora su valor booleano de retorno y siempre se devuelve `lista[posicionActual]`:
  ```java
  comprobarFiltro(this.filtros);
  return lista[posicionActual];
  ```
* **Qué hacer:** Agregar el `if` para que el método solo retorne el elemento si realmente cumple el filtro (y `null` en caso contrario):
  ```java
  if (comprobarFiltro(this.filtros)) {
      return lista[posicionActual];
  }
  return null;
  ```
* **Impacto:** Garantiza que cualquier invocación directa a `actual()` respete el filtro de negocio (`ENVIADO` + CM destino), incluso si no se avanzó previamente o si cambió el estado del elemento.

### 1.2 Renombrar / Unificar `elementoActual()` por `actual()`
* **Problema actual:** La interfaz [`IIterador`](src/main/java/com/ppai/iterator/IIterador.java#L34-L41) y sus 3 implementaciones (`IteradorBolsin`, `IteradorRemito`, `IteradorDetalleRemito`) mantienen dos métodos (`elementoActual()` y el alias `actual()`), cuando en la corrección del Diagrama de Secuencia se unificó el nombre del mensaje a **`actual()`**.
* **Qué hacer:**
  * Eliminar `elementoActual()` de `IIterador`, `IteradorBolsin`, `IteradorRemito` e `IteradorDetalleRemito`.
  * Dejar `actual()` como único método oficial del contrato, implementando allí la lógica directa (con el `if (comprobarFiltro(...))` en `IteradorBolsin`).
  * Actualizar los tests en `IteratorPatternTest.java` que llamen a `elementoActual()` para que utilicen `actual()`.

---

## 2. Desacoplar la Lógica de Negocio de la Pantalla (`PantallaRecepcionBolsin`)

* **Problema actual:** [`PantallaRecepcionBolsin.java`](src/main/java/com/ppai/boundary/cu28/PantallaRecepcionBolsin.java) tiene acoplamiento directo con entidades del dominio y contiene reglas que pertenecen al controlador o al dominio:
  * Importa entidades (`Bolsin`, `Remito`, `Documentacion`, `ComisionMedica`, `OpcionRecepcion`).
  * En `mostrarDatosDeRemito(Remito)` llama directamente a `remito.buscarDocumentacion()`.
  * En `construirCardsOpciones(...)` duplica la regla de negocio `esDestinoCMC` (`nombre.contains("CENTRAL") || nombre.contains("CMC")`), la cual ya existe en `GestorRecepcionBolsin`.
  * En `solicitarConfirmacionSeleccion(...)` construye manualmente la dirección de email de origen (`despacho.<cm>@srt.gob.ar`), lógica duplicada con `GestorRecepcionBolsin.notificarRecepcion()`.
* **Qué hacer:**
  * Centralizar en `ComisionMedica` (o en el `Gestor`) el método `esCentral()` y la obtención/formato del correo institucional.
  * Hacer que la pantalla reciba únicamente **DTOs / ViewModels** o datos simples desde el `Gestor`, sin navegar métodos de entidades ni calcular reglas de negocio.
  * Dejar la pantalla exclusivamente dedicada a renderizar componentes JavaFX, capturar eventos del usuario y delegar en el `Gestor`.

---

## 3. UX Pensada para el Usuario Real (Sin Jerga Técnica ni Académica)

* **Problema actual:** La interfaz gráfica ([`PantallaRecepcionBolsin.java`](src/main/java/com/ppai/boundary/cu28/PantallaRecepcionBolsin.java) y [`JavaFxApp.java`](src/main/java/com/ppai/JavaFxApp.java)) expone términos internos de análisis/diseño que un Encargado de Bolsines real nunca debería ver:
  * Referencias a casos de uso: `"CU 28"`, `"CU 29"`, `"(<<includes>> CU 29)"`.
  * Nombres de flujos alternativos y observaciones: `"Flujo Básico"`, `"Flujo A3"`, `"Flujo A4"`, `"Flujo A5"`, `"No Confirmar (A6)"`, `"Cancelar (Obs. 1)"`.
  * Referencias al patrón de diseño y arquitectura: `"RECORRIDO ITERATOR"`, `"IteradorRemito → IteradorDetalleRemito"`, `"Lectura desacoplada (GoF)"`, `"IteradorBolsin filtró..."`.
  * Controles de prueba en cabecera: selector `"Mock: Escenario 1 / Escenario 2"`.
* **Qué hacer:**
  * Reescribir títulos, subtítulos, badges, alertas y botones con lenguaje natural operativo del dominio (ej.: *"Confirmar Recepción"*, *"Cancelar operación"*, *"Faltan documentos"*, *"Documentación con error de destino"*, *"Redirigir internamente"*).
  * Eliminar el panel de `"RECORRIDO ITERATOR"` de la vista del Paso 2.
  * Ocultar o mover los controles de cambio de escenario/usuario de prueba fuera de la interfaz principal productiva (o reemplazarlos por un flujo real de inicio/cambio de sesión si se requiere).

---

## 4. Simular el `<<includes>>` del CU 29 Creando Otro Gestor

* **Problema actual:** En [`GestorRecepcionBolsin.notificarRecepcion()`](src/main/java/com/ppai/controller/cu28/GestorRecepcionBolsin.java#L516-L549), el mismo gestor del CU 28 arma el cuerpo del correo, construye `NotificacionRecepcionBolsin` y llama directamente a `InterfazServidorCorreo`.
* **Qué hacer:**
  * Crear un controlador dedicado para el caso de uso incluido, por ejemplo `GestorNotificarRecepcionBolsin` (o `GestorNotificacionCU29`) en `com.ppai.controller.cu29` (o `cu28`).
  * Mover a ese nuevo gestor la responsabilidad de armar la notificación y comunicarse con `InterfazServidorCorreo`.
  * Desde `GestorRecepcionBolsin.notificarRecepcion()`, instanciar/invocar a ese segundo gestor pasándole los datos necesarios de la recepción confirmada, reflejando fielmente la relación `<<includes>>` entre casos de uso.

---

## 5. Persistencia Real (No en Memoria) y Entidades Desacopladas de la BD

### 5.1 Pasar de base en memoria a persistencia real en disco
* **Problema actual:**
  * En [`persistence.xml`](src/main/resources/META-INF/persistence.xml#L27), H2 está configurado en memoria (`jdbc:h2:mem:ppaidb`), por lo que todos los cambios se pierden al cerrar la aplicación.
  * Además, [`RepositorioJpaCU28.inicializarDatosEscenario(...)`](src/main/java/com/ppai/repository/cu28/RepositorioJpaCU28.java#L27-L45) ejecuta `DELETE` sobre todas las tablas operacionales cada vez que se inicia el gestor.
* **Qué hacer:**
  * Configurar una base de datos persistente real (ej. `jdbc:h2:file:./data/ppaidb` o un motor externo como PostgreSQL/MySQL en `persistence.xml`).
  * Cambiar la estrategia de carga inicial para que **solo siembre datos semilla si la base está vacía** (o mediante un script/migración inicial), evitando borrar transacciones ya confirmadas al reiniciar el programa.

### 5.2 Quitar las anotaciones/relaciones de BD de las entidades del dominio
* **Problema actual:** Las clases del paquete `com.ppai.domain.model.cu28` (`Bolsin`, `Remito`, `DetalleRemito`, `Documentacion`, `CambioEstadoBolsin`, `CambioEstadoDocumentacion`, `Estado`, etc.) tienen anotaciones JPA (`@Entity`, `@Table`, `@Id`, `@ManyToOne`, `@OneToMany`, `@JoinColumn`, `@OrderColumn`, `@Transient`) y constructores `protected` pensados para Hibernate. Esto acopla el modelo de objetos puro a la tecnología de base de datos.
* **Qué hacer:**
  * Dejar las clases de `domain/model` como **POJOs puros orientados a objetos**, sin ningún `import jakarta.persistence.*` ni relaciones configuradas para la BD.
  * Mover el mapeo relacional hacia la capa de persistencia (`repository`), ya sea:
    * Usando clases de entidad JPA separadas (ej. `BolsinEntity`, `RemitoEntity`, etc. dentro de `repository/jpa/entity/`) + **Mappers** que conviertan entre Entidad de Dominio $\leftrightarrow$ Entidad de BD; o
    * Usando archivos externos de mapeo XML (`META-INF/orm.xml`) o JDBC/DAO puro en la capa de repositorio.

---

## Resumen de Prioridades

| # | Área | Cambio Principal | Impacto |
| :-: | :--- | :--- | :--- |
| 1 | **Iterator** | Agregar `if (comprobarFiltro(...))` en retorno y unificar a `actual()` | Corrección directa de secuencia y patrón GoF |
| 2 | **Boundary / Control** | Sacar lógica de negocio y entidades de `PantallaRecepcionBolsin` | Cumplimiento estricto de arquitectura BCE |
| 3 | **UX** | Quitar textos técnicos (`CU`, `GoF`, `A3..A6`, `Mock`) de la UI | Interfaz lista para usuario final |
| 4 | **CU 29 (`<<includes>>`)** | Crear gestor propio para notificación e invocarlo desde el gestor actual | Separación de responsabilidades entre CUs |
| 5 | **Persistencia y Dominio** | BD persistente real (sin borrar al iniciar) + entidades sin anotaciones JPA | Dominio OO puro desacoplado de la BD |
