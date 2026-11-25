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
 * Implementación JPA temporal de Fase 1 (será reemplazada en Fase 2 por
 * entidades *JpaEntity + MapeadorPersistenciaCU28 + H2 en disco).
 */
public class RepositorioJpaCU28 implements IRepositorioCU28 {

    @Override
    public synchronized void inicializarDatosEscenario(Sesion sesionActual, int escenario) {
        EntityManager em = JpaUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

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

            ComisionMedica cmCentral = asegurarComisionMedica(em, 1, "Comisión Médica Central (CMC) - CABA",
                    "Av. Roque Sáenz Peña 1100", "despacho.cmc@srt.gob.ar", "011-4321-1000");
            ComisionMedica cmRosario = asegurarComisionMedica(em, 2, "CMJ N° 7 - Rosario",
                    "Bv. Oroño 850", "despacho.rosario@srt.gob.ar", "0341-440-2000");
            ComisionMedica cmOrigenCordoba = asegurarComisionMedica(em, 10, "CMJ N° 1 - Córdoba Capital",
                    "Av. Colón 450", "despacho.cordoba@srt.gob.ar", "0351-420-1100");
            ComisionMedica cmOrigenMendoza = asegurarComisionMedica(em, 11, "CMJ N° 4 - Mendoza",
                    "San Martín 720", "despacho.mendoza@srt.gob.ar", "0261-425-3300");
            ComisionMedica cmOrigenLaPlata = asegurarComisionMedica(em, 12, "CMJ N° 11 - La Plata",
                    "Calle 7 N° 940", "despacho.laplata@srt.gob.ar", "0221-482-4400");
            ComisionMedica cmOrigenSantaFe = asegurarComisionMedica(em, 14, "CMJ N° 5 - Santa Fe Capital",
                    "25 de Mayo 1800", "despacho.santafe@srt.gob.ar", "0342-455-6600");
            ComisionMedica cmSalta = asegurarComisionMedica(em, 99, "CMJ N° 99 - Salta",
                    "Belgrano 610", "despacho.salta@srt.gob.ar", "0387-431-9900");

            TipoDocumento tipoExpediente = asegurarTipoDocumento(em, "Expediente de Accidente Laboral", "Expediente");
            TipoDocumento tipoDictamen = asegurarTipoDocumento(em, "Dictamen Médico Legal", "Dictamen");
            TipoDocumento tipoEstudio = asegurarTipoDocumento(em, "Estudio Clínico / Resonancia", "Estudio");
            TipoDocumento tipoCartaDoc = asegurarTipoDocumento(em, "Carta Documento Notificada", "Carta Documento");

            Empleado empCentral = new Empleado("Juan Pérez (Encargado Central)", cmCentral);
            Empleado empRosario = new Empleado("Mariana López (Encargada Rosario)", cmRosario);
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
                        cmUsuarioManaged = asegurarComisionMedica(em, cmSesion.getCodigo(), cmSesion.getNombre(),
                                cmSesion.getDireccion(), cmSesion.getEmail(), cmSesion.getTelefono());
                    }
                    Empleado existente = em.find(Empleado.class, empSesion.getNombre());
                    if (existente != null) {
                        empManaged = existente;
                    } else {
                        empManaged = new Empleado(empSesion.getNombre(), cmUsuarioManaged);
                        em.persist(empManaged);
                    }
                }
                Usuario usrManaged = new Usuario(usr.getNombre(), usr.getDescripcion(), empManaged);
                em.persist(usrManaged);
                em.persist(new Sesion(usrManaged, LocalDateTime.now()));
            }

            ComisionMedica cmUsuarioOriginal = (sesionActual != null) ? sesionActual.getCMEmpleado() : null;
            boolean esCMPredefinida = cmUsuarioOriginal != null
                    && (cmUsuarioOriginal.getCodigo() == 1 || cmUsuarioOriginal.getCodigo() == 2);
            ComisionMedica cmDestinoGrupo1 = esCMPredefinida ? cmCentral : cmUsuarioManaged;

            LocalDateTime f1 = LocalDateTime.now().minusDays(3);
            Documentacion d101 = new Documentacion(101, "EXP-2026-904: Trámite incapacidad laboral Gómez",
                    f1, tipoExpediente, cmOrigenCordoba, estDocEnBolsinEnviado, f1, null);
            Documentacion d102 = new Documentacion(102, "DIC-2026-312: Dictamen traumatológico pericial",
                    f1, tipoDictamen, cmOrigenCordoba, estDocEnBolsinEnviado, f1, null);
            Remito r1 = new Remito(458, f1, cmOrigenCordoba, cmDestinoGrupo1);
            r1.agregarDetalle(new DetalleRemito(d101, "Mesa de Entradas"));
            r1.agregarDetalle(new DetalleRemito(d102, "Junta Médica"));
            em.persist(new Bolsin(1, 7041, f1, 2.4, cmOrigenCordoba, cmDestinoGrupo1,
                    List.of(r1), estBolsinEnviado, f1, null));

            LocalDateTime f2 = LocalDateTime.now().minusDays(2);
            Documentacion d201 = new Documentacion(201, "EXP-2026-1188: Evaluación secuelas respiratorias",
                    f2, tipoExpediente, cmOrigenMendoza, estDocEnBolsinEnviado, f2, null);
            Documentacion d202 = new Documentacion(202, "EST-2026-049: Tomografía computada de tórax",
                    f2, tipoEstudio, cmOrigenMendoza, estDocEnBolsinEnviado, f2, null);
            Documentacion d203 = new Documentacion(203, "CD-2026-881: Intimación empleador ART preventiva",
                    f2, tipoCartaDoc, cmOrigenMendoza, estDocEnBolsinEnviado, f2, null);
            Documentacion d204 = new Documentacion(204, "DIC-2026-505: Junta médica interdisciplinaria",
                    f2, tipoDictamen, cmOrigenMendoza, estDocEnBolsinEnviado, f2, null);
            Remito r2 = new Remito(892, f2, cmOrigenMendoza, cmDestinoGrupo1);
            r2.agregarDetalle(new DetalleRemito(d201, "Neumología"));
            r2.agregarDetalle(new DetalleRemito(d202, "Diagnóstico"));
            r2.agregarDetalle(new DetalleRemito(d203, "Legales"));
            r2.agregarDetalle(new DetalleRemito(d204, "Junta Médica"));
            em.persist(new Bolsin(2, 8820, f2, 3.1, cmOrigenMendoza, cmDestinoGrupo1,
                    List.of(r2), estBolsinEnviado, f2, null));

            LocalDateTime f3 = LocalDateTime.now().minusHours(18);
            Documentacion d301 = new Documentacion(301, "EXP-2026-3401: Apelación Dictamen CMC Sala B",
                    f3, tipoExpediente, cmOrigenLaPlata, estDocEnBolsinEnviado, f3, null);
            Remito r3 = new Remito(1204, f3, cmOrigenLaPlata, cmDestinoGrupo1);
            r3.agregarDetalle(new DetalleRemito(d301, "Sala B"));
            em.persist(new Bolsin(3, 9150, f3, 1.8, cmOrigenLaPlata, cmDestinoGrupo1,
                    List.of(r3), estBolsinEnviado, f3, null));

            Documentacion d401 = new Documentacion(401, "EXP-2026-4102: Derivación Santa Fe",
                    f2, tipoExpediente, cmOrigenSantaFe, estDocEnBolsinEnviado, f2, null);
            Remito r4 = new Remito(2110, f2, cmOrigenSantaFe, cmRosario);
            r4.agregarDetalle(new DetalleRemito(d401, "Mesa de Entradas"));
            em.persist(new Bolsin(4, 6015, f2, 2.0, cmOrigenSantaFe, cmRosario,
                    List.of(r4), estBolsinEnviado, f2, null));

            Documentacion d900 = new Documentacion(900, "Trámite de otra provincia",
                    f2, tipoExpediente, cmRosario, estDocEnBolsinEnviado, f2, null);
            Remito r6 = new Remito(9999, f2, cmRosario, cmSalta);
            r6.agregarDetalle(new DetalleRemito(d900, "Mesa de Entradas"));
            em.persist(new Bolsin(6, 9999, f2, 1.2, cmRosario, cmSalta,
                    List.of(r6), estBolsinEnviado, f2, null));

            Documentacion d950 = new Documentacion(950, "DIC-2026-099: Bolsín ya recepcionado",
                    f1.minusDays(2), tipoDictamen, cmOrigenCordoba, estDocEnBolsinEnviado, f1.minusDays(2), null);
            Remito r7 = new Remito(112, f1.minusDays(2), cmOrigenCordoba, cmCentral);
            r7.agregarDetalle(new DetalleRemito(d950, "Archivo"));
            em.persist(new Bolsin(7, 5500, f1.minusDays(2), 1.5, cmOrigenCordoba, cmCentral,
                    List.of(r7), estBolsinRecibido, f1.minusDays(2), null));

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
            return em.createQuery("SELECT e FROM Estado e", Estado.class).getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public List<ComisionMedica> obtenerComisionesMedicas() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT c FROM ComisionMedica c", ComisionMedica.class).getResultList();
        } finally {
            em.close();
        }
    }

    @Override
    public List<Bolsin> obtenerTodosBolsines() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT b FROM Bolsin b", Bolsin.class).getResultList();
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
                    ComisionMedica cm = empleadoResponsable.getCM();
                    asegurarComisionMedica(em, cm.getCodigo(), cm.getNombre(),
                            cm.getDireccion(), cm.getEmail(), cm.getTelefono());
                }
                if (em.find(Empleado.class, empleadoResponsable.getNombre()) == null) {
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
        Estado nuevo = new Estado(nombre, nombre, ambito);
        em.persist(nuevo);
        return nuevo;
    }

    private ComisionMedica asegurarComisionMedica(EntityManager em, int codigo, String nombre,
                                                 String direccion, String email, String telefono) {
        ComisionMedica existente = em.find(ComisionMedica.class, codigo);
        if (existente != null) {
            return existente;
        }
        ComisionMedica nueva = new ComisionMedica(codigo, nombre, direccion, email, telefono);
        em.persist(nueva);
        return nueva;
    }

    private TipoDocumento asegurarTipoDocumento(EntityManager em, String nombre, String descripcion) {
        TipoDocumento existente = em.find(TipoDocumento.class, nombre);
        if (existente != null) {
            return existente;
        }
        TipoDocumento nuevo = new TipoDocumento(nombre, descripcion);
        em.persist(nuevo);
        return nuevo;
    }
}
