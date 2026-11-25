package com.ppai.repository.cu28;

import com.ppai.domain.model.cu28.Bolsin;
import com.ppai.domain.model.cu28.ComisionMedica;
import com.ppai.domain.model.cu28.DetalleRemito;
import com.ppai.domain.model.cu28.Documentacion;
import com.ppai.domain.model.cu28.Empleado;
import com.ppai.domain.model.cu28.Estado;
import com.ppai.domain.model.cu28.Remito;
import com.ppai.domain.model.cu28.Sesion;
import com.ppai.domain.model.cu28.TipoDocumento;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fuente de datos semilla temporal para Fase 1 (reemplazada en Fase 2 por ReiniciarEscenarioBD).
 */
public class RepositorioMockBolsines {

    public List<Bolsin> crearBolsinesParaEscenario(Sesion sesionActual,
                                                   List<Estado> estadosDisponibles,
                                                   int escenario) {
        List<Bolsin> bolsines = new ArrayList<>();

        Estado estadoEnviado = buscarEstadoBolsinEnviado(estadosDisponibles);
        Estado estadoRecibido = buscarEstadoBolsinRecibido(estadosDisponibles);
        Estado estadoDocEnviado = buscarEstadoDocumentacionEnBolsinEnviado(estadosDisponibles);
        ComisionMedica cmUsuario = sesionActual != null ? sesionActual.getCMEmpleado() : null;
        Empleado empOrigen = null;

        ComisionMedica cmCentral = new ComisionMedica(1, "Comisión Médica Central (CMC) - CABA",
                "Av. Roque Sáenz Peña 1100", "despacho.cmc@srt.gob.ar", "011-4321-1000");
        ComisionMedica cmRosario = new ComisionMedica(2, "CMJ N° 7 - Rosario",
                "Bv. Oroño 850", "despacho.rosario@srt.gob.ar", "0341-440-2000");
        ComisionMedica cmOrigenCordoba = new ComisionMedica(10, "CMJ N° 1 - Córdoba Capital",
                "Av. Colón 450", "despacho.cordoba@srt.gob.ar", "0351-420-1100");
        ComisionMedica cmOrigenMendoza = new ComisionMedica(11, "CMJ N° 4 - Mendoza",
                "San Martín 720", "despacho.mendoza@srt.gob.ar", "0261-425-3300");
        ComisionMedica cmOrigenLaPlata = new ComisionMedica(12, "CMJ N° 11 - La Plata",
                "Calle 7 N° 940", "despacho.laplata@srt.gob.ar", "0221-482-4400");
        ComisionMedica cmOrigenSantaFe = new ComisionMedica(14, "CMJ N° 5 - Santa Fe Capital",
                "25 de Mayo 1800", "despacho.santafe@srt.gob.ar", "0342-455-6600");
        ComisionMedica cmSalta = new ComisionMedica(99, "CMJ N° 99 - Salta",
                "Belgrano 610", "despacho.salta@srt.gob.ar", "0387-431-9900");

        boolean esCMPredefinida = cmUsuario != null
                && (cmUsuario.getCodigo() == 1 || cmUsuario.getCodigo() == 2);
        ComisionMedica cmDestinoGrupo1 = esCMPredefinida ? cmCentral : cmUsuario;

        TipoDocumento tipoExpediente = new TipoDocumento("Expediente de Accidente Laboral", "Expediente administrativo ART");
        TipoDocumento tipoDictamen = new TipoDocumento("Dictamen Médico Legal", "Resolución médica pericial");
        TipoDocumento tipoEstudio = new TipoDocumento("Estudio Clínico / Resonancia", "Estudio complementario de diagnóstico");
        TipoDocumento tipoCartaDoc = new TipoDocumento("Carta Documento Notificada", "Notificación fehaciente");

        bolsines.add(crearBolsinFlujoBasico(cmDestinoGrupo1, cmOrigenCordoba, estadoEnviado, estadoDocEnviado,
                tipoExpediente, tipoDictamen, empOrigen));
        bolsines.add(crearBolsinAlternativosA3A4(cmDestinoGrupo1, cmOrigenMendoza, estadoEnviado, estadoDocEnviado,
                tipoExpediente, tipoEstudio, tipoCartaDoc, tipoDictamen, empOrigen));
        bolsines.add(crearBolsinRedireccionCMC(cmDestinoGrupo1, cmOrigenLaPlata, estadoEnviado, estadoDocEnviado,
                tipoExpediente, empOrigen));

        bolsines.add(crearBolsinRosario1(cmRosario, cmOrigenSantaFe, estadoEnviado, estadoDocEnviado,
                tipoExpediente, tipoEstudio, empOrigen));
        bolsines.add(crearBolsinRosario2(cmRosario, cmCentral, estadoEnviado, estadoDocEnviado,
                tipoDictamen, tipoExpediente, tipoCartaDoc, empOrigen));

        bolsines.add(crearBolsinOtroDestino(cmRosario, cmSalta, estadoEnviado, estadoDocEnviado, tipoExpediente, empOrigen));
        bolsines.add(crearBolsinYaRecibido(cmCentral, cmOrigenCordoba, estadoRecibido, estadoDocEnviado, tipoDictamen, empOrigen));

        return bolsines;
    }

