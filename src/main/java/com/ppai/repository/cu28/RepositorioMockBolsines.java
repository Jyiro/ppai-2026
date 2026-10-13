package com.ppai.repository.cu28;

import com.ppai.domain.model.cu28.Bolsin;
import com.ppai.domain.model.cu28.ComisionMedica;
import com.ppai.domain.model.cu28.DetalleRemito;
import com.ppai.domain.model.cu28.Documentacion;
import com.ppai.domain.model.cu28.Estado;
import com.ppai.domain.model.cu28.Remito;
import com.ppai.domain.model.cu28.Sesion;
import com.ppai.domain.model.cu28.TipoDocumento;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fuente de datos mock para ejercitar CU 28 sin mezclar fixtures con el flujo del Gestor.
 * Mantiene objetos de dominio reales para que las pruebas recorran las mismas entidades del caso de uso.
 */
public class RepositorioMockBolsines {

    public List<Bolsin> crearBolsinesParaEscenario(Sesion sesionActual,
                                                   List<Estado> estadosDisponibles,
                                                   int escenario) {
        List<Bolsin> bolsines = new ArrayList<>();

        Estado estadoEnviado = buscarEstadoBolsinEnviado(estadosDisponibles);
        Estado estadoDocEnviado = buscarEstadoDocumentacionEnBolsinEnviado(estadosDisponibles);
        ComisionMedica cmDestinoActual = sesionActual.getUsuario().getCMEmpleado();

        ComisionMedica cmOrigenCordoba = new ComisionMedica(10L, "CMJ N° 1 - Córdoba Capital");
        ComisionMedica cmOrigenMendoza = new ComisionMedica(11L, "CMJ N° 4 - Mendoza");
        ComisionMedica cmOrigenLaPlata = new ComisionMedica(12L, "CMJ N° 11 - La Plata");
        ComisionMedica cmOrigenRosario = new ComisionMedica(13L, "CMJ N° 7 - Rosario");

        TipoDocumento tipoExpediente = new TipoDocumento(1L, "Expediente de Accidente Laboral");
        TipoDocumento tipoDictamen = new TipoDocumento(2L, "Dictamen Médico Legal");
        TipoDocumento tipoEstudio = new TipoDocumento(3L, "Estudio Clínico / Resonancia");
        TipoDocumento tipoCartaDoc = new TipoDocumento(4L, "Carta Documento Notificada");

        bolsines.add(crearBolsinFlujoBasico(cmDestinoActual, cmOrigenCordoba, estadoEnviado, estadoDocEnviado,
                tipoExpediente, tipoDictamen));
        bolsines.add(crearBolsinAlternativosA3A4(cmDestinoActual, cmOrigenMendoza, estadoEnviado, estadoDocEnviado,
                tipoExpediente, tipoEstudio, tipoCartaDoc, tipoDictamen));
        bolsines.add(crearBolsinRedireccionCMC(cmDestinoActual, cmOrigenLaPlata, estadoEnviado, estadoDocEnviado,
                tipoExpediente));
        bolsines.add(crearBolsinOtroDestino(cmOrigenRosario, estadoEnviado, estadoDocEnviado, tipoExpediente));

        return bolsines;
    }

    private Estado buscarEstadoBolsinEnviado(List<Estado> estadosDisponibles) {
        return estadosDisponibles.stream()
                .filter(e -> e.esAmbitoBolsin() && e.esEnviado())
                .findFirst()
                .orElse(new Estado(Estado.NOMBRE_ENVIADO, Estado.AMBITO_BOLSIN));
    }

    private Estado buscarEstadoDocumentacionEnBolsinEnviado(List<Estado> estadosDisponibles) {
        return estadosDisponibles.stream()
                .filter(e -> e.esAmbitoDocumentacion() && Estado.NOMBRE_EN_BOLSIN_ENVIADO.equals(e.getNombre()))
                .findFirst()
                .orElse(new Estado(Estado.NOMBRE_EN_BOLSIN_ENVIADO, Estado.AMBITO_DOCUMENTACION));
    }

