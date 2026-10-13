# Escenarios mock CU 28

Este documento describe los datos precargados que usa el sistema para probar el CU 28 sin depender todavía de una base de datos real.

## Ubicación en código

- `GestorRecepcionBolsin.inicializarBolsinesDeEjemplo(int escenario)` conserva el punto de entrada usado por la pantalla y las pruebas.
- `RepositorioMockBolsines.crearBolsinesParaEscenario(...)` construye los datos mock y los devuelve como objetos de dominio.

La separación permite mantener el flujo del gestor alineado al diagrama de secuencia, dejando los datos de prueba en una clase de infraestructura reemplazable por JPA/DAO más adelante.

## Escenario 1: Comisión Médica Central

Usuario mock: `jperez.cmc`

CM destino: `Comisión Médica Central (CMC) - CABA`

Este escenario habilita la opción 4 del CU 28: "La documentación se debe redirigir a otra área".

## Escenario 2: CM Jurisdiccional Rosario

Usuario mock: `mlopez.rosario`

CM destino: `CMJ N° 7 - Rosario`

Este escenario sirve para validar que la opción 4 se muestre como solo aplicable a CMC desde la pantalla.

## Bolsines precargados

| Precinto | Remito | CM origen | Destino | Documentación | Escenario de prueba |
|---|---|---|---|---:|---|
| `PREC-7041` | `REM-2026-00458` | Córdoba Capital | CM del usuario activo | 2 docs | Flujo básico, opción 1 |
| `PREC-8820` | `REM-2026-00892` | Mendoza | CM del usuario activo | 4 docs | Alternativos A3 y A4 |
| `PREC-9150` | `REM-2026-01204` | La Plata | CM del usuario activo | 1 doc | Alternativo A5 si destino es CMC |
| `PREC-9999` | `REM-2026-99999` | Rosario | Salta | 1 doc | Control negativo: no debe aparecer en la lista |

Nota de alcance: los mocks actuales usan un remito por bolsín. La ERS y la descripción del caso admiten uno o más remitos por bolsín, por lo que falta un escenario de prueba con varios remitos asociados al mismo bolsín si el diagrama de clases de diseño mantiene esa multiplicidad.

## Cobertura de flujos

- Flujo básico: cubierto con `PREC-7041`, opción 1, confirmación positiva.
- A1: cubierto filtrando por un número de precinto inexistente.
- A2: cubierto filtrando por una CM origen inexistente.
- A3: cubierto con `PREC-8820`, marcando una documentación como faltante.
- A4: cubierto con `PREC-8820`, marcando una documentación como no correspondiente al destino.
- A5: cubierto con usuario CMC y opción 4.
- A6: cubierto con una confirmación negativa; el bolsín queda `ENVIADO` y no se genera notificación CU 29.

## Pendientes de validación contra Lucid Chart

- Confirmar si `Bolsin` debe tener un solo `Remito` o una colección de remitos.
- Confirmar si las relaciones del diagrama de clases son dependencias simples o asociaciones con navegabilidad.
- Confirmar si `RepositorioMockBolsines` debe mostrarse en el diagrama de diseño como infraestructura auxiliar o quedar fuera por ser solo datos de prueba.
