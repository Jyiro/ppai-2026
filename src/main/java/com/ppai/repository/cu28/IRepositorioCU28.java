package com.ppai.repository.cu28;

import com.ppai.domain.model.cu28.Bolsin;
import com.ppai.domain.model.cu28.ComisionMedica;
import com.ppai.domain.model.cu28.Empleado;
import com.ppai.domain.model.cu28.Estado;
import com.ppai.domain.model.cu28.Sesion;

import java.util.List;

/**
 * Contrato de persistencia para el Caso de Uso 28 (Inversión de Dependencias).
 */
public interface IRepositorioCU28 {

    boolean estaVacia();

    Sesion obtenerSesionActiva();

    List<Estado> obtenerEstados();

    List<ComisionMedica> obtenerComisionesMedicas();

    List<Bolsin> obtenerTodosBolsines();

    Bolsin actualizarBolsin(Bolsin bolsin, Empleado empleadoResponsable);
}
