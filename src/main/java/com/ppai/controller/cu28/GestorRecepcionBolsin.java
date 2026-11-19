package com.ppai.controller.cu28;

import com.ppai.boundary.cu28.InterfazServidorCorreo;
import com.ppai.boundary.cu28.PantallaRecepcionBolsin;
import com.ppai.domain.model.cu28.*;
import com.ppai.iterator.IAgregado;
import com.ppai.iterator.IIterador;
import com.ppai.iterator.IteradorBolsin;
import com.ppai.repository.cu28.IRepositorioCU28;
import com.ppai.repository.cu28.RepositorioJpaCU28;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador GestorRecepcionBolsin — CU 28: Registrar Recepción de Bolsín.
 * Orquesta los mensajes del caso de uso entre la Pantalla (Boundary) y el Dominio (Entity).
 * Implementa IAgregado para delegar el recorrido y filtrado de bolsines en IteradorBolsin.
 * Mantiene estricta trazabilidad con el Diagrama de Secuencia y el modelo de objetos DSI.
 *
 * NOTA DE DISEÑO: No utiliza ni extiende java.util.Iterable ni java.util.Iterator.
 */
public class GestorRecepcionBolsin implements IAgregado {

    private PantallaRecepcionBolsin pantalla;
    private final InterfazServidorCorreo servidorCorreo;
    private final IRepositorioCU28 repositorio;

    private Sesion sesionActual;
    private ComisionMedica comisionMedicaEmpleado;
    private Empleado empleadoActual;

    private List<Bolsin> todosBolsines;
    private List<Bolsin> bolsinesEnviados;
    private Bolsin bolsinSeleccionado;
    private Remito remitoSeleccionado;
    private OpcionRecepcion opcionSeleccionada;
    private List<Documentacion> documentacionesNoConformes;

    private List<Estado> estadosDisponibles;
    private Estado estadoRecibidoCMDestino;
    private Estado estadoRemitoRecibidoAceptado;
    private Estado estadoRemitoRecibidoAceptadoParcial;
    private Estado estadoDocRecibidaAceptada;
    private Estado estadoDocNoRecibida;
    private Estado estadoDocRecibidaRechazada;
    private Estado estadoDocParaRedirigir;

    private LocalDateTime fechaHoraActual;
    private NotificacionRecepcionBolsin ultimaNotificacionEnviada;

    public GestorRecepcionBolsin(Sesion sesionActual) {
        this(sesionActual, new RepositorioJpaCU28());
    }

    public GestorRecepcionBolsin(Sesion sesionActual, IRepositorioCU28 repositorio) {
        this.sesionActual = sesionActual;
        this.servidorCorreo = new InterfazServidorCorreo();
        this.repositorio = repositorio;
        this.bolsinesEnviados = new ArrayList<>();
        this.estadosDisponibles = new ArrayList<>();
        this.todosBolsines = new ArrayList<>();
        this.documentacionesNoConformes = new ArrayList<>();

        inicializarBolsinesDeEjemplo(1); // Por defecto: Escenario 1 (CMC)
        inicializarEstadosDelSistema();
    }

    public void setPantalla(PantallaRecepcionBolsin pantalla) {
        this.pantalla = pantalla;
    }

    public PantallaRecepcionBolsin getPantalla() {
        return pantalla;
    }

    /**
     * DS-CU28 pasos 1-3: Coordina identificación de usuario y búsqueda de bolsines.
     */
    public void tomarOpcionRegistrarRecepcionBolsin() {
        System.out.println("[GESTOR] Iniciando CU-28: Registrar Recepción de Bolsín.");

        this.bolsinSeleccionado = null;
        this.remitoSeleccionado = null;
        this.opcionSeleccionada = null;
        this.documentacionesNoConformes = new ArrayList<>();

        // DS-CU28 pasos 4-7: Buscar usuario y CM
        buscarUsuario();

        // DS-CU28 paso 8: Mostrar CM del usuario en pantalla
        if (pantalla != null) {
            pantalla.mostrarCM(comisionMedicaEmpleado);
        }

        // DS-CU28 paso 9 + sec. 2: Buscar bolsines en estado Enviado con Patrón Iterator
        buscarBolsinEstadoEnviado();

        // DS-CU28 sec. 3 + paso 10: Mostrar bolsines y solicitar selección (FUERA DEL LOOP)
        if (pantalla != null) {
            pantalla.mostrarBolsines(bolsinesEnviados);
            pantalla.mostrarNumeroPrecinto();
            pantalla.mostrarCMOrigen();
            pantalla.solicitarSeleccionBolsin();
        }
    }