    public List<ComisionMedica> obtenerComisionesMedicasOrigenCatalogo() {
        List<ComisionMedica> catalogo = new ArrayList<>();
        catalogo.add(new ComisionMedica(1, "Comisión Médica Central (CMC) - CABA",
                "Av. Roque Sáenz Peña 1100", "despacho.cmc@srt.gob.ar", "011-4321-1000"));
        catalogo.add(new ComisionMedica(10, "CMJ N° 1 - Córdoba Capital",
                "Av. Colón 450", "despacho.cordoba@srt.gob.ar", "0351-420-1100"));
        catalogo.add(new ComisionMedica(11, "CMJ N° 4 - Mendoza",
                "San Martín 720", "despacho.mendoza@srt.gob.ar", "0261-425-3300"));
        catalogo.add(new ComisionMedica(12, "CMJ N° 11 - La Plata",
                "Calle 7 N° 940", "despacho.laplata@srt.gob.ar", "0221-482-4400"));
        catalogo.add(new ComisionMedica(14, "CMJ N° 5 - Santa Fe Capital",
                "25 de Mayo 1800", "despacho.santafe@srt.gob.ar", "0342-455-6600"));
        catalogo.add(new ComisionMedica(2, "CMJ N° 7 - Rosario",
                "Bv. Oroño 850", "despacho.rosario@srt.gob.ar", "0341-440-2000"));
        catalogo.add(new ComisionMedica(99, "CMJ N° 99 - Salta",
                "Belgrano 610", "despacho.salta@srt.gob.ar", "0387-431-9900"));
        return catalogo;
    }

    private Estado buscarEstadoBolsinEnviado(List<Estado> estadosDisponibles) {
        return estadosDisponibles.stream()
                .filter(e -> e.esAmbitoBolsin() && e.esEnviado())
                .findFirst()
                .orElse(new Estado(Estado.NOMBRE_ENVIADO, "Bolsín enviado", Estado.AMBITO_BOLSIN));
    }

    private Estado buscarEstadoBolsinRecibido(List<Estado> estadosDisponibles) {
        return estadosDisponibles.stream()
                .filter(e -> e.esAmbitoBolsin() && e.esEstadoRecibidoEnCMDestino())
                .findFirst()
                .orElse(new Estado(Estado.NOMBRE_RECIBIDO_CM_DESTINO, "Recibido en CM destino", Estado.AMBITO_BOLSIN));
    }

    private Estado buscarEstadoDocumentacionEnBolsinEnviado(List<Estado> estadosDisponibles) {
        return estadosDisponibles.stream()
                .filter(e -> e.esAmbitoDocumentacion() && Estado.NOMBRE_EN_BOLSIN_ENVIADO.equals(e.getNombre()))
                .findFirst()
                .orElse(new Estado(Estado.NOMBRE_EN_BOLSIN_ENVIADO, "En bolsín enviado", Estado.AMBITO_DOCUMENTACION));
    }

