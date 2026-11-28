package com.ppai.controller.cu28;

import com.ppai.boundary.cu28.PantallaRecepcionBolsin;
import com.ppai.controller.cu29.GestorNotificarRecepcionBolsin;
import com.ppai.domain.model.cu28.Bolsin;
import com.ppai.domain.model.cu28.ComisionMedica;
import com.ppai.domain.model.cu28.DetalleRemito;
import com.ppai.domain.model.cu28.Documentacion;
import com.ppai.domain.model.cu28.Empleado;
import com.ppai.domain.model.cu28.Estado;
import com.ppai.domain.model.cu28.Remito;
import com.ppai.domain.model.cu28.Sesion;
import com.ppai.domain.model.cu28.Usuario;
import com.ppai.iterator.IAgregado;
import com.ppai.iterator.IIterador;
import com.ppai.iterator.IteradorBolsin;
import com.ppai.repository.cu28.IRepositorioCU28;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Controlador GestorRecepcionBolsin — CU 28: Registrar Recepción de Bolsín.
 * Orquesta el caso de uso entre PantallaRecepcionBolsin (Boundary) y el Dominio OO puro,
 * exponiendo hacia la interfaz exclusivamente tipos primitivos / String / arreglos.
 * Implementa IAgregado para delegar el recorrido y filtrado de bolsines en IteradorBolsin.
 *
 * NOTA DE DISEÑO: No utiliza ni extiende java.util.Iterable ni java.util.Iterator.
 */
public class GestorRecepcionBolsin implements IAgregado {

    public static final int OPCION_CONTENIDO_IGUAL = 1;
    public static final int OPCION_FALTA_DOCUMENTACION = 2;
    public static final int OPCION_NO_CORRESPONDE_DESTINO = 3;
    public static final int OPCION_REDIRIGIR_OTRA_AREA = 4;

    private static final String SEV_OK = "OK";
    private static final String SEV_WARN = "WARN";
    private static final String SEV_ERROR = "ERROR";
    private static final String SEV_INFO = "INFO";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private PantallaRecepcionBolsin pantalla;
    private final IRepositorioCU28 repositorio;
    private final GestorNotificarRecepcionBolsin gestorNotificacion;

    private Sesion sesionActual;
    private ComisionMedica comisionMedicaEmpleado;
    private Empleado empleadoActual;

    private List<Bolsin> todosBolsines;
    private List<Bolsin> bolsinesEnviados;
    private Bolsin bolsinSeleccionado;
    private int opcionSeleccionada;
    private Set<Integer> numerosDocsNoConformes;

    private List<Estado> estadosDisponibles;
    private Estado estadoRecibidoCMDestino;
    private Estado estadoRemitoRecibidoAceptado;
    private Estado estadoRemitoRecibidoAceptadoParcial;
    private Estado estadoDocRecibidaAceptada;
    private Estado estadoDocNoRecibida;
    private Estado estadoDocRecibidaRechazada;
    private Estado estadoDocParaRedirigir;

    private LocalDateTime fechaHoraActual;
    private String[] ultimaNotificacionDatos;
    private String[][] ultimosRemitosActualizados;

    public GestorRecepcionBolsin(Sesion sesionActual, IRepositorioCU28 repositorio) {
        this(sesionActual, repositorio, new GestorNotificarRecepcionBolsin());
    }

    public GestorRecepcionBolsin(Sesion sesionActual,
                                 IRepositorioCU28 repositorio,
                                 GestorNotificarRecepcionBolsin gestorNotificacion) {
        this.sesionActual = sesionActual;
        this.repositorio = repositorio;
        this.gestorNotificacion = gestorNotificacion;
        this.bolsinesEnviados = new ArrayList<>();
        this.estadosDisponibles = new ArrayList<>(repositorio.obtenerEstados());
        this.todosBolsines = new ArrayList<>(repositorio.obtenerTodosBolsines());
        this.numerosDocsNoConformes = new LinkedHashSet<>();
    }

    public void setPantalla(PantallaRecepcionBolsin pantalla) {
        this.pantalla = pantalla;
    }