    /**
     * DS-CU28 pasos 4-7: Obtiene el usuario autenticado y su Comisión Médica asignada.
     */
    public void buscarUsuario() {
        Usuario usuarioActual = sesionActual.getUsuario();
        this.empleadoActual = usuarioActual.getEmpleado();
        this.comisionMedicaEmpleado = usuarioActual.getCMEmpleado();
        System.out.println("[GESTOR] Usuario: " + usuarioActual.getUsername()
                + " | Empleado: " + empleadoActual.getNombre()
                + " | CM: " + comisionMedicaEmpleado.getNombre());
    }

    @Override
    public IIterador crearIterador(Object[] elementos) {
        Bolsin[] array;
        if (elementos instanceof Bolsin[]) {
            array = (Bolsin[]) elementos;
        } else if (elementos != null) {
            array = new Bolsin[elementos.length];
            for (int i = 0; i < elementos.length; i++) {
                array[i] = (Bolsin) elementos[i];
            }
        } else {
            array = this.todosBolsines.toArray(new Bolsin[0]);
        }
        Object[] filtros = new Object[]{ this.comisionMedicaEmpleado };
        return new IteradorBolsin(array, filtros);
    }

    public IIterador crearIterador(Bolsin[] elementos) {
        Object[] filtros = new Object[]{ this.comisionMedicaEmpleado };
        return new IteradorBolsin(elementos != null ? elementos : this.todosBolsines.toArray(new Bolsin[0]), filtros);
    }

    /**
     * DS-CU28 paso 9 y loop sec. 2: Filtra bolsines cuyo estado sea ENVIADO hacia la CM del empleado
     * utilizando el patrón Iterator propio (IteradorBolsin).
     */
    public void buscarBolsinEstadoEnviado() {
        this.bolsinesEnviados.clear();
        this.todosBolsines = new ArrayList<>(repositorio.obtenerTodosBolsines());
        Bolsin[] elementos = todosBolsines.toArray(new Bolsin[0]);
        IIterador itBol = crearIterador(elementos);
        itBol.primero();
        while (!itBol.haFinalizado()) {
            Bolsin bolsin = (Bolsin) itBol.actual();
            if (bolsin != null) {
                bolsin.getNombreCMOrigen();
                bolsin.getNroPrecinto();
                bolsinesEnviados.add(bolsin);
            }
            itBol.siguiente();
        }
        System.out.println("[GESTOR] Bolsines en estado ENVIADO encontrados: " + bolsinesEnviados.size());
    }

    /**
     * Flujos Alternativos A1 y A2: Filtra bolsines por número de precinto y/o CM de origen.
     */
    public List<Bolsin> filtrarBolsines(String filtroPrecinto, ComisionMedica filtroCMOrigen) {
        List<Bolsin> filtrados = new ArrayList<>(bolsinesEnviados);

        if (filtroPrecinto != null && !filtroPrecinto.trim().isEmpty()) {
            String f = filtroPrecinto.trim().toLowerCase();
            filtrados = filtrados.stream()
                    .filter(b -> b.getNumeroPrecinto().toLowerCase().contains(f))
                    .collect(Collectors.toList());

            if (filtrados.isEmpty()) {
                if (pantalla != null) {
                    pantalla.mostrarAlerta("Flujo Alternativo A1",
                            "No se encuentra bolsín con el número de precinto ingresado: '" + filtroPrecinto + "'.");
                }
                return filtrados;
            }
        }

        if (filtroCMOrigen != null) {
            filtrados = filtrados.stream()
                    .filter(b -> b.getCMOrigen().getId().equals(filtroCMOrigen.getId()))
                    .collect(Collectors.toList());

            if (filtrados.isEmpty() && pantalla != null) {
                pantalla.mostrarAlerta("Flujo Alternativo A2",
                        "No se encuentra bolsín con la CM origen seleccionada: " + filtroCMOrigen.getNombre() + ".");
            }
        }

        return filtrados;
    }