    private Bolsin crearBolsinFlujoBasico(ComisionMedica cmDestinoActual,
                                          ComisionMedica cmOrigen,
                                          Estado estadoEnviado,
                                          Estado estadoDocEnviado,
                                          TipoDocumento tipoExpediente,
                                          TipoDocumento tipoDictamen,
                                          Empleado emp) {
        LocalDateTime fecha = LocalDateTime.now().minusDays(3);
        Documentacion d1 = new Documentacion(101, "EXP-2026-904: Trámite incapacidad laboral Gómez",
                fecha, tipoExpediente, cmOrigen, estadoDocEnviado, fecha, emp);
        Documentacion d2 = new Documentacion(102, "DIC-2026-312: Dictamen traumatológico pericial",
                fecha, tipoDictamen, cmOrigen, estadoDocEnviado, fecha, emp);

        Remito remito = new Remito(458, fecha, cmOrigen, cmDestinoActual);
        remito.agregarDetalle(new DetalleRemito(d1, "Mesa de Entradas"));
        remito.agregarDetalle(new DetalleRemito(d2, "Junta Médica"));

        return new Bolsin(1, 7041, fecha, 2.4, cmOrigen, cmDestinoActual,
                List.of(remito), estadoEnviado, fecha, emp);
    }

    private Bolsin crearBolsinAlternativosA3A4(ComisionMedica cmDestinoActual,
                                               ComisionMedica cmOrigen,
                                               Estado estadoEnviado,
                                               Estado estadoDocEnviado,
                                               TipoDocumento tipoExpediente,
                                               TipoDocumento tipoEstudio,
                                               TipoDocumento tipoCartaDoc,
                                               TipoDocumento tipoDictamen,
                                               Empleado emp) {
        LocalDateTime fecha = LocalDateTime.now().minusDays(2);
        Documentacion d1 = new Documentacion(201, "EXP-2026-1188: Evaluación secuelas respiratorias",
                fecha, tipoExpediente, cmOrigen, estadoDocEnviado, fecha, emp);
        Documentacion d2 = new Documentacion(202, "EST-2026-049: Tomografía computada de tórax",
                fecha, tipoEstudio, cmOrigen, estadoDocEnviado, fecha, emp);
        Documentacion d3 = new Documentacion(203, "CD-2026-881: Intimación empleador ART preventiva",
                fecha, tipoCartaDoc, cmOrigen, estadoDocEnviado, fecha, emp);
        Documentacion d4 = new Documentacion(204, "DIC-2026-505: Junta médica interdisciplinaria",
                fecha, tipoDictamen, cmOrigen, estadoDocEnviado, fecha, emp);

        Remito remito1 = new Remito(892, fecha, cmOrigen, cmDestinoActual);
        remito1.agregarDetalle(new DetalleRemito(d1, "Área Neumología"));
        remito1.agregarDetalle(new DetalleRemito(d2, "Área Diagnóstico"));

        Remito remito2 = new Remito(893, fecha, cmOrigen, cmDestinoActual);
        remito2.agregarDetalle(new DetalleRemito(d3, "Legales"));
        remito2.agregarDetalle(new DetalleRemito(d4, "Junta Médica"));

        return new Bolsin(2, 8820, fecha, 3.1, cmOrigen, cmDestinoActual,
                List.of(remito1, remito2), estadoEnviado, fecha, emp);
    }

    private Bolsin crearBolsinRedireccionCMC(ComisionMedica cmDestinoActual,
                                             ComisionMedica cmOrigen,
                                             Estado estadoEnviado,
                                             Estado estadoDocEnviado,
                                             TipoDocumento tipoExpediente,
                                             Empleado emp) {
        LocalDateTime fecha = LocalDateTime.now().minusHours(18);
        Documentacion documentacion = new Documentacion(301, "EXP-2026-3401: Apelación Dictamen CMC Sala B",
                fecha, tipoExpediente, cmOrigen, estadoDocEnviado, fecha, emp);

        Remito remito = new Remito(1204, fecha, cmOrigen, cmDestinoActual);
        remito.agregarDetalle(new DetalleRemito(documentacion, "Sala B - Apelaciones"));

        return new Bolsin(3, 9150, fecha, 1.8, cmOrigen, cmDestinoActual,
                List.of(remito), estadoEnviado, fecha, emp);
    }