    /**
     * DS-CU28 pasos 1-3: Coordina identificación de usuario y búsqueda de bolsines.
     */
    public void tomarOpcionRegistrarRecepcionBolsin() {
        System.out.println("[GESTOR] Iniciando CU-28: Registrar Recepción de Bolsín.");

        this.bolsinSeleccionado = null;
        this.opcionSeleccionada = 0;
        this.numerosDocsNoConformes = new LinkedHashSet<>();

        buscarUsuario();

        if (pantalla != null) {
            String nombreUsr = (sesionActual != null && sesionActual.getUsuario() != null)
                    ? sesionActual.getUsuario().getNombre() : "";
            String nombreEmp = (empleadoActual != null) ? empleadoActual.getNombre() : "";
            String nombreCM = (comisionMedicaEmpleado != null) ? comisionMedicaEmpleado.getNombre() : "";
            pantalla.mostrarCM(nombreUsr, nombreEmp, nombreCM);
        }

        buscarBolsinEstadoEnviado();
        notificarListadoBolsinesAPantalla(this.bolsinesEnviados);
    }

    /**
     * DS-CU28 pasos 4-7: Obtiene usuario autenticado, su CM y empleado responsable vía Sesion.
     */
    public void buscarUsuario() {
        Usuario usuarioActual = sesionActual.getUsuario();
        this.comisionMedicaEmpleado = sesionActual.getCMEmpleado();
        this.empleadoActual = sesionActual.getEmpleadoLogueado();
        System.out.println("[GESTOR] Usuario: " + (usuarioActual != null ? usuarioActual.getNombre() : "-")
                + " | Empleado: " + (empleadoActual != null ? empleadoActual.getNombre() : "-")
                + " | CM: " + (comisionMedicaEmpleado != null ? comisionMedicaEmpleado.getNombre() : "-"));
    }

    @Override
    public IIterador crearIterador(Object[] elementos) {
        Object[] fuente = (elementos != null) ? elementos : this.todosBolsines.toArray(new Bolsin[0]);
        String nombreCM = (this.comisionMedicaEmpleado != null) ? this.comisionMedicaEmpleado.getNombre() : "";
        Object[] filtros = new Object[]{ nombreCM };
        return new IteradorBolsin(fuente, filtros);
    }