    /**
     * DS-CU28 paso 11-12: Captura la selección de un bolsín a través de su objeto o precinto.
     */
    public void tomarSeleccionBolsin(Bolsin bolsin) {
        if (bolsin == null) {
            System.out.println("[GESTOR] Advertencia: Se intentó seleccionar un bolsín nulo.");
            return;
        }

        this.bolsinSeleccionado = bolsin;
        System.out.println("[GESTOR] Bolsín seleccionado: " + bolsinSeleccionado.getNumeroPrecinto());
        obtenerInformacionRemito();
    }

    /**
     * Sobrecarga de compatibilidad con la secuencia de texto.
     */
    public void tomarSeleccionOpcion(String precinto) {
        Bolsin encontrado = null;
        for (Bolsin b : bolsinesEnviados) {
            if (b.getNumeroPrecinto().equalsIgnoreCase(precinto)) {
                encontrado = b;
                break;
            }
        }
        if (encontrado != null) {
            tomarSeleccionBolsin(encontrado);
        } else if (pantalla != null) {
            pantalla.mostrarAlerta("Bolsín no encontrado", "No se encontró un bolsín con precinto: " + precinto);
        }
    }

    /**
     * DS-CU28 pasos 13-17: Obtiene datos del remito, los muestra en pantalla y presenta opciones de recepción.
     */
    public void obtenerInformacionRemito() {
        if (bolsinSeleccionado == null) return;

        this.remitoSeleccionado = bolsinSeleccionado.obtenerInformacionRemito();
        if (remitoSeleccionado == null) {
            if (pantalla != null) {
                pantalla.mostrarAlerta("Sin Remito", "El bolsín seleccionado no tiene remito asociado.");
            }
            return;
        }

        // DS-CU28 paso 15: Mostrar datos de remito
        if (pantalla != null) {
            pantalla.mostrarDatosDeRemito(remitoSeleccionado);

            // DS-CU28 paso 16-17: Mostrar opciones y solicitar selección
            List<OpcionRecepcion> opciones = buscarOpcionesRecepcion();
            pantalla.mostrarOpcionesRecepcion(opciones);
            pantalla.solicitarSeleccionOpcion();
        }
    }

    /**
     * Genera la lista de opciones de recepción según la regla de negocio del CU 28.
     * Evalúa si la CM destino es la CMC para habilitar la opción 4.
     */
    public List<OpcionRecepcion> buscarOpcionesRecepcion() {
        List<OpcionRecepcion> opciones = new ArrayList<>();

        boolean esDestinoCMC = comisionMedicaEmpleado != null
                && (comisionMedicaEmpleado.getNombre().toUpperCase().contains("CENTRAL")
                || comisionMedicaEmpleado.getNombre().toUpperCase().contains("CMC"));

        opciones.add(new OpcionRecepcion(
                1,
                "El contenido es igual al registrado",
                "Flujo básico: Todos los documentos y remitos coinciden exactamente con lo enviado.",
                "Recibido y aceptado",
                "Recibida y aceptada (todas)",
                false,
                false
        ));

        opciones.add(new OpcionRecepcion(
                2,
                "No se recibe toda la documentación",
                "Flujo A3: Uno o más documentos registrados en el remito no están físicamente en el bolsín.",
                "Recibido y aceptado parcial",
                "Recibida y aceptada (las presentes) / No recibida (las faltantes)",
                true,
                false
        ));

        opciones.add(new OpcionRecepcion(
                3,
                "Existe documentación que no corresponde al destino",
                "Flujo A4: Se recibió documentación física dirigida erróneamente a esta Comisión Médica.",
                "Recibido y aceptado parcial",
                "Recibida y aceptada (las correctas) / Recibida y rechazada (las erróneas)",
                true,
                false
        ));

        opciones.add(new OpcionRecepcion(
                4,
                "La documentación se debe redirigir a otra área",
                "Flujo A5: Aplica exclusivamente si la CM destino es la Comisión Médica Central (CMC).",
                "Recibido y aceptado",
                "Para redirigir (pase interno sin bolsín)",
                false,
                true // Solo aplica si es CMC
        ));

        return opciones;
    }