    private Bolsin crearBolsinFlujoBasico(ComisionMedica cmDestinoActual,
                                          ComisionMedica cmOrigen,
                                          Estado estadoEnviado,
                                          Estado estadoDocEnviado,
                                          TipoDocumento tipoExpediente,
                                          TipoDocumento tipoDictamen) {
        Documentacion d1 = new Documentacion(101L, "EXP-2026-904: Trámite incapacidad laboral Gómez",
                tipoExpediente, estadoDocEnviado, LocalDateTime.now().minusDays(3));
        Documentacion d2 = new Documentacion(102L, "DIC-2026-312: Dictamen traumatológico pericial",
                tipoDictamen, estadoDocEnviado, LocalDateTime.now().minusDays(3));

        Remito remito = new Remito("REM-2026-00458");
        remito.agregarDetalle(new DetalleRemito(d1));
        remito.agregarDetalle(new DetalleRemito(d2));

        return new Bolsin("PREC-7041", cmOrigen, cmDestinoActual, remito, estadoEnviado, LocalDateTime.now().minusDays(3));
    }

    private Bolsin crearBolsinAlternativosA3A4(ComisionMedica cmDestinoActual,
                                               ComisionMedica cmOrigen,
                                               Estado estadoEnviado,
                                               Estado estadoDocEnviado,
                                               TipoDocumento tipoExpediente,
                                               TipoDocumento tipoEstudio,
                                               TipoDocumento tipoCartaDoc,
                                               TipoDocumento tipoDictamen) {
        Documentacion d1 = new Documentacion(201L, "EXP-2026-1188: Evaluación secuelas respiratorias",
                tipoExpediente, estadoDocEnviado, LocalDateTime.now().minusDays(2));
        Documentacion d2 = new Documentacion(202L, "EST-2026-049: Tomografía computada de tórax",
                tipoEstudio, estadoDocEnviado, LocalDateTime.now().minusDays(2));
        Documentacion d3 = new Documentacion(203L, "CD-2026-881: Intimación empleador ART preventiva",
                tipoCartaDoc, estadoDocEnviado, LocalDateTime.now().minusDays(2));
        Documentacion d4 = new Documentacion(204L, "DIC-2026-505: Junta médica interdisciplinaria",
                tipoDictamen, estadoDocEnviado, LocalDateTime.now().minusDays(2));

        Remito remito = new Remito("REM-2026-00892");
        remito.agregarDetalle(new DetalleRemito(d1));
        remito.agregarDetalle(new DetalleRemito(d2));
        remito.agregarDetalle(new DetalleRemito(d3));
        remito.agregarDetalle(new DetalleRemito(d4));

        return new Bolsin("PREC-8820", cmOrigen, cmDestinoActual, remito, estadoEnviado, LocalDateTime.now().minusDays(2));
    }

    private Bolsin crearBolsinRedireccionCMC(ComisionMedica cmDestinoActual,
                                             ComisionMedica cmOrigen,
                                             Estado estadoEnviado,
                                             Estado estadoDocEnviado,
                                             TipoDocumento tipoExpediente) {
        Documentacion documentacion = new Documentacion(301L, "EXP-2026-3401: Apelación Dictamen CMC Sala B",
                tipoExpediente, estadoDocEnviado, LocalDateTime.now().minusHours(18));

        Remito remito = new Remito("REM-2026-01204");
        remito.agregarDetalle(new DetalleRemito(documentacion));

        return new Bolsin("PREC-9150", cmOrigen, cmDestinoActual, remito, estadoEnviado, LocalDateTime.now().minusHours(18));
    }

    private Bolsin crearBolsinOtroDestino(ComisionMedica cmOrigen,
                                          Estado estadoEnviado,
                                          Estado estadoDocEnviado,
                                          TipoDocumento tipoExpediente) {
        ComisionMedica cmOtra = new ComisionMedica(99L, "CMJ N° 99 - Salta");
        Documentacion documentacion = new Documentacion(900L, "Trámite de otra provincia",
                tipoExpediente, estadoDocEnviado, LocalDateTime.now());

        Remito remito = new Remito("REM-2026-99999");
        remito.agregarDetalle(new DetalleRemito(documentacion));

        return new Bolsin("PREC-9999", cmOrigen, cmOtra, remito, estadoEnviado, LocalDateTime.now().minusDays(1));
    }
}