    private Bolsin crearBolsinRosario1(ComisionMedica cmDestinoRosario,
                                       ComisionMedica cmOrigen,
                                       Estado estadoEnviado,
                                       Estado estadoDocEnviado,
                                       TipoDocumento tipoExpediente,
                                       TipoDocumento tipoEstudio,
                                       Empleado emp) {
        LocalDateTime fecha = LocalDateTime.now().minusDays(2);
        Documentacion d1 = new Documentacion(401, "EXP-2026-4102: Derivación interjurisdiccional Santa Fe",
                fecha, tipoExpediente, cmOrigen, estadoDocEnviado, fecha, emp);
        Documentacion d2 = new Documentacion(402, "EST-2026-198: Electromiograma miembro superior",
                fecha, tipoEstudio, cmOrigen, estadoDocEnviado, fecha, emp);

        Remito remito = new Remito(2110, fecha, cmOrigen, cmDestinoRosario);
        remito.agregarDetalle(new DetalleRemito(d1, "Mesa de Entradas"));
        remito.agregarDetalle(new DetalleRemito(d2, "Consultorios"));

        return new Bolsin(4, 6015, fecha, 2.0, cmOrigen, cmDestinoRosario,
                List.of(remito), estadoEnviado, fecha, emp);
    }

    private Bolsin crearBolsinRosario2(ComisionMedica cmDestinoRosario,
                                       ComisionMedica cmOrigen,
                                       Estado estadoEnviado,
                                       Estado estadoDocEnviado,
                                       TipoDocumento tipoDictamen,
                                       TipoDocumento tipoExpediente,
                                       TipoDocumento tipoCartaDoc,
                                       Empleado emp) {
        LocalDateTime fecha = LocalDateTime.now().minusDays(1);
        Documentacion d1 = new Documentacion(501, "DIC-2026-710: Devolución expediente apelado CMC",
                fecha, tipoDictamen, cmOrigen, estadoDocEnviado, fecha, emp);
        Documentacion d2 = new Documentacion(502, "EXP-2026-2905: Reapertura trámite incapacidad",
                fecha, tipoExpediente, cmOrigen, estadoDocEnviado, fecha, emp);
        Documentacion d3 = new Documentacion(503, "CD-2026-912: Notificación fehaciente al trabajador",
                fecha, tipoCartaDoc, cmOrigen, estadoDocEnviado, fecha, emp);

        Remito remito = new Remito(2145, fecha, cmOrigen, cmDestinoRosario);
        remito.agregarDetalle(new DetalleRemito(d1, "Mesa de Entradas"));
        remito.agregarDetalle(new DetalleRemito(d2, "Mesa de Entradas"));
        remito.agregarDetalle(new DetalleRemito(d3, "Notificaciones"));

        return new Bolsin(5, 6088, fecha, 2.7, cmOrigen, cmDestinoRosario,
                List.of(remito), estadoEnviado, fecha, emp);
    }

    private Bolsin crearBolsinOtroDestino(ComisionMedica cmOrigen,
                                          ComisionMedica cmOtra,
                                          Estado estadoEnviado,
                                          Estado estadoDocEnviado,
                                          TipoDocumento tipoExpediente,
                                          Empleado emp) {
        LocalDateTime fecha = LocalDateTime.now().minusDays(1);
        Documentacion documentacion = new Documentacion(900, "Trámite de otra provincia",
                fecha, tipoExpediente, cmOrigen, estadoDocEnviado, fecha, emp);

        Remito remito = new Remito(9999, fecha, cmOrigen, cmOtra);
        remito.agregarDetalle(new DetalleRemito(documentacion, "Mesa de Entradas"));

        return new Bolsin(6, 9999, fecha, 1.2, cmOrigen, cmOtra,
                List.of(remito), estadoEnviado, fecha, emp);
    }

    private Bolsin crearBolsinYaRecibido(ComisionMedica cmDestino,
                                         ComisionMedica cmOrigen,
                                         Estado estadoRecibido,
                                         Estado estadoDocEnviado,
                                         TipoDocumento tipoDictamen,
                                         Empleado emp) {
        LocalDateTime fecha = LocalDateTime.now().minusDays(5);
        Documentacion documentacion = new Documentacion(950, "DIC-2026-099: Bolsín ya recepcionado anteriormente",
                fecha, tipoDictamen, cmOrigen, estadoDocEnviado, fecha, emp);

        Remito remito = new Remito(112, fecha, cmOrigen, cmDestino);
        remito.agregarDetalle(new DetalleRemito(documentacion, "Archivo"));

        return new Bolsin(7, 5500, fecha, 1.5, cmOrigen, cmDestino,
                List.of(remito), estadoRecibido, fecha, emp);
    }
}