    /**
     * DS-CU28 pasos 18-19: Captura la opción de recepción elegida y la lista de documentos afectados (para A3 o A4).
     */
    public void tomarSeleccionOpcion(OpcionRecepcion opcion, List<Documentacion> docsNoConformes) {
        if (opcion != null && opcion.isSoloCMC()) {
            boolean esDestinoCMC = comisionMedicaEmpleado != null
                    && (comisionMedicaEmpleado.getNombre().toUpperCase().contains("CENTRAL")
                    || comisionMedicaEmpleado.getNombre().toUpperCase().contains("CMC"));
            if (!esDestinoCMC) {
                System.out.println("[GESTOR] Advertencia Flujo A5: La opción 4 solo está permitida para Comisión Médica Central.");
                if (pantalla != null) {
                    pantalla.mostrarAlerta("Opción No Disponible (Flujo A5)",
                            "La opción 'Redirigir a otra área' solo aplica si la CM destino es CMC.");
                }
                this.opcionSeleccionada = null;
                return;
            }
        }

        this.opcionSeleccionada = opcion;
        this.documentacionesNoConformes = (docsNoConformes != null) ? new ArrayList<>(docsNoConformes) : new ArrayList<>();

        System.out.println("[GESTOR] Opción de recepción seleccionada: " + opcion.getTitulo()
                + " | Documentos observados: " + this.documentacionesNoConformes.size());

        // DS-CU28 paso 20: Solicitar confirmación
        if (pantalla != null) {
            pantalla.solicitarConfirmacionSeleccion(bolsinSeleccionado, remitoSeleccionado, opcionSeleccionada, this.documentacionesNoConformes);
        }
    }

    /**
     * DS-CU28 pasos 21-22: Procesa la confirmación del Encargado de Bolsines.
     */
    public void tomarConfirmacionSeleccion(boolean confirmada) {
        if (!confirmada) {
            System.out.println("[GESTOR] Flujo alternativo A6: El Encargado de Bolsines no confirma la operación.");
            if (pantalla != null) {
                pantalla.mostrarAlerta("Operación Cancelada (A6)",
                        "No se confirmó la recepción. El bolsín permanece en estado 'Enviado'.");
                pantalla.cancelar();
            }
            return;
        }

        System.out.println("[GESTOR] Recepción confirmada. Iniciando actualización de estados...");

        // DS-CU28 paso 23: Recuperar estados del sistema
        obtenerEstado();

        // DS-CU28 paso 24: Capturar fecha y hora actual
        getFechaHoraActual();

        // DS-CU28 pasos 25-30: Actualizar estados de Bolsín
        actualizarEstadoBolsin();

        // DS-CU28 pasos 39-40: Actualizar estados de Remitos y Documentaciones
        actualizarRemitoYDocumentacion();

        // Persistencia transaccional ACID en BD H2 vía JPA/Hibernate
        persistirRecepcion();

        // DS-CU28 pasos 31-32: Notificar recepción (CU 29 post-commit)
        notificarRecepcion();

        // DS-CU28 paso 33: Informar en pantalla resultado y notificación
        if (pantalla != null) {
            pantalla.informarCorrectoEnvioNotificacion(ultimaNotificacionEnviada);
        }

        // DS-CU28 paso 34: Finalizar caso de uso
        finCU();
    }

