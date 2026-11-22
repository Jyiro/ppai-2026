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
import com.ppai.domain.model.cu28.Usuario;
import com.ppai.repository.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación JPA / Hibernate 6 + H2 en memoria para la persistencia del CU
 * 28.
 * Gestiona el esquema relacional, datos maestros/iniciales y actualizaciones
 * transaccionales.
 */
public class RepositorioJpaCU28 implements IRepositorioCU28 {

    @Override
    public synchronized void inicializarDatosEscenario(Sesion sesionActual, int escenario) {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            // 1. Limpiar datos operacionales previos en orden hijo -> padre
            em.createQuery("DELETE FROM CambioEstadoBolsin").executeUpdate();
            em.createQuery("DELETE FROM CambioEstadoDocumentacion").executeUpdate();
            em.createQuery("DELETE FROM DetalleRemito").executeUpdate();
            em.createQuery("DELETE FROM Remito").executeUpdate();
            em.createQuery("DELETE FROM Bolsin").executeUpdate();
            em.createQuery("DELETE FROM Documentacion").executeUpdate();
            em.createQuery("DELETE FROM Sesion").executeUpdate();
            em.createQuery("DELETE FROM Usuario").executeUpdate();
            em.createQuery("DELETE FROM Empleado").executeUpdate();
            em.flush();
            em.clear();

            // 2. Asegurar catálogo maestro de Estados (9 estados del CU 28)
            Estado estBolsinEnviado = asegurarEstado(em, Estado.NOMBRE_ENVIADO, Estado.AMBITO_BOLSIN);
            Estado estBolsinRecibido = asegurarEstado(em, Estado.NOMBRE_RECIBIDO_CM_DESTINO, Estado.AMBITO_BOLSIN);

            asegurarEstado(em, Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, Estado.AMBITO_REMITO);
            asegurarEstado(em, Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, Estado.AMBITO_REMITO);

            Estado estDocEnBolsinEnviado = asegurarEstado(em, Estado.NOMBRE_EN_BOLSIN_ENVIADO,
                    Estado.AMBITO_DOCUMENTACION);
            asegurarEstado(em, Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, Estado.AMBITO_DOCUMENTACION);
            asegurarEstado(em, Estado.NOMBRE_NO_RECIBIDA, Estado.AMBITO_DOCUMENTACION);
            asegurarEstado(em, Estado.NOMBRE_RECIBIDA_Y_RECHAZADA, Estado.AMBITO_DOCUMENTACION);
            asegurarEstado(em, Estado.NOMBRE_PARA_REDIRIGIR, Estado.AMBITO_DOCUMENTACION);

            // 3. Asegurar catálogo de Comisiones Médicas
            ComisionMedica cmCentral = asegurarComisionMedica(em, 1L, "Comisión Médica Central (CMC) - CABA");
            ComisionMedica cmRosario = asegurarComisionMedica(em, 2L, "CMJ N° 7 - Rosario");
            ComisionMedica cmOrigenCordoba = asegurarComisionMedica(em, 10L, "CMJ N° 1 - Córdoba Capital");
            ComisionMedica cmOrigenMendoza = asegurarComisionMedica(em, 11L, "CMJ N° 4 - Mendoza");
            ComisionMedica cmOrigenLaPlata = asegurarComisionMedica(em, 12L, "CMJ N° 11 - La Plata");
            ComisionMedica cmOrigenSantaFe = asegurarComisionMedica(em, 14L, "CMJ N° 5 - Santa Fe Capital");
            ComisionMedica cmSalta = asegurarComisionMedica(em, 99L, "CMJ N° 99 - Salta");

            // 4. Asegurar catálogo de Tipos de Documento
            TipoDocumento tipoExpediente = asegurarTipoDocumento(em, 1L, "Expediente de Accidente Laboral");
            TipoDocumento tipoDictamen = asegurarTipoDocumento(em, 2L, "Dictamen Médico Legal");
            TipoDocumento tipoEstudio = asegurarTipoDocumento(em, 3L, "Estudio Clínico / Resonancia");
            TipoDocumento tipoCartaDoc = asegurarTipoDocumento(em, 4L, "Carta Documento Notificada");

            // 5. Asegurar Empleados, Usuario y Sesión activa en BD
            Empleado empCentral = new Empleado(100L, "Juan Pérez (Encargado Central)", cmCentral);
            Empleado empRosario = new Empleado(200L, "Mariana López (Encargada Rosario)", cmRosario);
            em.persist(empCentral);
            em.persist(empRosario);

            ComisionMedica cmUsuarioManaged = cmCentral;
            if (sesionActual != null && sesionActual.getUsuario() != null) {
                Usuario usr = sesionActual.getUsuario();
                Empleado empSesion = usr.getEmpleado();
                Empleado empManaged = empCentral;
                if (empSesion != null) {
                    ComisionMedica cmSesion = empSesion.getCM();
                    if (cmSesion != null) {
                        cmUsuarioManaged = asegurarComisionMedica(em, cmSesion.getId(), cmSesion.getNombre());
                    }
                    Empleado existente = em.find(Empleado.class, empSesion.getId());
                    if (existente != null) {
                        empManaged = existente;
                    } else {
                        empManaged = new Empleado(empSesion.getId(), empSesion.getNombre(), cmUsuarioManaged);
                        em.persist(empManaged);
                    }
                }
                Usuario usrManaged = new Usuario(usr.getUsername(), empManaged);
                em.persist(usrManaged);
                em.persist(new Sesion(usrManaged));
            }

            // 6. Poblar los 7 bolsines del escenario en BD (mismos datos de
            // RepositorioMockBolsines)
            ComisionMedica cmUsuarioOriginal = (sesionActual != null && sesionActual.getUsuario() != null)
                    ? sesionActual.getUsuario().getCMEmpleado()
                    : null;
            boolean esCMPredefinida = cmUsuarioOriginal != null
                    && (Long.valueOf(1L).equals(cmUsuarioOriginal.getId())
                            || Long.valueOf(2L).equals(cmUsuarioOriginal.getId()));
            ComisionMedica cmDestinoGrupo1 = esCMPredefinida ? cmCentral : cmUsuarioManaged;

            em.persist(crearBolsinFlujoBasico(cmDestinoGrupo1, cmOrigenCordoba, estBolsinEnviado,
                    estDocEnBolsinEnviado, tipoExpediente, tipoDictamen));
            em.persist(crearBolsinAlternativosA3A4(cmDestinoGrupo1, cmOrigenMendoza, estBolsinEnviado,
                    estDocEnBolsinEnviado, tipoExpediente, tipoEstudio, tipoCartaDoc, tipoDictamen));
            em.persist(crearBolsinRedireccionCMC(cmDestinoGrupo1, cmOrigenLaPlata, estBolsinEnviado,
                    estDocEnBolsinEnviado, tipoExpediente));

            em.persist(crearBolsinRosario1(cmRosario, cmOrigenSantaFe, estBolsinEnviado,
                    estDocEnBolsinEnviado, tipoExpediente, tipoEstudio));
            em.persist(crearBolsinRosario2(cmRosario, cmCentral, estBolsinEnviado,
                    estDocEnBolsinEnviado, tipoDictamen, tipoExpediente, tipoCartaDoc));

            em.persist(crearBolsinOtroDestino(cmRosario, cmSalta, estBolsinEnviado,
                    estDocEnBolsinEnviado, tipoExpediente));
            em.persist(crearBolsinYaRecibido(cmCentral, cmOrigenCordoba, estBolsinRecibido,
                    estDocEnBolsinEnviado, tipoDictamen));

            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public List<Estado> obtenerEstados() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT e FROM Estado e ORDER BY e.id", Estado.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public List<ComisionMedica> obtenerComisionesMedicas() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT c FROM ComisionMedica c ORDER BY c.id", ComisionMedica.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public List<Bolsin> obtenerTodosBolsines() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT b FROM Bolsin b ORDER BY b.id", Bolsin.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public synchronized Bolsin actualizarBolsin(Bolsin bolsin, Empleado empleadoResponsable) {
        if (bolsin == null) {
            return null;
        }
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (empleadoResponsable != null) {
                if (empleadoResponsable.getCM() != null) {
                    asegurarComisionMedica(em, empleadoResponsable.getCM().getId(),
                            empleadoResponsable.getCM().getNombre());
                }
                if (em.find(Empleado.class, empleadoResponsable.getId()) == null) {
                    em.merge(empleadoResponsable);
                }
            }
            Bolsin actualizado = em.merge(bolsin);
            tx.commit();
            return actualizado;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    private Estado asegurarEstado(EntityManager em, String nombre, String ambito) {
        List<Estado> encontrados = em.createQuery(
                "SELECT e FROM Estado e WHERE e.nombre = :nombre AND e.ambito = :ambito",
                Estado.class)
                .setParameter("nombre", nombre)
                .setParameter("ambito", ambito)
                .getResultList();
        if (!encontrados.isEmpty()) {
            return encontrados.get(0);
        }
        Estado nuevo = new Estado(nombre, ambito);
        em.persist(nuevo);
        return nuevo;
    }

    private ComisionMedica asegurarComisionMedica(EntityManager em, Long id, String nombre) {
        ComisionMedica existente = em.find(ComisionMedica.class, id);
        if (existente != null) {
            return existente;
        }
        ComisionMedica nueva = new ComisionMedica(id, nombre);
        em.persist(nueva);
        return nueva;
    }

    private TipoDocumento asegurarTipoDocumento(EntityManager em, Long id, String nombre) {
        TipoDocumento existente = em.find(TipoDocumento.class, id);
        if (existente != null) {
            return existente;
        }
        TipoDocumento nuevo = new TipoDocumento(id, nombre);
        em.persist(nuevo);
        return nuevo;
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

        return new Bolsin("PREC-7041", cmOrigen, cmDestinoActual, remito, estadoEnviado,
                LocalDateTime.now().minusDays(3));
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

        return new Bolsin("PREC-8820", cmOrigen, cmDestinoActual, remito, estadoEnviado,
                LocalDateTime.now().minusDays(2));
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

        return new Bolsin("PREC-9150", cmOrigen, cmDestinoActual, remito, estadoEnviado,
                LocalDateTime.now().minusHours(18));
    }

    private Bolsin crearBolsinRosario1(ComisionMedica cmDestinoRosario,
            ComisionMedica cmOrigen,
            Estado estadoEnviado,
            Estado estadoDocEnviado,
            TipoDocumento tipoExpediente,
            TipoDocumento tipoEstudio) {
        Documentacion d1 = new Documentacion(401L, "EXP-2026-4102: Derivación interjurisdiccional Santa Fe",
                tipoExpediente, estadoDocEnviado, LocalDateTime.now().minusDays(2));
        Documentacion d2 = new Documentacion(402L, "EST-2026-198: Electromiograma miembro superior",
                tipoEstudio, estadoDocEnviado, LocalDateTime.now().minusDays(2));

        Remito remito = new Remito("REM-2026-02110");
        remito.agregarDetalle(new DetalleRemito(d1));
        remito.agregarDetalle(new DetalleRemito(d2));

        return new Bolsin("PREC-6015", cmOrigen, cmDestinoRosario, remito, estadoEnviado,
                LocalDateTime.now().minusDays(2));
    }

    private Bolsin crearBolsinRosario2(ComisionMedica cmDestinoRosario,
            ComisionMedica cmOrigen,
            Estado estadoEnviado,
            Estado estadoDocEnviado,
            TipoDocumento tipoDictamen,
            TipoDocumento tipoExpediente,
            TipoDocumento tipoCartaDoc) {
        Documentacion d1 = new Documentacion(501L, "DIC-2026-710: Devolución expediente apelado CMC",
                tipoDictamen, estadoDocEnviado, LocalDateTime.now().minusDays(1));
        Documentacion d2 = new Documentacion(502L, "EXP-2026-2905: Reapertura trámite incapacidad",
                tipoExpediente, estadoDocEnviado, LocalDateTime.now().minusDays(1));
        Documentacion d3 = new Documentacion(503L, "CD-2026-912: Notificación fehaciente al trabajador",
                tipoCartaDoc, estadoDocEnviado, LocalDateTime.now().minusDays(1));

        Remito remito = new Remito("REM-2026-02145");
        remito.agregarDetalle(new DetalleRemito(d1));
        remito.agregarDetalle(new DetalleRemito(d2));
        remito.agregarDetalle(new DetalleRemito(d3));

        return new Bolsin("PREC-6088", cmOrigen, cmDestinoRosario, remito, estadoEnviado,
                LocalDateTime.now().minusDays(1));
    }

    private Bolsin crearBolsinOtroDestino(ComisionMedica cmOrigen,
            ComisionMedica cmOtra,
            Estado estadoEnviado,
            Estado estadoDocEnviado,
            TipoDocumento tipoExpediente) {
        Documentacion documentacion = new Documentacion(900L, "Trámite de otra provincia",
                tipoExpediente, estadoDocEnviado, LocalDateTime.now());

        Remito remito = new Remito("REM-2026-99999");
        remito.agregarDetalle(new DetalleRemito(documentacion));

        return new Bolsin("PREC-9999", cmOrigen, cmOtra, remito, estadoEnviado, LocalDateTime.now().minusDays(1));
    }

    private Bolsin crearBolsinYaRecibido(ComisionMedica cmDestino,
            ComisionMedica cmOrigen,
            Estado estadoRecibido,
            Estado estadoDocEnviado,
            TipoDocumento tipoDictamen) {
        Documentacion documentacion = new Documentacion(950L, "DIC-2026-099: Bolsín ya recepcionado anteriormente",
                tipoDictamen, estadoDocEnviado, LocalDateTime.now().minusDays(5));

        Remito remito = new Remito("REM-2026-00112");
        remito.agregarDetalle(new DetalleRemito(documentacion));

        return new Bolsin("PREC-5500", cmOrigen, cmDestino, remito, estadoRecibido, LocalDateTime.now().minusDays(5));
    }
}
