# 🎭 `domain/state/cu28` — Paquete Reservado para Patrón State (CU 28)

Este paquete está **reservado y vacío intencionalmente**.

---

## 🎯 Propósito

Cuando se aplique el **patrón State** al CU 28, las subclases concretas
de estado vivirán aquí.

---

## 🔮 ¿Qué irá aquí en el futuro?

### Para el Bolsín

```
EstadoBolsin          (interfaz o clase abstracta)
├── EstadoEnviado
├── EstadoRecibidoEnCMDestino
└── EstadoObservado   (si se agrega)
```

### Para la Documentación

```
EstadoDocumentacion   (interfaz o clase abstracta)
├── EstadoEnTransito
└── EstadoRecibidoYAceptado
```

---

## 🔑 Cómo se integra con el código actual

Actualmente, la clase [`Estado`](../model/cu28/Estado.java) contiene
todos los métodos de consulta (`esEnviado()`, `esAmbitoBolsin()`, etc.)
como condicionales sobre el nombre/ámbito.

Al aplicar el patrón State:

1. Se extrae una interfaz `IEstadoBolsin` con esos métodos.
2. Cada subclase implementa el método correspondiente sin condicionales.
3. `Bolsin.marcarRecibido()` delega en el estado actual, que sabe cómo transicionar.
4. El `GestorRecepcionBolsin.obtenerEstado()` también se simplifica.

---

## 📐 Estructura esperada tras aplicar State

```java
// Interfaz futura
public interface IEstadoBolsin {
    boolean esEnviado();
    boolean esRecibidoEnCMDestino();
    IEstadoBolsin marcarRecibido(Bolsin bolsin);
}

// Implementación concreta
public class EstadoEnviado implements IEstadoBolsin {
    public boolean esEnviado() { return true; }
    public boolean esRecibidoEnCMDestino() { return false; }
    public IEstadoBolsin marcarRecibido(Bolsin bolsin) {
        // Lógica de transición
        return new EstadoRecibidoEnCMDestino();
    }
}
```