    /**
     * Sobrecarga de compatibilidad con strings.
     */
    public void tomarConfirmacionSeleccion(String opcionRecepcion, String confirmacion) {
        boolean conf = "S".equalsIgnoreCase(confirmacion) || "SI".equalsIgnoreCase(confirmacion);
        tomarConfirmacionSeleccion(conf);
    }

    /**
     * DS-CU28 paso 23 y loops sec. 6: Recupera los objetos Estado desde el repositorio en memoria.
     */
    public void obtenerEstado() {
        this.estadosDisponibles = new ArrayList<>(repositorio.obtenerEstados());
        for (Estado estado : estadosDisponibles) {
            if (estado.esAmbitoBolsin() && estado.esEstadoRecibidoEnCMDestino()) {
                this.estadoRecibidoCMDestino = estado;
            }
            if (estado.esAmbitoRemito() && estado.esRecibidoYAceptado()) {
                this.estadoRemitoRecibidoAceptado = estado;
            }
            if (estado.esAmbitoRemito() && estado.esRecibidoYAceptadoParcial()) {
                this.estadoRemitoRecibidoAceptadoParcial = estado;
            }
            if (estado.esAmbitoDocumentacion() && estado.esRecibidoYAceptado()) {
                this.estadoDocRecibidaAceptada = estado;
            }
            if (estado.esAmbitoDocumentacion() && estado.esNoRecibida()) {
                this.estadoDocNoRecibida = estado;
            }
            if (estado.esAmbitoDocumentacion() && estado.esRecibidaYRechazada()) {
                this.estadoDocRecibidaRechazada = estado;
            }
            if (estado.esAmbitoDocumentacion() && estado.esParaRedirigir()) {
                this.estadoDocParaRedirigir = estado;
            }
        }
    }

    /**
     * DS-CU28 paso 24: Captura fecha y hora actual del sistema.
     */
    public void getFechaHoraActual() {
        this.fechaHoraActual = LocalDateTime.now();
    }

    /**
     * DS-CU28 pasos 25-30: Delega la actualización de estado del bolsín.
     */
    public void actualizarEstadoBolsin() {
        if (bolsinSeleccionado == null || estadoRecibidoCMDestino == null) {
            System.out.println("[GESTOR] Error: Datos incompletos para actualizar estado del bolsín.");
            return;
        }

        // 1. Actualizar Bolsín -> Siempre "Recibido en CM destino" (+ fecha/hora + empleado)
        bolsinSeleccionado.marcarRecibido(estadoRecibidoCMDestino, fechaHoraActual, empleadoActual);
    }

    /**
     * Persiste de forma transaccional en BD (JPA/Hibernate) los cambios del bolsín,
     * su remito, las documentaciones y los historiales de estado.
     */
    public void persistirRecepcion() {
        if (bolsinSeleccionado == null) {
            return;
        }
        this.bolsinSeleccionado = repositorio.actualizarBolsin(this.bolsinSeleccionado, this.empleadoActual);
        if (this.bolsinSeleccionado != null) {
            this.remitoSeleccionado = this.bolsinSeleccionado.getRemito();
        }
    }