    /**
     * DS-CU28 paso 9 y loop sec. 2: Filtra bolsines en estado ENVIADO dirigidos a la CM del usuario
     * mediante IteradorBolsin (sin look-ahead).
     */
    public void buscarBolsinEstadoEnviado() {
        this.bolsinesEnviados.clear();
        this.todosBolsines = new ArrayList<>(repositorio.obtenerTodosBolsines());
        Bolsin[] elementos = todosBolsines.toArray(new Bolsin[0]);
        IIterador itBol = crearIterador(elementos);
        itBol.primero();
        while (!itBol.haFinalizado()) {
            Bolsin bolsin = (Bolsin) itBol.elementoActual();
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
     * Flujos Alternativos A1 y A2: Filtra bolsines por precinto (String comparado contra int)
     * y/o nombre de CM de origen, empujando los resultados a la pantalla en primitivos.
     */
    public int[] filtrarBolsines(String filtroPrecinto, String nombreCMOrigen) {
        List<Bolsin> filtrados = new ArrayList<>(this.bolsinesEnviados);

        if (filtroPrecinto != null && !filtroPrecinto.trim().isEmpty()) {
            String f = filtroPrecinto.trim();
            List<Bolsin> porPrecinto = new ArrayList<>();
            for (Bolsin b : filtrados) {
                if (String.valueOf(b.getNroPrecinto()).contains(f)) {
                    porPrecinto.add(b);
                }
            }
            filtrados = porPrecinto;

            if (filtrados.isEmpty()) {
                notificarListadoBolsinesAPantalla(filtrados);
                if (pantalla != null) {
                    pantalla.mostrarAlerta("Bolsín no encontrado",
                            "No se encuentra bolsín con el número de precinto ingresado: '" + filtroPrecinto + "'.");
                }
                return new int[0];
            }
        }

        if (nombreCMOrigen != null && !nombreCMOrigen.trim().isEmpty()) {
            String cmFiltro = nombreCMOrigen.trim();
            List<Bolsin> porCM = new ArrayList<>();
            for (Bolsin b : filtrados) {
                if (b.getNombreCMOrigen().equalsIgnoreCase(cmFiltro)) {
                    porCM.add(b);
                }
            }
            filtrados = porCM;

            if (filtrados.isEmpty()) {
                notificarListadoBolsinesAPantalla(filtrados);
                if (pantalla != null) {
                    pantalla.mostrarAlerta("Sin envíos para la CM origen",
                            "No se encuentra bolsín con la CM origen seleccionada: " + cmFiltro + ".");
                }
                return new int[0];
            }
        }

        notificarListadoBolsinesAPantalla(filtrados);
        return extraerPrecintos(filtrados);
    }

    /**
     * Restablece la vista al listado completo de bolsines enviados (sin pull desde UI).
     */
    public void limpiarFiltros() {
        notificarListadoBolsinesAPantalla(this.bolsinesEnviados);
    }

    /**
     * DS-CU28 pasos 11-12: Captura el número de precinto seleccionado desde la pantalla.
     */
    public void tomarSeleccionBolsin(int numeroPrecinto) {
        Bolsin encontrado = null;
        for (Bolsin b : bolsinesEnviados) {
            if (b.getNroPrecinto() == numeroPrecinto) {
                encontrado = b;
                break;
            }
        }
        if (encontrado == null) {
            if (pantalla != null) {
                pantalla.mostrarAlerta("Bolsín no encontrado",
                        "No se encontró un bolsín en estado Enviado con precinto: " + numeroPrecinto);
            }
            return;
        }

        this.bolsinSeleccionado = encontrado;
        System.out.println("[GESTOR] Bolsín seleccionado: " + bolsinSeleccionado.getNroPrecinto());
        obtenerInformacionRemito();
    }

    /**
     * DS-CU28 pasos 13-17: Lee todos los remitos y documentaciones del bolsín seleccionado
     * (mediante IteradorRemito -> IteradorDetalleRemito) y envía matrices de Strings a la pantalla.
     */
    public void obtenerInformacionRemito() {
        if (bolsinSeleccionado == null) {
            return;
        }

        String[][] infoRemitos = bolsinSeleccionado.obtenerInformacionRemito();
        if (bolsinSeleccionado.getRemitos().isEmpty()) {
            if (pantalla != null) {
                pantalla.mostrarAlerta("Sin Remito", "El bolsín seleccionado no tiene remitos asociados.");
            }
            return;
        }

        List<String[]> filasDocs = new ArrayList<>();
        StringBuilder remitosCsv = new StringBuilder();
        List<Remito> remitos = bolsinSeleccionado.getRemitos();
        for (int i = 0; i < remitos.size(); i++) {
            Remito r = remitos.get(i);
            if (i > 0) {
                remitosCsv.append(", ");
            }
            remitosCsv.append(r.getNumero());

            for (DetalleRemito det : r.getDetalles()) {
                Documentacion doc = det.getDocumentacion();
                if (doc != null) {
                    String estadoActualDisplay = doc.getEstadoActual() != null
                            ? doc.getEstadoActual().getNombreDisplay()
                            : "-";
                    filasDocs.add(new String[]{
                            String.valueOf(r.getNumero()),
                            String.valueOf(doc.getNumero()),
                            doc.mostrarTipoDocumentacion(),
                            doc.getAsunto(),
                            estadoActualDisplay
                    });
                }
            }
        }

        String fechaEnvio = bolsinSeleccionado.getFechaEnvio() != null
                ? bolsinSeleccionado.getFechaEnvio().format(FORMATO_FECHA)
                : "-";
        int totalDocs = Math.max(filasDocs.size(), infoRemitos.length);

        String[] resumenBolsin = new String[]{
                String.valueOf(bolsinSeleccionado.getNroPrecinto()),
                bolsinSeleccionado.getNombreCMOrigen(),
                fechaEnvio,
                remitosCsv.toString(),
                String.valueOf(remitos.size()),
                String.valueOf(totalDocs)
        };

        if (pantalla != null) {
            pantalla.mostrarDatosDeRemito(resumenBolsin, filasDocs.toArray(new String[0][]));
            pantalla.mostrarOpcionesRecepcion(buscarOpcionesRecepcion());
            pantalla.solicitarSeleccionOpcion();
        }
    }

    /**
     * Retorna las 4 opciones de recepción como matriz de String para la pantalla.
     * Fila: [numero, titulo, descripcion, impactoRemito, impactoDoc, requiereSeleccionDocs, habilitada]
     */
    public String[][] buscarOpcionesRecepcion() {
        asegurarEstadosCargados();
        boolean esDestinoCMC = comisionMedicaEmpleado != null && comisionMedicaEmpleado.esCentral();

        String remAcep = estadoRemitoRecibidoAceptado.getNombreDisplay();
        String remParc = estadoRemitoRecibidoAceptadoParcial.getNombreDisplay();
        String docAcep = estadoDocRecibidaAceptada.getNombreDisplay();
        String docNoRec = estadoDocNoRecibida.getNombreDisplay();
        String docRech = estadoDocRecibidaRechazada.getNombreDisplay();
        String docRedir = estadoDocParaRedirigir.getNombreDisplay();

        return new String[][]{
                {
                        String.valueOf(OPCION_CONTENIDO_IGUAL),
                        "El contenido es igual al registrado",
                        "Todos los documentos y remitos coinciden exactamente con lo enviado.",
                        remAcep,
                        docAcep + " (todas)",
                        "false",
                        "true"
                },
                {
                        String.valueOf(OPCION_FALTA_DOCUMENTACION),
                        "No se recibe toda la documentación",
                        "Uno o más documentos registrados no llegaron físicamente en el bolsín.",
                        remParc + " / " + remAcep,
                        docAcep + " (presentes) / " + docNoRec + " (faltantes)",
                        "true",
                        "true"
                },
                {
                        String.valueOf(OPCION_NO_CORRESPONDE_DESTINO),
                        "Existe documentación que no corresponde al destino",
                        "Se recibió documentación física dirigida erróneamente a esta Comisión Médica.",
                        remParc + " / " + remAcep,
                        docAcep + " (correctas) / " + docRech + " (erróneas)",
                        "true",
                        "true"
                },
                {
                        String.valueOf(OPCION_REDIRIGIR_OTRA_AREA),
                        "La documentación se debe redirigir a otra área",
                        "Disponible únicamente cuando la CM destino es la Comisión Médica Central (CMC).",
                        remAcep,
                        docRedir + " (pase interno)",
                        "false",
                        String.valueOf(esDestinoCMC)
                }
        };
    }

    /**
     * Calcula la previsualización de estado resultante y severidad visual por documento
     * para la pantalla de inspección (Paso 2), evitando lógica de negocio en el Boundary.
     *
     * @return Matriz [numeroDoc, estadoPrevistoDisplay, severidad(OK|WARN|ERROR|INFO)]
     */
    public String[][] previsualizarEstadosDocumentacion(int numeroOpcion, int[] numerosDocsNoConformes) {
        if (bolsinSeleccionado == null) {
            return new String[0][];
        }
        asegurarEstadosCargados();
        Set<Integer> noConformes = aSet(numerosDocsNoConformes);
        List<String[]> filas = new ArrayList<>();

        for (Remito r : bolsinSeleccionado.getRemitos()) {
            for (DetalleRemito det : r.getDetalles()) {
                Documentacion doc = det.getDocumentacion();
                if (doc == null) {
                    continue;
                }
                boolean esObservada = noConformes.contains(doc.getNumero());
                String[] prev = calcularPreviewDoc(numeroOpcion, esObservada);
                filas.add(new String[]{ String.valueOf(doc.getNumero()), prev[0], prev[1] });
            }
        }
        return filas.toArray(new String[0][]);
    }

    /**
     * Retorna [textoAviso, severidad] descriptivo para la opción seleccionada en Paso 2.
     */
    public String[] obtenerAvisoOpcion(int numeroOpcion) {
        asegurarEstadosCargados();
        return switch (numeroOpcion) {
            case OPCION_CONTENIDO_IGUAL -> new String[]{
                    "Todos los documentos pasarán a '" + estadoDocRecibidaAceptada.getNombreDisplay() + "'.",
                    SEV_OK
            };
            case OPCION_FALTA_DOCUMENTACION -> new String[]{
                    "Desmarque los documentos que NO llegaron físicamente (pasarán a '"
                            + estadoDocNoRecibida.getNombreDisplay() + "').",
                    SEV_WARN
            };
            case OPCION_NO_CORRESPONDE_DESTINO -> new String[]{
                    "Desmarque los documentos que NO corresponden a esta CM (pasarán a '"
                            + estadoDocRecibidaRechazada.getNombreDisplay() + "').",
                    SEV_ERROR
            };
            case OPCION_REDIRIGIR_OTRA_AREA -> new String[]{
                    "Todos los documentos pasarán a '" + estadoDocParaRedirigir.getNombreDisplay()
                            + "' hacia otra área interna.",
                    SEV_INFO
            };
            default -> new String[]{ "", SEV_OK };
        };
    }

    /**
     * DS-CU28 pasos 18-20: Valida y registra la opción elegida y los documentos observados,
     * preparando todos los datos previstos en primitivos para solicitar confirmación.
     */
    public void tomarSeleccionOpcion(int numeroOpcion, int[] numerosDocsNoConformes) {
        if (numeroOpcion < OPCION_CONTENIDO_IGUAL || numeroOpcion > OPCION_REDIRIGIR_OTRA_AREA) {
            if (pantalla != null) {
                pantalla.mostrarAlerta("Opción inválida", "Debe seleccionar una opción de recepción válida (1 a 4).");
            }
            this.opcionSeleccionada = 0;
            return;
        }

        if (numeroOpcion == OPCION_REDIRIGIR_OTRA_AREA) {
            boolean esDestinoCMC = comisionMedicaEmpleado != null && comisionMedicaEmpleado.esCentral();
            if (!esDestinoCMC) {
                System.out.println("[GESTOR] Advertencia A5: La opción 4 solo está permitida para Comisión Médica Central.");
                if (pantalla != null) {
                    pantalla.mostrarAlerta("Opción No Disponible",
                            "La opción 'Redirigir a otra área' solo aplica si la CM destino es CMC.");
                }
                this.opcionSeleccionada = 0;
                return;
            }
        }

        this.opcionSeleccionada = numeroOpcion;
        this.numerosDocsNoConformes = aSet(numerosDocsNoConformes);

        System.out.println("[GESTOR] Opción seleccionada: " + this.opcionSeleccionada
                + " | Documentos observados: " + this.numerosDocsNoConformes.size());

        if (pantalla != null && bolsinSeleccionado != null) {
            asegurarEstadosCargados();
            ComisionMedica cmOrigen = bolsinSeleccionado.getCMOrigen();
            String cmOrigenNombre = cmOrigen != null ? cmOrigen.getNombre() : "";
            String emailOrigen = cmOrigen != null ? cmOrigen.getEmail() : "";

            String[] cabecera = new String[]{
                    String.valueOf(bolsinSeleccionado.getNroPrecinto()),
                    cmOrigenNombre,
                    estadoRecibidoCMDestino.getNombreDisplay(),
                    "Opción " + numeroOpcion + ": " + obtenerTituloOpcion(numeroOpcion),
                    empleadoActual != null ? empleadoActual.getNombre() : "Encargado de Bolsines",
                    LocalDateTime.now().format(FORMATO_FECHA),
                    cmOrigenNombre + " <" + emailOrigen + ">",
                    SEV_OK
            };

            List<String[]> remitosPrev = new ArrayList<>();
            List<String[]> docsPrev = new ArrayList<>();
            for (Remito r : bolsinSeleccionado.getRemitos()) {
                boolean remitoTieneObservadas = false;
                for (DetalleRemito det : r.getDetalles()) {
                    Documentacion doc = det.getDocumentacion();
                    if (doc == null) {
                        continue;
                    }
                    boolean esObservada = this.numerosDocsNoConformes.contains(doc.getNumero());
                    if (esObservada && (numeroOpcion == OPCION_FALTA_DOCUMENTACION
                            || numeroOpcion == OPCION_NO_CORRESPONDE_DESTINO)) {
                        remitoTieneObservadas = true;
                    }
                    String[] prevDoc = calcularPreviewDoc(numeroOpcion, esObservada);
                    String estadoActualTxt = doc.getEstadoActual() != null
                            ? doc.getEstadoActual().getNombreDisplay() : "-";
                    docsPrev.add(new String[]{
                            String.valueOf(r.getNumero()),
                            String.valueOf(doc.getNumero()),
                            doc.mostrarTipoDocumentacion(),
                            doc.getAsunto(),
                            estadoActualTxt,
                            prevDoc[0],
                            prevDoc[1]
                    });
                }
                String estadoRemitoPrev = remitoTieneObservadas
                        ? estadoRemitoRecibidoAceptadoParcial.getNombreDisplay()
                        : estadoRemitoRecibidoAceptado.getNombreDisplay();
                String sevRemito = remitoTieneObservadas ? SEV_WARN : SEV_OK;
                remitosPrev.add(new String[]{ String.valueOf(r.getNumero()), estadoRemitoPrev, sevRemito });
            }

            pantalla.solicitarConfirmacionSeleccion(
                    cabecera,
                    remitosPrev.toArray(new String[0][]),
                    docsPrev.toArray(new String[0][])
            );
        }
    }

    /**
     * DS-CU28 pasos 21-22: Procesa la confirmación del Encargado de Bolsines.
     */
    public void tomarConfirmacionSeleccion(boolean confirmada) {
        if (!confirmada) {
            System.out.println("[GESTOR] Flujo alternativo A6: El Encargado de Bolsines no confirma la operación.");
            this.opcionSeleccionada = 0;
            this.numerosDocsNoConformes.clear();
            if (pantalla != null) {
                pantalla.mostrarAlerta("Operación No Confirmada",
                        "No se confirmó la recepción. El bolsín permanece en estado pendiente.");
                pantalla.cancelar();
            }
            return;
        }

        if (bolsinSeleccionado == null || opcionSeleccionada == 0) {
            return;
        }

        System.out.println("[GESTOR] Recepción confirmada. Iniciando actualización de estados...");

        obtenerEstado();
        getFechaHoraActual();
        actualizarEstadoBolsin();
        actualizarRemitoYDocumentacion();
        persistirRecepcion();
        notificarRecepcion();

        if (pantalla != null) {
            pantalla.informarCorrectoEnvioNotificacion(this.ultimaNotificacionDatos, this.ultimosRemitosActualizados);
        }

        finCU();
    }

    /**
     * DS-CU28 paso 23 y loops sec. 6: Recupera los objetos Estado desde el repositorio.
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
        bolsinSeleccionado.marcarRecibido(fechaHoraActual, estadoRecibidoCMDestino, empleadoActual);
    }

    /**
     * DS-CU28 pasos 39-40 y loop sec. 8: Delega la actualización de todos los remitos y documentaciones.
     */
    public void actualizarRemitoYDocumentacion() {
        if (bolsinSeleccionado == null) {
            return;
        }

        if (opcionSeleccionada == OPCION_CONTENIDO_IGUAL) {
            bolsinSeleccionado.actualizarRemitoYDocumentacion(
                    fechaHoraActual, estadoRemitoRecibidoAceptado, estadoDocRecibidaAceptada, empleadoActual);
        } else if (opcionSeleccionada == OPCION_FALTA_DOCUMENTACION) {
            bolsinSeleccionado.registrarFaltaDocumentacion(
                    fechaHoraActual, estadoRemitoRecibidoAceptado, estadoRemitoRecibidoAceptadoParcial,
                    estadoDocRecibidaAceptada, estadoDocNoRecibida, numerosDocsNoConformes, empleadoActual);
        } else if (opcionSeleccionada == OPCION_NO_CORRESPONDE_DESTINO) {
            bolsinSeleccionado.registrarDestinoErroneo(
                    fechaHoraActual, estadoRemitoRecibidoAceptado, estadoRemitoRecibidoAceptadoParcial,
                    estadoDocRecibidaAceptada, estadoDocRecibidaRechazada, numerosDocsNoConformes, empleadoActual);
        } else if (opcionSeleccionada == OPCION_REDIRIGIR_OTRA_AREA) {
            bolsinSeleccionado.marcarParaRedirigir(
                    fechaHoraActual, estadoRemitoRecibidoAceptado, estadoDocParaRedirigir, empleadoActual);
        }
    }

    /**
     * Persiste transaccionalmente en BD los cambios del bolsín, remitos, documentaciones e historiales.
     */
    public void persistirRecepcion() {
        if (bolsinSeleccionado == null) {
            return;
        }
        this.bolsinSeleccionado = repositorio.actualizarBolsin(this.bolsinSeleccionado, this.empleadoActual);
    }

    /**
     * DS-CU28 pasos 31-32: Incluye CU 29 (<<includes>>) delegando en GestorNotificarRecepcionBolsin
     * estrictamente post-commit después de persistirRecepcion().
     */
    public void notificarRecepcion() {
        if (bolsinSeleccionado == null) {
            return;
        }

        List<String[]> resumenRemitos = new ArrayList<>();
        List<String[]> remitosParaVista = new ArrayList<>();
        List<String[]> detalleDocs = new ArrayList<>();

        for (Remito r : bolsinSeleccionado.getRemitos()) {
            Estado estRem = r.getEstado();
            String estRemTxt = estRem != null ? estRem.getNombreDisplay() : "-";
            String sevRem = (estRem != null && estRem.esRecibidoYAceptadoParcial()) ? SEV_WARN : SEV_OK;
            resumenRemitos.add(new String[]{ String.valueOf(r.getNumero()), estRemTxt });
            remitosParaVista.add(new String[]{ String.valueOf(r.getNumero()), estRemTxt, sevRem });

            r.buscarDocumentacion();
            for (DetalleRemito d : r.getDetalles()) {
                Documentacion doc = d.getDocumentacion();
                if (doc != null) {
                    String estDocTxt = doc.getEstadoActual() != null
                            ? doc.getEstadoActual().getNombreDisplay() : "-";
                    detalleDocs.add(new String[]{
                            String.valueOf(r.getNumero()),
                            doc.mostrarTipoDocumentacion(),
                            doc.getAsunto(),
                            estDocTxt
                    });
                }
            }
        }

        ComisionMedica cmOrigen = bolsinSeleccionado.getCMOrigen();
        String cmOrigenNombre = cmOrigen != null ? cmOrigen.getNombre() : "";
        String emailOrigen = cmOrigen != null ? cmOrigen.getEmail() : "";
        String estadoBolsinTxt = estadoRecibidoCMDestino != null ? estadoRecibidoCMDestino.getNombreDisplay() : "-";
        String fechaFmt = fechaHoraActual != null ? fechaHoraActual.format(FORMATO_FECHA) : "";
        String respNombre = empleadoActual != null ? empleadoActual.getNombre() : "Encargado de Bolsines";

        this.ultimosRemitosActualizados = remitosParaVista.toArray(new String[0][]);
        this.ultimaNotificacionDatos = gestorNotificacion.notificarRecepcionBolsin(
                bolsinSeleccionado.getNroPrecinto(),
                cmOrigenNombre,
                emailOrigen,
                estadoBolsinTxt,
                resumenRemitos.toArray(new String[0][]),
                detalleDocs.toArray(new String[0][]),
                fechaFmt,
                respNombre
        );
    }

    /**
     * DS-CU28 paso 34: Finaliza el CU liberando únicamente las referencias transitorias en memoria.
     */
    public void finCU() {
        System.out.println("[GESTOR] Caso de uso finalizado con éxito.");
        this.bolsinSeleccionado = null;
        this.opcionSeleccionada = 0;
        this.numerosDocsNoConformes = new LinkedHashSet<>();
    }

    /**
     * Obs. 1: Cancela la operación actual en cualquier momento sin persistir ni notificar.
     */
    public void cancelarRecepcion() {
        System.out.println("[GESTOR] Operación cancelada por el usuario.");
        this.bolsinSeleccionado = null;
        this.opcionSeleccionada = 0;
        this.numerosDocsNoConformes = new LinkedHashSet<>();
        buscarBolsinEstadoEnviado();
        notificarListadoBolsinesAPantalla(this.bolsinesEnviados);
    }

    // ── Getters públicos estrictamente primitivos (0 entidades expuestas) ──

    public int[] getPrecintosBolsinesEnviados() {
        return extraerPrecintos(this.bolsinesEnviados);
    }

    public int getPrecintoBolsinSeleccionado() {
        return this.bolsinSeleccionado != null ? this.bolsinSeleccionado.getNroPrecinto() : -1;
    }

    public int getOpcionSeleccionadaNumero() {
        return this.opcionSeleccionada;
    }

    public int[] getNumerosDocsNoConformes() {
        int[] arr = new int[this.numerosDocsNoConformes.size()];
        int i = 0;
        for (Integer n : this.numerosDocsNoConformes) {
            arr[i++] = n;
        }
        return arr;
    }

    public String[] getUltimaNotificacionDatos() {
        return this.ultimaNotificacionDatos;
    }

    public String[][] getUltimosRemitosActualizados() {
        return this.ultimosRemitosActualizados;
    }

    // ── Helpers privados internos ──

    private void notificarListadoBolsinesAPantalla(List<Bolsin> bolsines) {
        if (pantalla == null) {
            return;
        }
        int n = bolsines.size();
        int[] precintos = new int[n];
        String[] cmsOrigen = new String[n];
        String[][] filasTabla = new String[n][6];

        for (int i = 0; i < n; i++) {
            Bolsin b = bolsines.get(i);
            precintos[i] = b.getNroPrecinto();
            cmsOrigen[i] = b.getNombreCMOrigen();

            StringBuilder remitosCsv = new StringBuilder();
            List<Remito> remitos = b.getRemitos();
            for (int j = 0; j < remitos.size(); j++) {
                if (j > 0) {
                    remitosCsv.append(", ");
                }
                remitosCsv.append(remitos.get(j).getNumero());
            }

            filasTabla[i][0] = String.valueOf(b.getNroPrecinto());
            filasTabla[i][1] = b.getNombreCMOrigen();
            filasTabla[i][2] = b.getCmDestino() != null ? b.getCmDestino().getNombre() : "-";
            filasTabla[i][3] = remitos.isEmpty() ? "S/R" : remitosCsv.toString();
            filasTabla[i][4] = b.getFechaEnvio() != null ? b.getFechaEnvio().format(FORMATO_FECHA) : "-";
            filasTabla[i][5] = b.getEstadoActual() != null ? b.getEstadoActual().getNombreDisplay() : "-";
        }

        List<ComisionMedica> catalogoCMs = repositorio.obtenerComisionesMedicas();
        Set<String> nombresCMOrigen = new LinkedHashSet<>();
        for (Bolsin b : bolsines) {
            if (!b.getNombreCMOrigen().isEmpty()) {
                nombresCMOrigen.add(b.getNombreCMOrigen());
            }
        }
        if (catalogoCMs != null) {
            for (ComisionMedica cm : catalogoCMs) {
                if (cm.getNombre() != null && !cm.getNombre().isEmpty()) {
                    nombresCMOrigen.add(cm.getNombre());
                }
            }
        }

        pantalla.mostrarNumeroPrecinto(precintos);
        pantalla.mostrarCMOrigen(cmsOrigen);
        pantalla.mostrarInformacionAdicionalBolsines(
                filasTabla,
                nombresCMOrigen.toArray(new String[0]),
                this.todosBolsines != null ? this.todosBolsines.size() : n
        );
        pantalla.solicitarSeleccionBolsin();
    }

    private void asegurarEstadosCargados() {
        if (estadoRecibidoCMDestino == null || estadoRemitoRecibidoAceptado == null
                || estadoDocRecibidaAceptada == null || estadoDocNoRecibida == null
                || estadoDocRecibidaRechazada == null || estadoDocParaRedirigir == null) {
            obtenerEstado();
        }
    }

    private String[] calcularPreviewDoc(int numeroOpcion, boolean esObservada) {
        return switch (numeroOpcion) {
            case OPCION_CONTENIDO_IGUAL -> new String[]{ estadoDocRecibidaAceptada.getNombreDisplay(), SEV_OK };
            case OPCION_FALTA_DOCUMENTACION -> esObservada
                    ? new String[]{ estadoDocNoRecibida.getNombreDisplay(), SEV_WARN }
                    : new String[]{ estadoDocRecibidaAceptada.getNombreDisplay(), SEV_OK };
            case OPCION_NO_CORRESPONDE_DESTINO -> esObservada
                    ? new String[]{ estadoDocRecibidaRechazada.getNombreDisplay(), SEV_ERROR }
                    : new String[]{ estadoDocRecibidaAceptada.getNombreDisplay(), SEV_OK };
            case OPCION_REDIRIGIR_OTRA_AREA -> new String[]{ estadoDocParaRedirigir.getNombreDisplay(), SEV_INFO };
            default -> new String[]{ "-", SEV_OK };
        };
    }

    private String obtenerTituloOpcion(int numeroOpcion) {
        return switch (numeroOpcion) {
            case OPCION_CONTENIDO_IGUAL -> "El contenido es igual al registrado";
            case OPCION_FALTA_DOCUMENTACION -> "No se recibe toda la documentación";
            case OPCION_NO_CORRESPONDE_DESTINO -> "Existe documentación que no corresponde al destino";
            case OPCION_REDIRIGIR_OTRA_AREA -> "La documentación se debe redirigir a otra área";
            default -> "";
        };
    }

    private static Set<Integer> aSet(int[] arr) {
        Set<Integer> set = new LinkedHashSet<>();
        if (arr != null) {
            for (int n : arr) {
                set.add(n);
            }
        }
        return set;
    }

    private static int[] extraerPrecintos(List<Bolsin> lista) {
        int[] precintos = new int[lista.size()];
        for (int i = 0; i < lista.size(); i++) {
            precintos[i] = lista.get(i).getNroPrecinto();
        }
        return precintos;
    }
}
