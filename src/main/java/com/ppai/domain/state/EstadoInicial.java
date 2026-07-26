package com.ppai.domain.state;

public class EstadoInicial implements EstadoEjemplo {

    @Override
    public String getNombre() {
        return "INICIAL";
    }

    @Override
    public void procesar(Object contexto) {
        System.out.println("Procesando negocio en estado INICIAL...");
    }

    @Override
    public boolean esFinalizado() {
        return false;
    }
}