    /**
     * DS-CU28 pasos 39-40 y loop sec. 8: Delega la actualización de remitos y documentaciones.
     * En el flujo básico (Opción 1), delega en bolsinSeleccionado.actualizarRemitoYDocumentacion(...)
     * conforme a la especificación estricta de message.md.
     */
    public void actualizarRemitoYDocumentacion() {
        if (bolsinSeleccionado == null) {
            return;
        }

        if (opcionSeleccionada == null || opcionSeleccionada.esContenidoIgual()) {
            // Opción 1: Orquestador único en Bolsin -> Remito -> DetalleRemito -> Documentacion
            if (remitoSeleccionado != null) {
                remitoSeleccionado.marcarRecibidoYAceptado(estadoRemitoRecibidoAceptado);
            }
            bolsinSeleccionado.actualizarRemitoYDocumentacion(estadoDocRecibidaAceptada, fechaHoraActual, empleadoActual);
        } else if (opcionSeleccionada.esFaltaDocumentacion()) {
            // Opción 2 (A3): Remito a Recibido y aceptado parcial; docs faltantes a No recibida, demás a Recibida y aceptada
            if (remitoSeleccionado != null) {
                remitoSeleccionado.marcarRecibidoYAceptadoParcial(estadoRemitoRecibidoAceptadoParcial);
                for (DetalleRemito detalle : remitoSeleccionado.buscarDocumentacion()) {
                    Documentacion doc = detalle.getDocumentacion();
                    if (documentacionesNoConformes.contains(doc)) {
                        doc.marcarNoRecibida(estadoDocNoRecibida, fechaHoraActual, empleadoActual);
                    } else {
                        doc.aceptar(estadoDocRecibidaAceptada, fechaHoraActual, empleadoActual);
                    }
                }
            }
        } else if (opcionSeleccionada.esNoCorrespondeDestino()) {
            // Opción 3 (A4): Remito a Recibido y aceptado parcial; erróneas a Recibida y rechazada, demás a Recibida y aceptada
            if (remitoSeleccionado != null) {
                remitoSeleccionado.marcarRecibidoYAceptadoParcial(estadoRemitoRecibidoAceptadoParcial);
                for (DetalleRemito detalle : remitoSeleccionado.buscarDocumentacion()) {
                    Documentacion doc = detalle.getDocumentacion();
                    if (documentacionesNoConformes.contains(doc)) {
                        doc.rechazar(estadoDocRecibidaRechazada, fechaHoraActual, empleadoActual);
                    } else {
                        doc.aceptar(estadoDocRecibidaAceptada, fechaHoraActual, empleadoActual);
                    }
                }
            }
        } else if (opcionSeleccionada.esRedirigirAOtraArea()) {
            // Opción 4 (A5): Remito a Recibido y aceptado; docs a Para redirigir
            if (remitoSeleccionado != null) {
                remitoSeleccionado.marcarRecibidoYAceptado(estadoRemitoRecibidoAceptado);
                for (DetalleRemito detalle : remitoSeleccionado.buscarDocumentacion()) {
                    Documentacion doc = detalle.getDocumentacion();
                    doc.paraRedirigir(estadoDocParaRedirigir, fechaHoraActual, empleadoActual);
                }
            }
        }
    }

    /**
     * DS-CU28 pasos 31-32: Incluye el CU 29 para notificar a la CM de origen por correo.
     */
    public void notificarRecepcion() {
        StringBuilder detalleDocs = new StringBuilder();
        if (remitoSeleccionado != null) {
            for (DetalleRemito d : remitoSeleccionado.buscarDocumentacion()) {
                Documentacion doc = d.getDocumentacion();
                detalleDocs.append("    • [")
                        .append(doc.mostrarTipoDocumentacion())
                        .append("] ")
                        .append(doc.getAsunto())
                        .append(" ── Estado: ")
                        .append(doc.getEstadoActual() != null ? doc.getEstadoActual().getNombreDisplay() : "Procesada")
                        .append("\n");
            }
        }

        String cmOrigenNombre = bolsinSeleccionado.getCMOrigen().getNombre();
        String emailOrigen = "despacho." + cmOrigenNombre.toLowerCase().replaceAll("[^a-z0-9]", "") + "@srt.gob.ar";

        this.ultimaNotificacionEnviada = new NotificacionRecepcionBolsin(
                cmOrigenNombre,
                emailOrigen,
                bolsinSeleccionado.getNumeroPrecinto(),
                remitoSeleccionado != null ? remitoSeleccionado.getNumero() : "N/A",
                estadoRecibidoCMDestino.getNombreDisplay(),
                remitoSeleccionado != null && remitoSeleccionado.getEstado() != null
                        ? remitoSeleccionado.getEstado().getNombreDisplay() : "Aceptado",
                detalleDocs.toString(),
                fechaHoraActual,
                empleadoActual != null ? empleadoActual.getNombre() : "Encargado de Bolsines"
        );

        // Envío formal a través de la interfaz de boundary con el servidor de correo
        servidorCorreo.enviarNotificacion(ultimaNotificacionEnviada);
    }

