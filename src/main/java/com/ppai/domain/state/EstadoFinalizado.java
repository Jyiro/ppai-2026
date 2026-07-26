package com.ppai.domain.state;

public class EstadoFinalizado implements EstadoEjemplo {

    @Override
    public String getNombre() {
        return "FINALIZADO";
    }

    @Override
    public void procesar(Object contexto) {
        System.out.println("El proceso ya ha FINALIZADO. No se permiten más modificaciones.");
    }

    @Override
    public boolean esFinalizado() {
        return true;
    }
}