    /**
     * DS-CU28 paso 34: Finaliza el CU liberando las referencias transitorias.
     */
    public void finCU() {
        System.out.println("[GESTOR] Caso de uso CU-28 finalizado con éxito.");
        this.bolsinSeleccionado = null;
        this.remitoSeleccionado = null;
        this.opcionSeleccionada = null;
        this.documentacionesNoConformes = new ArrayList<>();
    }

    /**
     * Cancela la operación actual en cualquier momento (Obs. 1).
     */
    public void cancelarRecepcion() {
        System.out.println("[GESTOR] Operación cancelada por el usuario (Obs. 1).");
        this.bolsinSeleccionado = null;
        this.remitoSeleccionado = null;
        this.opcionSeleccionada = null;
        this.documentacionesNoConformes = new ArrayList<>();
        buscarBolsinEstadoEnviado();
        if (pantalla != null) {
            pantalla.mostrarBolsines(bolsinesEnviados);
        }
    }

    /**
     * Permite alternar escenarios de prueba mock (CMC vs CM Regional) para probar todas las ramas del caso.
     */
    public void cargarEscenario(int escenario) {
        if (escenario == 1) {
            // Escenario 1: Comisión Médica Central (CMC) - CABA
            ComisionMedica cmCentral = new ComisionMedica(1L, "Comisión Médica Central (CMC) - CABA");
            Empleado emp = new Empleado(100L, "Juan Pérez (Encargado Central)", cmCentral);
            Usuario user = new Usuario("jperez.cmc", emp);
            this.sesionActual = new Sesion(user);
        } else {
            // Escenario 2: CM Jurisdiccional Rosario
            ComisionMedica cmRosario = new ComisionMedica(2L, "CMJ N° 7 - Rosario");
            Empleado emp = new Empleado(200L, "Mariana López (Encargada Rosario)", cmRosario);
            Usuario user = new Usuario("mlopez.rosario", emp);
            this.sesionActual = new Sesion(user);
        }

        inicializarBolsinesDeEjemplo(escenario);
        tomarOpcionRegistrarRecepcionBolsin();
    }

    private void inicializarEstadosDelSistema() {
        this.estadosDisponibles = new ArrayList<>(repositorio.obtenerEstados());
    }

    public void inicializarBolsinesDeEjemplo(int escenario) {
        repositorio.inicializarDatosEscenario(sesionActual, escenario);
        this.estadosDisponibles = new ArrayList<>(repositorio.obtenerEstados());
        this.todosBolsines = new ArrayList<>(repositorio.obtenerTodosBolsines());
    }

    // Getters auxiliares para la interfaz
    public Sesion getSesionActual() {
        return sesionActual;
    }

    public ComisionMedica getComisionMedicaEmpleado() {
        return comisionMedicaEmpleado;
    }

    public Empleado getEmpleadoActual() {
        return empleadoActual;
    }

    public List<Bolsin> getBolsinesEnviados() {
        return bolsinesEnviados;
    }

    public List<Bolsin> getTodosBolsines() {
        return todosBolsines;
    }

    public List<ComisionMedica> getComisionesMedicasFiltro() {
        return repositorio.obtenerComisionesMedicas();
    }

    public Bolsin getBolsinSeleccionado() {
        return bolsinSeleccionado;
    }

    public Remito getRemitoSeleccionado() {
        return remitoSeleccionado;
    }

    public OpcionRecepcion getOpcionSeleccionada() {
        return opcionSeleccionada;
    }

    public NotificacionRecepcionBolsin getUltimaNotificacionEnviada() {
        return ultimaNotificacionEnviada;
    }

    public List<Documentacion> getDocumentacionesNoConformes() {
        return documentacionesNoConformes;
    }
}
