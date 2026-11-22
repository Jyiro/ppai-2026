package com.ppai.boundary.cu28;

import com.ppai.controller.cu28.GestorRecepcionBolsin;
import com.ppai.domain.model.cu28.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Boundary PantallaRecepcionBolsin — CU 28: Registrar Recepción de Bolsín.
 * Implementación 100% Orientada a Objetos en JavaFX.
 * Mantiene correspondencia y trazabilidad directa con los métodos del Diagrama de Secuencia.
 */
public class PantallaRecepcionBolsin extends BorderPane {

    private final GestorRecepcionBolsin gestor;
    private boolean habilitada;

    // ── Componentes de Encabezado y Navegación ──
    private Label lblTitulo;
    private Label lblSubtitulo;
    private Label lblUsuarioActivo;
    private Label lblCmActiva;
    private ComboBox<String> cmbEscenarioMock;
    private Button btnCancelarGlobal;

    // ── Stepper ──
    private HBox stepperBox;
    private Label[] stepCircles;
    private Label[] stepLabels;

    // ── Contenedor Central Dinámico ──
    private ScrollPane mainScrollPane;
    private StackPane contentArea;

    // ── Paso 1: Bolsines por Recibir ──
    private VBox viewPaso1;
    private Label lblCardSub;
    private TextField txtFiltroPrecinto;
    private ComboBox<ComisionMedica> cmbFiltroCMOrigen;
    private TableView<Bolsin> tblBolsines;
    private ObservableList<Bolsin> listaBolsinesData;
    private Button btnSeleccionarBolsin;
    private VBox boxAlertaPaso1;
    private Label lblMensajeAlertaPaso1;
    private boolean actualizandoFiltros;

    // ── Paso 2: Remito, Documentación y Opciones ──
    private VBox viewPaso2;
    private Label lblDetallePrecinto;
    private Label lblDetalleCMOrigen;
    private Label lblDetalleFechaEnvio;
    private Label lblDetalleNumeroRemito;
    private Label lblDetalleCantDocs;
    private TableView<DocumentacionRow> tblDocumentacion;
    private ObservableList<DocumentacionRow> listaDocsData;
    private VBox boxOpcionesCards;
    private ToggleGroup grupoOpciones;
    private List<RadioButton> radioOpcionesList;
    private List<OpcionRecepcion> listaOpcionesRecepcion;
    private Button btnContinuarConfirmacion;
    private Label lblAvisoDocsNoConformes;

    // ── Paso 3: Confirmación ──
    private VBox viewPaso3;
    private Label lblConfPrecinto;
    private Label lblConfCMOrigen;
    private Label lblConfOpcionElegida;
    private Label lblConfRemito;
    private Label lblConfEstadoRemito;
    private VBox boxConfDocsList;
    private Label lblConfEmpleado;
    private Label lblConfFechaHora;
    private Label lblConfDestinatarioCU29;
    private Button btnConfirmarRecepcion;

    // ── Paso 4: Resultado y CU 29 ──
    private VBox viewPaso4;
    private Label lblResultadoTitulo;
    private Label lblResultadoSubtitulo;
    private Label lblResBolsin;
    private Label lblResRemito;
    private Label lblResEstadoRemito;
    private Label lblResEmpleadoFecha;
    private Label lblEmailPara;
    private Label lblEmailAsunto;
    private Label lblEmailFecha;
    private TextArea txtCuerpoCorreoCU29;
    private Button btnNuevaRecepcion;

    // ── Barra Inferior (Footer) ──
    private Label lblStatusMensaje;
    private Label lblStatusTimestamp;

    // ── Estado Transitorio de la Vista ──
    private Bolsin bolsinSeleccionadoEnTabla;
    private OpcionRecepcion opcionSeleccionadaActual;
    private String ultimoBolsinRegistradoInfo;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public PantallaRecepcionBolsin(GestorRecepcionBolsin gestor) {
        this.gestor = gestor;
        this.habilitada = false;

        construirInterfaz();
    }

    // =========================================================================
    // MÉTODOS DEL DIAGRAMA DE SECUENCIA (Trazabilidad DSI)
    // =========================================================================

    /**
     * DS-CU28 pasos 1-3: El Encargado de Bolsines inicia el CU; la pantalla se habilita
     * y delega el flujo de inicialización en el gestor.
     */
    public void tomarOpcionRegistrarRecepcionBolsin() {
        habilitar();
        gestor.tomarOpcionRegistrarRecepcionBolsin();
    }

    /**
     * DS-CU28 paso 2: Habilita la interfaz gráfica para operar.
     */
    public void habilitar() {
        this.habilitada = true;
        this.bolsinSeleccionadoEnTabla = null;
        this.opcionSeleccionadaActual = null;
        irAPaso(1);
        actualizarStatus("Pantalla habilitada. Esperando selección de bolsín enviado.");
    }

    /**
     * DS-CU28 paso 8: Muestra la Comisión Médica activa y el usuario autenticado en el encabezado.
     */
    public void mostrarCM(ComisionMedica cm) {
        if (gestor != null && gestor.getSesionActual() != null && gestor.getSesionActual().getUsuario() != null) {
            Usuario usuario = gestor.getSesionActual().getUsuario();
            String nombreEmpleado = gestor.getEmpleadoActual() != null ? gestor.getEmpleadoActual().getNombre() : "";
            String textoUsuario = "Usuario: " + usuario.getUsername()
                    + (nombreEmpleado != null && !nombreEmpleado.isEmpty() ? " (" + nombreEmpleado + ")" : "");
            if (lblUsuarioActivo != null) {
                lblUsuarioActivo.setText(textoUsuario);
                lblUsuarioActivo.setTooltip(new Tooltip(textoUsuario));
            }
        }
        if (cm != null) {
            String textoCm = "CM Destino: " + cm.getNombre();
            lblCmActiva.setText(textoCm);
            lblCmActiva.setTooltip(new Tooltip(textoCm));
            actualizarStatus("Comisión Médica activa: " + cm.getNombre());
        }
    }

    /**
     * DS-CU28 sec. 3: Muestra la lista de bolsines en estado ENVIADO hacia la CM activa.
     */
    public void mostrarBolsines(List<Bolsin> bolsines) {
        this.bolsinSeleccionadoEnTabla = null;
        this.opcionSeleccionadaActual = null;
        this.listaBolsinesData.setAll(bolsines);
        tblBolsines.refresh();

        if (lblCardSub != null && gestor != null) {
            String cmNombre = gestor.getComisionMedicaEmpleado() != null
                    ? gestor.getComisionMedicaEmpleado().getNombre()
                    : "CM activa";
            int totalSistema = gestor.getTodosBolsines() != null ? gestor.getTodosBolsines().size() : bolsines.size();
            lblCardSub.setText("IteradorBolsin filtró " + bolsines.size() + " bolsín(es) en estado ENVIADO dirigidos a ["
                    + cmNombre + "] (sobre " + totalSistema + " bolsines totales cargados en el sistema).");
        }

        // Actualizar opciones de CM Origen para el filtro (incluye catálogo completo para poder probar A2)
        List<ComisionMedica> origenes = new ArrayList<>();
        for (Bolsin b : bolsines) {
            agregarSiNoExisteCM(origenes, b.getCMOrigen());
        }
        if (gestor != null && gestor.getComisionesMedicasFiltro() != null) {
            for (ComisionMedica cmCatalogo : gestor.getComisionesMedicasFiltro()) {
                agregarSiNoExisteCM(origenes, cmCatalogo);
            }
        }

        actualizandoFiltros = true;
        try {
            ObservableList<ComisionMedica> origenesList = FXCollections.observableArrayList(origenes);
            cmbFiltroCMOrigen.setItems(origenesList);
        } finally {
            actualizandoFiltros = false;
        }

        ocultarAlertaPaso1();
        actualizarStatus("Bolsines en estado ENVIADO disponibles: " + bolsines.size());
    }

    private void agregarSiNoExisteCM(List<ComisionMedica> lista, ComisionMedica candidata) {
        if (candidata == null) {
            return;
        }
        for (ComisionMedica existente : lista) {
            if (existente.getId() != null && existente.getId().equals(candidata.getId())) {
                return;
            }
        }
        lista.add(candidata);
    }

    /**
     * DS-CU28 sec. 3: Métodos de compatibilidad individual con el diagrama de secuencia.
     */
    public void mostrarNumeroPrecinto() {
        // Invocado fuera del loop según corrección docente
    }

    public void mostrarNumeroPrecinto(String numeroPrecinto) {
        // Integrado en la tabla de bolsines
    }

    public void mostrarCMOrigen() {
        // Invocado fuera del loop según corrección docente
    }

    public void mostrarCMOrigen(ComisionMedica cmOrigen) {
        // Integrado en la tabla de bolsines
    }

    /**
     * DS-CU28 paso 10: Solicita al Encargado de Bolsines la selección de un bolsín.
     */
    public void solicitarSeleccionBolsin() {
        btnSeleccionarBolsin.setDisable(tblBolsines.getSelectionModel().getSelectedItem() == null);
    }

    /**
     * DS-CU28 paso 11-12: Captura el bolsín seleccionado por el usuario y notifica al gestor.
     */
    public void tomarSeleccionBolsin(Bolsin bolsin) {
        if (bolsin != null) {
            this.bolsinSeleccionadoEnTabla = bolsin;
            gestor.tomarSeleccionBolsin(bolsin);
        }
    }

    public String tomarSeleccionBolsin() {
        return bolsinSeleccionadoEnTabla != null ? bolsinSeleccionadoEnTabla.getNumeroPrecinto() : "";
    }

    public void tomarSeleccionOpcion(String seleccion) {
        gestor.tomarSeleccionOpcion(seleccion);
    }

    /**
     * DS-CU28 paso 15: Muestra los datos del remito y el detalle de documentaciones asociadas.
     */
    public void mostrarDatosDeRemito(Remito remito) {
        Bolsin bolsin = (gestor != null && gestor.getBolsinSeleccionado() != null)
                ? gestor.getBolsinSeleccionado()
                : this.bolsinSeleccionadoEnTabla;
        if (bolsin != null) {
            this.bolsinSeleccionadoEnTabla = bolsin;
            lblDetallePrecinto.setText(bolsin.getNumeroPrecinto());
            lblDetalleCMOrigen.setText("CM Origen: " + bolsin.getCMOrigen().getNombre());
            lblDetalleFechaEnvio.setText("Fecha de Envío: " + bolsin.getFechaEnvio().format(FORMATO_FECHA));
        }
        lblDetalleNumeroRemito.setText(remito.getNumero());
        lblDetalleCantDocs.setText(String.valueOf(remito.getDetalles().size()));

        // Cargar filas de documentación con refresco reactivo al tildar/destildar ¿Conforme?
        List<DocumentacionRow> rows = new ArrayList<>();
        for (DetalleRemito d : remito.buscarDocumentacion()) {
            DocumentacionRow row = new DocumentacionRow(d.getDocumentacion());
            row.seleccionadaProperty().addListener((obs, oldVal, newVal) -> tblDocumentacion.refresh());
            rows.add(row);
        }
        listaDocsData.setAll(rows);
        tblDocumentacion.refresh();

        irAPaso(2);
        actualizarStatus("Detalle del remito " + remito.getNumero() + " desplegado.");
    }

    public void mostrarDatosDeRemito(String datosRemito) {
        // Sobrecarga de compatibilidad
    }

    /**
     * DS-CU28 paso 16: Muestra en pantalla las 4 opciones de recepción.
     */
    public void mostrarOpcionesRecepcion(List<OpcionRecepcion> opciones) {
        this.listaOpcionesRecepcion = opciones;
        construirCardsOpciones(opciones);
    }

    public void mostrarOpcionesRecepcion() {
        mostrarOpcionesRecepcion(gestor.buscarOpcionesRecepcion());
    }

    /**
     * DS-CU28 paso 17: Solicita al Encargado de Bolsines la selección de una opción de recepción.
     */
    public void solicitarSeleccionOpcion() {
        btnContinuarConfirmacion.setDisable(opcionSeleccionadaActual == null);
    }

    public String tomarSeleccionOpcionRecepcion() {
        return opcionSeleccionadaActual != null ? String.valueOf(opcionSeleccionadaActual.getNumero()) : "1";
    }

    /**
     * DS-CU28 paso 18-19: Captura la opción seleccionada y los documentos observados, y los envía al gestor.
     */
    public void tomarSeleccionOpcion(OpcionRecepcion opcion) {
        this.opcionSeleccionadaActual = opcion;

        // Si la opción requiere marcar documentos (A3 o A4), extraer los no conformes
        List<Documentacion> docsNoConformes = new ArrayList<>();
        if (opcion.isRequiereSeleccionDocs()) {
            for (DocumentacionRow row : listaDocsData) {
                if (!row.isSeleccionada()) {
                    docsNoConformes.add(row.getDocumentacion());
                }
            }
        }

        gestor.tomarSeleccionOpcion(opcion, docsNoConformes);
    }

    /**
     * DS-CU28 paso 20: Muestra los datos consolidados y solicita la confirmación de la recepción.
     */
    public void solicitarConfirmacionSeleccion(Bolsin bolsin, Remito remito, OpcionRecepcion opcion, List<Documentacion> docsNoConformes) {
        lblConfPrecinto.setText(bolsin.getNumeroPrecinto());
        lblConfCMOrigen.setText(bolsin.getCMOrigen().getNombre());
        if (lblConfOpcionElegida != null && opcion != null) {
            lblConfOpcionElegida.setText("Opción " + opcion.getNumero() + ": " + opcion.getTitulo());
        }
        lblConfRemito.setText(remito.getNumero());
        lblConfEstadoRemito.setText("Nuevo Estado: " + opcion.getImpactoRemito());
        lblConfEstadoRemito.getStyleClass().removeAll("badge-recibido", "badge-parcial");
        if (opcion.esFaltaDocumentacion() || opcion.esNoCorrespondeDestino()) {
            lblConfEstadoRemito.getStyleClass().add("badge-parcial");
        } else {
            lblConfEstadoRemito.getStyleClass().add("badge-recibido");
        }

        lblConfEmpleado.setText(gestor.getEmpleadoActual() != null ? gestor.getEmpleadoActual().getNombre() : "Juan Pérez");
        lblConfFechaHora.setText(java.time.LocalDateTime.now().format(FORMATO_FECHA));

        String cmOrigenNombre = bolsin.getCMOrigen().getNombre();
        String emailOrigen = "despacho." + cmOrigenNombre.toLowerCase().replaceAll("[^a-z0-9]", "") + "@srt.gob.ar";
        lblConfDestinatarioCU29.setText(cmOrigenNombre + " <" + emailOrigen + ">");

        // Desglose de documentos en confirmación
        boxConfDocsList.getChildren().clear();
        for (DocumentacionRow row : listaDocsData) {
            HBox itemBox = new HBox(10);
            itemBox.setAlignment(Pos.CENTER_LEFT);

            Label lblTipo = new Label("#" + row.getId() + " [" + row.getTipo() + "]");
            lblTipo.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");

            Label lblAsunto = new Label(row.getAsunto());
            lblAsunto.setStyle("-fx-text-fill: #1e293b;");
            HBox.setHgrow(lblAsunto, Priority.ALWAYS);

            Label lblEstadoPrevio = new Label(row.getEstadoActual() + "  ➔");
            lblEstadoPrevio.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

            Label lblNuevoEstado = new Label(row.getEstadoResultante(opcion));
            lblNuevoEstado.getStyleClass().add("badge");

            if (row.isSeleccionada() || opcion.esContenidoIgual()) {
                lblNuevoEstado.getStyleClass().add("badge-recibido");
            } else if (opcion.esFaltaDocumentacion()) {
                lblNuevoEstado.getStyleClass().add("badge-parcial");
            } else if (opcion.esNoCorrespondeDestino()) {
                lblNuevoEstado.getStyleClass().add("badge-rechazado");
            } else if (opcion.esRedirigirAOtraArea()) {
                lblNuevoEstado.getStyleClass().add("badge-redirigir");
            }

            itemBox.getChildren().addAll(lblTipo, lblAsunto, lblEstadoPrevio, lblNuevoEstado);
            boxConfDocsList.getChildren().add(itemBox);
        }

        irAPaso(3);
        actualizarStatus("Esperando confirmación final de la operación.");
    }

    public void solicitarConfirmacionSeleccion() {
        // Sobrecarga de compatibilidad
    }

    /**
     * DS-CU28 paso 21-22: Captura la confirmación del Encargado de Bolsines.
     */
    public void tomarConfirmacionSeleccion(boolean confirmada) {
        gestor.tomarConfirmacionSeleccion(confirmada);
    }

    public String tomarConfirmacionSeleccion() {
        return "S";
    }

    /**
     * DS-CU28 paso 33: Informa el éxito de la recepción y la notificación emitida (CU 29).
     */
    public void informarCorrectoEnvioNotificacion(NotificacionRecepcionBolsin notificacion) {
        if (notificacion != null) {
            String fechaTexto = notificacion.getFechaHoraEnvio() != null
                    ? notificacion.getFechaHoraEnvio().format(FORMATO_FECHA)
                    : "";

            lblResultadoTitulo.setText("¡Recepción Registrada con Éxito! (CU 28)");
            lblResultadoSubtitulo.setText("El bolsín " + notificacion.getNumeroPrecinto()
                    + " pasó a 'Recibido en CM Destino' y se envió la notificación electrónica a "
                    + notificacion.getDestinatarioCM() + " (<<includes>> CU 29).");

            if (lblResBolsin != null) {
                lblResBolsin.setText(notificacion.getNumeroPrecinto());
            }
            if (lblResRemito != null) {
                lblResRemito.setText(notificacion.getNumeroRemito());
            }
            if (lblResEstadoRemito != null) {
                lblResEstadoRemito.setText(notificacion.getEstadoRemito());
                lblResEstadoRemito.getStyleClass().removeAll("badge-recibido", "badge-parcial");
                if (notificacion.getEstadoRemito() != null
                        && notificacion.getEstadoRemito().toLowerCase().contains("parcial")) {
                    lblResEstadoRemito.getStyleClass().add("badge-parcial");
                } else {
                    lblResEstadoRemito.getStyleClass().add("badge-recibido");
                }
            }
            if (lblResEmpleadoFecha != null) {
                lblResEmpleadoFecha.setText(notificacion.getEmpleadoResponsable() + " • " + fechaTexto);
            }
            if (lblEmailPara != null) {
                lblEmailPara.setText(notificacion.getDestinatarioCM() + " <" + notificacion.getEmailDestino() + ">");
            }
            if (lblEmailAsunto != null) {
                lblEmailAsunto.setText("[Notificación de Recepción] Bolsín Precinto N° " + notificacion.getNumeroPrecinto());
            }
            if (lblEmailFecha != null) {
                lblEmailFecha.setText(fechaTexto);
            }
            txtCuerpoCorreoCU29.setText(notificacion.getCuerpoCorreo());
            this.ultimoBolsinRegistradoInfo = "Bolsín " + notificacion.getNumeroPrecinto()
                    + " (Remito " + notificacion.getNumeroRemito()
                    + ") registrado en estado 'Recibido en CM Destino'; ya no figura como pendiente ENVIADO.";
        }

        irAPaso(4);
        actualizarStatus("Recepción registrada correctamente. Notificación CU 29 despachada.");

        if (notificacion != null
                && javafx.application.Platform.isFxApplicationThread()
                && this.getScene() != null
                && this.getScene().getWindow() != null
                && this.getScene().getWindow().isShowing()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(this.getScene().getWindow());
            alert.setTitle("Recepción Registrada — CU 28");
            alert.setHeaderText("✔ ¡Bolsín " + notificacion.getNumeroPrecinto() + " registrado con éxito!");
            alert.setContentText(
                    "Estado del Bolsín: Recibido en CM Destino\n"
                    + "Estado del Remito (" + notificacion.getNumeroRemito() + "): " + notificacion.getEstadoRemito() + "\n"
                    + "Notificación CU 29 enviada a: " + notificacion.getDestinatarioCM()
                    + " (" + notificacion.getEmailDestino() + ")");
            alert.showAndWait();
        }
    }

    public void informarCorrectoEnvioNotificacion() {
        informarCorrectoEnvioNotificacion(gestor.getUltimaNotificacionEnviada());
    }

    /**
     * Obs. 1: El Encargado de Bolsines puede cancelar el CU en cualquier momento.
     */
    public void cancelar() {
        this.bolsinSeleccionadoEnTabla = null;
        this.opcionSeleccionadaActual = null;
        this.ultimoBolsinRegistradoInfo = null;
        gestor.cancelarRecepcion();
        irAPaso(1);
        actualizarStatus("Operación cancelada. Regresando a la lista de bolsines.");
    }

    /**
     * Muestra una alerta informativa o de flujo alternativo (A1, A2, A6).
     */
    public void mostrarAlerta(String titulo, String mensaje) {
        boxAlertaPaso1.getStyleClass().remove("alert-box-success");
        if (!boxAlertaPaso1.getStyleClass().contains("alert-box-warning")) {
            boxAlertaPaso1.getStyleClass().add("alert-box-warning");
        }
        boxAlertaPaso1.setVisible(true);
        boxAlertaPaso1.setManaged(true);
        lblMensajeAlertaPaso1.setText("⚠ " + titulo + ": " + mensaje);
        actualizarStatus("Aviso: " + mensaje);
    }

    private void mostrarAvisoExitoPaso1(String mensaje) {
        boxAlertaPaso1.getStyleClass().remove("alert-box-warning");
        if (!boxAlertaPaso1.getStyleClass().contains("alert-box-success")) {
            boxAlertaPaso1.getStyleClass().add("alert-box-success");
        }
        boxAlertaPaso1.setVisible(true);
        boxAlertaPaso1.setManaged(true);
        lblMensajeAlertaPaso1.setText("✔ " + mensaje);
        actualizarStatus(mensaje);
    }

    private void ocultarAlertaPaso1() {
        boxAlertaPaso1.setVisible(false);
        boxAlertaPaso1.setManaged(false);
    }

    // =========================================================================
    // CONSTRUCCIÓN DE LA INTERFAZ GRÁFICA (JavaFX 100% Objetos)
    // =========================================================================

    private void construirInterfaz() {
        // Hoja de estilos CSS
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            this.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("[PANTALLA] No se pudo cargar styles.css: " + e.getMessage());
        }

        // Estructura general
        this.setTop(crearTopContainer());

        this.contentArea = new StackPane();
        this.contentArea.setPadding(new Insets(12, 18, 14, 18));

        this.viewPaso1 = crearVistaPaso1();
        this.viewPaso2 = crearVistaPaso2();
        this.viewPaso3 = crearVistaPaso3();
        this.viewPaso4 = crearVistaPaso4();

        this.contentArea.getChildren().addAll(viewPaso1, viewPaso2, viewPaso3, viewPaso4);

        this.mainScrollPane = new ScrollPane(this.contentArea);
        this.mainScrollPane.setFitToWidth(true);
        this.mainScrollPane.setFitToHeight(false);
        this.mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        this.mainScrollPane.getStyleClass().add("main-scroll-pane");

        this.setCenter(this.mainScrollPane);

        this.setBottom(crearFooterBar());

        // Iniciar en Paso 1
        irAPaso(1);
    }

    private VBox crearTopContainer() {
        VBox topBox = new VBox();

        // 1. Header Bar
        HBox headerBar = new HBox(10);
        headerBar.getStyleClass().add("header-bar");
        headerBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(1);
        titleBox.setMinWidth(210);
        this.lblTitulo = new Label("Recepción de Bolsines — CU 28");
        this.lblTitulo.getStyleClass().add("header-title");

        this.lblSubtitulo = new Label("Sistema de Seguimiento y Trazabilidad de Remitos y Documentación Física");
        this.lblSubtitulo.getStyleClass().add("header-subtitle");
        titleBox.getChildren().addAll(lblTitulo, lblSubtitulo);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Chips informativos
        Usuario usrInicial = gestor.getSesionActual().getUsuario();
        String nombreEmpInicial = usrInicial.getEmpleado() != null ? " (" + usrInicial.getEmpleado().getNombre() + ")" : "";
        this.lblUsuarioActivo = new Label("Usuario: " + usrInicial.getUsername() + nombreEmpInicial);
        this.lblUsuarioActivo.getStyleClass().add("session-chip");
        this.lblUsuarioActivo.setMaxWidth(240);
        this.lblUsuarioActivo.setTooltip(new Tooltip(this.lblUsuarioActivo.getText()));

        this.lblCmActiva = new Label("CM Destino: " + gestor.getSesionActual().getUsuario().getCMEmpleado().getNombre());
        this.lblCmActiva.getStyleClass().add("session-chip");
        this.lblCmActiva.setMaxWidth(240);
        this.lblCmActiva.setTooltip(new Tooltip(this.lblCmActiva.getText()));

        // Selector de Mock / Escenario para evaluación docente
        HBox mockControlBox = new HBox(6);
        mockControlBox.setAlignment(Pos.CENTER);
        mockControlBox.setMinWidth(Region.USE_PREF_SIZE);
        Label lblMockTag = new Label("Mock:");
        lblMockTag.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 12px;");
        lblMockTag.setMinWidth(Region.USE_PREF_SIZE);

        this.cmbEscenarioMock = new ComboBox<>();
        this.cmbEscenarioMock.getItems().addAll("Escenario 1: Comisión Médica Central (CMC)", "Escenario 2: CMJ Regional (Rosario)");
        this.cmbEscenarioMock.setValue("Escenario 1: Comisión Médica Central (CMC)");
        this.cmbEscenarioMock.setPrefWidth(215);
        this.cmbEscenarioMock.setOnAction(e -> {
            int esc = cmbEscenarioMock.getSelectionModel().getSelectedIndex() + 1;
            actualizandoFiltros = true;
            try {
                if (txtFiltroPrecinto != null) {
                    txtFiltroPrecinto.clear();
                }
                if (cmbFiltroCMOrigen != null) {
                    cmbFiltroCMOrigen.setValue(null);
                }
            } finally {
                actualizandoFiltros = false;
            }
            gestor.cargarEscenario(esc);
            irAPaso(1);
        });

        mockControlBox.getChildren().addAll(lblMockTag, cmbEscenarioMock);

        this.btnCancelarGlobal = new Button("✕ Cancelar (Obs. 1)");
        this.btnCancelarGlobal.getStyleClass().addAll("btn-danger");
        this.btnCancelarGlobal.setMinWidth(Region.USE_PREF_SIZE);
        this.btnCancelarGlobal.setOnAction(e -> cancelar());

        headerBar.getChildren().addAll(titleBox, lblUsuarioActivo, lblCmActiva, mockControlBox, btnCancelarGlobal);

        // 2. Stepper Bar
        this.stepperBox = crearStepper();

        topBox.getChildren().addAll(headerBar, stepperBox);
        return topBox;
    }

    private HBox crearStepper() {
        HBox box = new HBox(12);
        box.getStyleClass().add("stepper-container");

        String[] titulos = {
                "1. Bolsines Enviados",
                "2. Remito y Documentación",
                "3. Confirmación de Recepción",
                "4. Resultado y Notificación CU 29"
        };

        this.stepCircles = new Label[4];
        this.stepLabels = new Label[4];

        for (int i = 0; i < 4; i++) {
            HBox stepItem = new HBox(8);
            stepItem.setAlignment(Pos.CENTER_LEFT);

            stepCircles[i] = new Label(String.valueOf(i + 1));
            stepCircles[i].getStyleClass().add("step-circle");

            stepLabels[i] = new Label(titulos[i]);
            stepLabels[i].getStyleClass().add("step-label");

            stepItem.getChildren().addAll(stepCircles[i], stepLabels[i]);
            box.getChildren().add(stepItem);

            if (i < 3) {
                Region separator = new Region();
                separator.getStyleClass().add("step-separator");
                box.getChildren().add(separator);
            }
        }

        return box;
    }

    private void irAPaso(int paso) {
        viewPaso1.setVisible(paso == 1);
        viewPaso1.setManaged(paso == 1);

        viewPaso2.setVisible(paso == 2);
        viewPaso2.setManaged(paso == 2);

        viewPaso3.setVisible(paso == 3);
        viewPaso3.setManaged(paso == 3);

        viewPaso4.setVisible(paso == 4);
        viewPaso4.setManaged(paso == 4);

        if (mainScrollPane != null) {
            mainScrollPane.setVvalue(0.0);
        }

        // Actualizar Stepper visual
        for (int i = 0; i < 4; i++) {
            stepCircles[i].getStyleClass().removeAll("step-circle-active", "step-circle-completed");
            stepLabels[i].getStyleClass().remove("step-label-active");

            if (i + 1 < paso) {
                stepCircles[i].getStyleClass().add("step-circle-completed");
                stepCircles[i].setText("✓");
            } else if (i + 1 == paso) {
                stepCircles[i].getStyleClass().add("step-circle-active");
                stepCircles[i].setText(String.valueOf(i + 1));
                stepLabels[i].getStyleClass().add("step-label-active");
            } else {
                stepCircles[i].setText(String.valueOf(i + 1));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VISTA 1: BOLSINES POR RECIBIR
    // ─────────────────────────────────────────────────────────────────────────

    private VBox crearVistaPaso1() {
        VBox root = new VBox(16);
        root.setAlignment(Pos.TOP_CENTER);

        VBox card = new VBox(16);
        card.getStyleClass().add("card-panel");
        VBox.setVgrow(card, Priority.ALWAYS);

        // Header de la tarjeta
        HBox cardHeader = new HBox(12);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        VBox cardTitles = new VBox(2);
        Label lblCardTitle = new Label("Bolsines en Estado ENVIADO");
        lblCardTitle.getStyleClass().add("card-title");
        this.lblCardSub = new Label("Seleccione del listado el bolsín recibido para inspeccionar su remito y documentaciones.");
        this.lblCardSub.getStyleClass().add("card-subtitle");
        cardTitles.getChildren().addAll(lblCardTitle, lblCardSub);
        cardHeader.getChildren().add(cardTitles);

        // Panel de Filtros (Nº precinto y CM origen)
        HBox filterBox = new HBox(12);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.setPadding(new Insets(10, 14, 10, 14));
        filterBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

        Label lblFiltrarTag = new Label("Filtros de Búsqueda:");
        lblFiltrarTag.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");

        this.txtFiltroPrecinto = new TextField();
        this.txtFiltroPrecinto.setPromptText("Buscar por Nº de precinto (ej. PREC-7041)...");
        this.txtFiltroPrecinto.setPrefWidth(240);
        this.txtFiltroPrecinto.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());

        this.cmbFiltroCMOrigen = new ComboBox<>();
        this.cmbFiltroCMOrigen.setPromptText("Todas las CM Origen");
        this.cmbFiltroCMOrigen.setPrefWidth(220);
        this.cmbFiltroCMOrigen.setOnAction(e -> aplicarFiltros());

        Button btnLimpiarFiltros = new Button("Limpiar");
        btnLimpiarFiltros.getStyleClass().add("btn-secondary");
        btnLimpiarFiltros.setOnAction(e -> {
            actualizandoFiltros = true;
            try {
                txtFiltroPrecinto.clear();
                cmbFiltroCMOrigen.setValue(null);
            } finally {
                actualizandoFiltros = false;
            }
            mostrarBolsines(gestor.getBolsinesEnviados());
        });

        filterBox.getChildren().addAll(lblFiltrarTag, txtFiltroPrecinto, cmbFiltroCMOrigen, btnLimpiarFiltros);

        // Alerta A1 / A2
        this.boxAlertaPaso1 = new VBox();
        this.boxAlertaPaso1.getStyleClass().addAll("alert-box", "alert-box-warning");
        this.lblMensajeAlertaPaso1 = new Label();
        this.boxAlertaPaso1.getChildren().add(lblMensajeAlertaPaso1);
        this.boxAlertaPaso1.setVisible(false);
        this.boxAlertaPaso1.setManaged(false);

        // Tabla de Bolsines
        this.listaBolsinesData = FXCollections.observableArrayList();
        this.tblBolsines = new TableView<>(listaBolsinesData);
        this.tblBolsines.setPlaceholder(new Label("No se encontraron bolsines enviados para esta Comisión Médica."));
        this.tblBolsines.setMinHeight(240);
        this.tblBolsines.setPrefHeight(340);
        this.tblBolsines.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(tblBolsines, Priority.ALWAYS);

        TableColumn<Bolsin, String> colPrecinto = new TableColumn<>("Nº de Precinto");
        colPrecinto.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNumeroPrecinto()));
        colPrecinto.setPrefWidth(120);
        colPrecinto.setStyle("-fx-font-weight: bold;");

        TableColumn<Bolsin, String> colOrigen = new TableColumn<>("CM de Origen");
        colOrigen.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCMOrigen().getNombre()));
        colOrigen.setPrefWidth(210);

        TableColumn<Bolsin, String> colDestino = new TableColumn<>("CM de Destino");
        colDestino.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCmDestino() != null ? data.getValue().getCmDestino().getNombre() : "-"));
        colDestino.setPrefWidth(230);

        TableColumn<Bolsin, String> colRemito = new TableColumn<>("Nº Remito");
        colRemito.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getRemito() != null ? data.getValue().getRemito().getNumero() : "S/R"));
        colRemito.setPrefWidth(135);

        TableColumn<Bolsin, String> colFecha = new TableColumn<>("Fecha de Envío");
        colFecha.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getFechaEnvio().format(FORMATO_FECHA)));
        colFecha.setPrefWidth(135);

        TableColumn<Bolsin, String> colEstado = new TableColumn<>("Estado Actual");
        colEstado.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEstadoActual() != null ? data.getValue().getEstadoActual().getNombreDisplay() : "Enviado"));
        colEstado.setPrefWidth(120);
        colEstado.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("badge", "badge-enviado");
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        tblBolsines.getColumns().addAll(colPrecinto, colOrigen, colDestino, colRemito, colFecha, colEstado);

        tblBolsines.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            this.bolsinSeleccionadoEnTabla = newVal;
            btnSeleccionarBolsin.setDisable(newVal == null);
        });

        // Doble clic para seleccionar directo
        tblBolsines.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tblBolsines.getSelectionModel().getSelectedItem() != null) {
                tomarSeleccionBolsin(tblBolsines.getSelectionModel().getSelectedItem());
            }
        });

        // Barra de acciones inferior
        HBox actionsBar = new HBox(12);
        actionsBar.setAlignment(Pos.CENTER_RIGHT);

        this.btnSeleccionarBolsin = new Button("Seleccionar Bolsín e Inspeccionar Contenido →");
        this.btnSeleccionarBolsin.getStyleClass().addAll("btn-primary");
        this.btnSeleccionarBolsin.setDisable(true);
        this.btnSeleccionarBolsin.setOnAction(e -> tomarSeleccionBolsin(tblBolsines.getSelectionModel().getSelectedItem()));

        actionsBar.getChildren().add(btnSeleccionarBolsin);

        card.getChildren().addAll(cardHeader, filterBox, boxAlertaPaso1, tblBolsines, actionsBar);
        root.getChildren().add(card);
        return root;
    }

    private void aplicarFiltros() {
        if (actualizandoFiltros) {
            return;
        }
        ocultarAlertaPaso1();
        String precinto = txtFiltroPrecinto.getText();
        ComisionMedica origen = cmbFiltroCMOrigen.getValue();
        List<Bolsin> filtrados = gestor.filtrarBolsines(precinto, origen);
        this.listaBolsinesData.setAll(filtrados);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VISTA 2: INSPECCIÓN DE REMITO Y SELECCIÓN DE OPCIÓN DE RECEPCIÓN
    // ─────────────────────────────────────────────────────────────────────────

    private VBox crearVistaPaso2() {
        VBox root = new VBox(10);
        root.setAlignment(Pos.TOP_CENTER);

        VBox card = new VBox(10);
        card.getStyleClass().add("card-panel");
        VBox.setVgrow(card, Priority.ALWAYS);

        // 1. Resumen de Cabecera (Bolsín y Remito)
        HBox summaryBox = new HBox(16);
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        summaryBox.setPadding(new Insets(8, 14, 8, 14));
        summaryBox.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8px; -fx-border-color: #cbd5e1; -fx-border-radius: 8px;");

        VBox colBolsin = new VBox(2);
        Label lblT1 = new Label("BOLSÍN SELECCIONADO");
        lblT1.setStyle("-fx-font-size: 9.5px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        this.lblDetallePrecinto = new Label("PREC-XXXX");
        this.lblDetallePrecinto.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        this.lblDetalleCMOrigen = new Label("Origen: -");
        this.lblDetalleCMOrigen.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        colBolsin.getChildren().addAll(lblT1, lblDetallePrecinto, lblDetalleCMOrigen);

        VBox colRemito = new VBox(2);
        Label lblT2 = new Label("REMITO ADJUNTO");
        lblT2.setStyle("-fx-font-size: 9.5px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        this.lblDetalleNumeroRemito = new Label("REM-XXXX");
        this.lblDetalleNumeroRemito.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        this.lblDetalleFechaEnvio = new Label("Enviado: -");
        this.lblDetalleFechaEnvio.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        colRemito.getChildren().addAll(lblT2, lblDetalleNumeroRemito, lblDetalleFechaEnvio);

        VBox colDocsInfo = new VBox(2);
        Label lblT3 = new Label("CANTIDAD DE DOCUMENTACIONES");
        lblT3.setStyle("-fx-font-size: 9.5px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        this.lblDetalleCantDocs = new Label("0");
        this.lblDetalleCantDocs.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
        Label lblT3Sub = new Label("Documentos declarados en remito");
        lblT3Sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        colDocsInfo.getChildren().addAll(lblT3, lblDetalleCantDocs, lblT3Sub);

        VBox colIteratorInfo = new VBox(2);
        Label lblT4 = new Label("RECORRIDO ITERATOR");
        lblT4.setStyle("-fx-font-size: 9.5px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        Label lblT4Val = new Label("IteradorRemito → IteradorDetalleRemito");
        lblT4Val.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label lblT4Sub = new Label("Lectura desacoplada (GoF)");
        lblT4Sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        colIteratorInfo.getChildren().addAll(lblT4, lblT4Val, lblT4Sub);

        summaryBox.getChildren().addAll(colBolsin, new Separator(javafx.geometry.Orientation.VERTICAL),
                colRemito, new Separator(javafx.geometry.Orientation.VERTICAL),
                colDocsInfo, new Separator(javafx.geometry.Orientation.VERTICAL), colIteratorInfo);

        // 2. Tabla de Documentación con Checkbox reactivo
        VBox tableDocsSection = new VBox(6);
        VBox.setVgrow(tableDocsSection, Priority.ALWAYS);
        HBox docsHeaderBox = new HBox(10);
        docsHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label lblDocsTitle = new Label("Documentación Contenida en el Remito:");
        lblDocsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12.5px; -fx-text-fill: #1e293b;");

        this.lblAvisoDocsNoConformes = new Label("ℹ Marque/desmarque los casilleros para indicar documentos faltantes (A3) o erróneos (A4).");
        this.lblAvisoDocsNoConformes.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        docsHeaderBox.getChildren().addAll(lblDocsTitle, lblAvisoDocsNoConformes);

        this.listaDocsData = FXCollections.observableArrayList();
        this.tblDocumentacion = new TableView<>(listaDocsData);
        this.tblDocumentacion.setMinHeight(145);
        this.tblDocumentacion.setPrefHeight(175);
        this.tblDocumentacion.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        this.tblDocumentacion.setEditable(true);
        VBox.setVgrow(this.tblDocumentacion, Priority.ALWAYS);

        TableColumn<DocumentacionRow, Boolean> colCheck = new TableColumn<>("¿Conforme?");
        colCheck.setCellValueFactory(cellData -> cellData.getValue().seleccionadaProperty());
        colCheck.setCellFactory(CheckBoxTableCell.forTableColumn(colCheck));
        colCheck.setPrefWidth(90);
        colCheck.setEditable(true);

        TableColumn<DocumentacionRow, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));
        colId.setPrefWidth(70);

        TableColumn<DocumentacionRow, String> colTipoDoc = new TableColumn<>("Tipo de Documento");
        colTipoDoc.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTipo()));
        colTipoDoc.setPrefWidth(200);

        TableColumn<DocumentacionRow, String> colAsunto = new TableColumn<>("Asunto / Carátula");
        colAsunto.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAsunto()));
        colAsunto.setPrefWidth(300);

        TableColumn<DocumentacionRow, String> colEstadoActual = new TableColumn<>("Estado Actual");
        colEstadoActual.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEstadoActual()));
        colEstadoActual.setPrefWidth(140);
        colEstadoActual.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("badge", "badge-enviado");
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<DocumentacionRow, String> colEstadoResultante = new TableColumn<>("Estado Resultante Previsto");
        colEstadoResultante.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getEstadoResultante(opcionSeleccionadaActual)));
        colEstadoResultante.setPrefWidth(180);
        colEstadoResultante.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if (item.contains("aceptada")) {
                        badge.getStyleClass().add("badge-recibido");
                    } else if (item.contains("No recibida")) {
                        badge.getStyleClass().add("badge-parcial");
                    } else if (item.contains("rechazada")) {
                        badge.getStyleClass().add("badge-rechazado");
                    } else if (item.contains("redirigir")) {
                        badge.getStyleClass().add("badge-redirigir");
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        tblDocumentacion.getColumns().addAll(colCheck, colId, colTipoDoc, colAsunto, colEstadoActual, colEstadoResultante);
        tableDocsSection.getChildren().addAll(docsHeaderBox, tblDocumentacion);

        // 3. Panel de las 4 Opciones de Recepción
        Label lblOpcionesHeader = new Label("Seleccione la Opción de Recepción correspondiente (CU 28):");
        lblOpcionesHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 12.5px; -fx-text-fill: #1e293b;");

        this.boxOpcionesCards = new VBox(6);
        this.grupoOpciones = new ToggleGroup();
        this.radioOpcionesList = new ArrayList<>();

        // 4. Barra de Acciones
        HBox actionsBar = new HBox(12);
        actionsBar.setAlignment(Pos.CENTER_RIGHT);

        Button btnVolver = new Button("← Volver a Bolsines");
        btnVolver.getStyleClass().add("btn-secondary");
        btnVolver.setOnAction(e -> irAPaso(1));

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-danger");
        btnCancelar.setOnAction(e -> cancelar());

        this.btnContinuarConfirmacion = new Button("Continuar a Confirmación →");
        this.btnContinuarConfirmacion.getStyleClass().add("btn-primary");
        this.btnContinuarConfirmacion.setOnAction(e -> {
            if (opcionSeleccionadaActual != null) {
                tomarSeleccionOpcion(opcionSeleccionadaActual);
            }
        });

        actionsBar.getChildren().addAll(btnVolver, btnCancelar, btnContinuarConfirmacion);

        card.getChildren().addAll(summaryBox, tableDocsSection, lblOpcionesHeader, boxOpcionesCards, actionsBar);
        root.getChildren().add(card);
        return root;
    }

    private void construirCardsOpciones(List<OpcionRecepcion> opciones) {
        boxOpcionesCards.getChildren().clear();
        grupoOpciones.getToggles().clear();
        radioOpcionesList.clear();

        boolean destinoEsCMC = gestor.getComisionMedicaEmpleado() != null
                && (gestor.getComisionMedicaEmpleado().getNombre().toUpperCase().contains("CENTRAL")
                || gestor.getComisionMedicaEmpleado().getNombre().toUpperCase().contains("CMC"));

        for (OpcionRecepcion opc : opciones) {
            boolean deshabilitada = opc.isSoloCMC() && !destinoEsCMC;

            HBox card = new HBox(10);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("option-card");

            RadioButton rb = new RadioButton();
            rb.setToggleGroup(grupoOpciones);
            rb.setUserData(opc);
            rb.setDisable(deshabilitada);
            radioOpcionesList.add(rb);

            Label badgeNum = new Label("Opción " + opc.getNumero());
            badgeNum.setStyle("-fx-font-weight: bold; -fx-font-size: 10.5px; -fx-text-fill: #2563eb; -fx-background-color: #eff6ff; -fx-padding: 2px 7px; -fx-background-radius: 4px;");

            VBox textBox = new VBox(1);
            HBox titleRow = new HBox(8);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            Label lblTitulo = new Label(opc.getTitulo());
            lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 12.5px; -fx-text-fill: " + (deshabilitada ? "#94a3b8;" : "#0f172a;"));
            titleRow.getChildren().addAll(badgeNum, lblTitulo);

            if (deshabilitada) {
                Label badgeSoloCMC = new Label("Solo disponible en Comisión Médica Central (CMC)");
                badgeSoloCMC.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #b91c1c; -fx-background-color: #fef2f2; -fx-padding: 1px 6px; -fx-background-radius: 4px;");
                titleRow.getChildren().add(badgeSoloCMC);
                card.getStyleClass().add("option-card-disabled");
            }

            Label lblDesc = new Label(opc.getDescripcion());
            lblDesc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748b;");

            HBox badgesImpacto = new HBox(8);
            badgesImpacto.setPadding(new Insets(1, 0, 0, 0));
            Label tagRemito = new Label("Remito: " + opc.getImpactoRemito());
            tagRemito.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #475569; -fx-background-color: #f1f5f9; -fx-padding: 1px 6px; -fx-background-radius: 4px;");
            Label tagDoc = new Label("Documentación: " + opc.getImpactoDocumentacion());
            tagDoc.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #475569; -fx-background-color: #f1f5f9; -fx-padding: 1px 6px; -fx-background-radius: 4px;");
            badgesImpacto.getChildren().addAll(tagRemito, tagDoc);

            textBox.getChildren().addAll(titleRow, lblDesc, badgesImpacto);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            card.getChildren().addAll(rb, textBox);

            if (!deshabilitada) {
                card.setOnMouseClicked(event -> rb.setSelected(true));
            }

            rb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    card.getStyleClass().add("option-card-selected");
                    this.opcionSeleccionadaActual = opc;
                    btnContinuarConfirmacion.setDisable(false);
                    actualizarAvisoOpcionPaso2(opc);
                    tblDocumentacion.refresh();
                } else {
                    card.getStyleClass().remove("option-card-selected");
                }
            });

            boxOpcionesCards.getChildren().add(card);
        }

        // Seleccionar Opción 1 por defecto
        if (!radioOpcionesList.isEmpty()) {
            radioOpcionesList.get(0).setSelected(true);
            if (!opciones.isEmpty()) {
                actualizarAvisoOpcionPaso2(opciones.get(0));
            }
        }
    }

    private void actualizarAvisoOpcionPaso2(OpcionRecepcion opc) {
        if (lblAvisoDocsNoConformes == null || opc == null) {
            return;
        }
        if (opc.esContenidoIgual()) {
            lblAvisoDocsNoConformes.setText("✔ Opción 1 (Flujo Básico): Todos los documentos del remito pasarán a 'Recibida y aceptada'.");
            lblAvisoDocsNoConformes.setStyle("-fx-font-size: 11px; -fx-text-fill: #047857; -fx-font-weight: bold;");
        } else if (opc.esFaltaDocumentacion()) {
            lblAvisoDocsNoConformes.setText("⚠ Flujo A3: Desmarque en la columna '¿Conforme?' los documentos que NO llegaron físicamente (pasarán a 'No recibida').");
            lblAvisoDocsNoConformes.setStyle("-fx-font-size: 11px; -fx-text-fill: #b45309; -fx-font-weight: bold;");
        } else if (opc.esNoCorrespondeDestino()) {
            lblAvisoDocsNoConformes.setText("⚠ Flujo A4: Desmarque en la columna '¿Conforme?' los documentos que NO corresponden a esta CM (pasarán a 'Recibida y rechazada').");
            lblAvisoDocsNoConformes.setStyle("-fx-font-size: 11px; -fx-text-fill: #b91c1c; -fx-font-weight: bold;");
        } else if (opc.esRedirigirAOtraArea()) {
            lblAvisoDocsNoConformes.setText("↪ Flujo A5 (Solo CMC): Todos los documentos del remito pasarán a 'Para redirigir' a otra área interna.");
            lblAvisoDocsNoConformes.setStyle("-fx-font-size: 11px; -fx-text-fill: #1d4ed8; -fx-font-weight: bold;");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VISTA 3: CONFIRMACIÓN DE LA RECEPCIÓN
    // ─────────────────────────────────────────────────────────────────────────

    private VBox crearVistaPaso3() {
        VBox root = new VBox(16);
        root.setAlignment(Pos.TOP_CENTER);

        VBox card = new VBox(18);
        card.getStyleClass().add("card-panel");
        VBox.setVgrow(card, Priority.ALWAYS);

        // Header de Confirmación
        HBox confHeader = new HBox(12);
        confHeader.setAlignment(Pos.CENTER_LEFT);
        VBox titles = new VBox(2);
        Label lblTitle = new Label("Confirmación de Recepción de Bolsín");
        lblTitle.getStyleClass().add("card-title");
        Label lblSub = new Label("Verifique el resumen de cambios de estado antes de asentar la recepción formal.");
        lblSub.getStyleClass().add("card-subtitle");
        titles.getChildren().addAll(lblTitle, lblSub);
        confHeader.getChildren().add(titles);

        // Panel de Resumen
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

        // Fila 0: Bolsín
        Label t0 = new Label("Bolsín a Recepcionar:");
        t0.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblConfPrecinto = new Label();
        this.lblConfPrecinto.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label t0Estado = new Label("Enviado ➔ Nuevo Estado: Recibido en CM Destino");
        t0Estado.getStyleClass().addAll("badge", "badge-recibido");
        grid.add(t0, 0, 0);
        grid.add(lblConfPrecinto, 1, 0);
        grid.add(t0Estado, 2, 0);

        // Fila 1: CM Origen
        Label t1 = new Label("Comisión Médica Origen:");
        t1.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblConfCMOrigen = new Label();
        grid.add(t1, 0, 1);
        grid.add(lblConfCMOrigen, 1, 1);

        // Fila 2: Opción elegida
        Label tOpc = new Label("Opción Seleccionada:");
        tOpc.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblConfOpcionElegida = new Label();
        this.lblConfOpcionElegida.setStyle("-fx-font-weight: bold; -fx-text-fill: #1d4ed8;");
        grid.add(tOpc, 0, 2);
        grid.add(lblConfOpcionElegida, 1, 2, 2, 1);

        // Fila 3: Remito
        Label t2 = new Label("Remito Asociado:");
        t2.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblConfRemito = new Label();
        this.lblConfEstadoRemito = new Label();
        this.lblConfEstadoRemito.getStyleClass().addAll("badge", "badge-recibido");
        grid.add(t2, 0, 3);
        grid.add(lblConfRemito, 1, 3);
        grid.add(lblConfEstadoRemito, 2, 3);

        // Fila 4: Empleado responsable
        Label t3 = new Label("Empleado Responsable:");
        t3.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblConfEmpleado = new Label();
        this.lblConfFechaHora = new Label();
        this.lblConfFechaHora.setStyle("-fx-text-fill: #64748b;");
        grid.add(t3, 0, 4);
        grid.add(lblConfEmpleado, 1, 4);
        grid.add(lblConfFechaHora, 2, 4);

        // Fila 5: Notificación CU 29
        Label t4 = new Label("Notificación (CU 29):");
        t4.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblConfDestinatarioCU29 = new Label();
        Label t4Badge = new Label("Se enviará email a la CM Origen");
        t4Badge.getStyleClass().addAll("badge", "badge-enviado");
        grid.add(t4, 0, 5);
        grid.add(lblConfDestinatarioCU29, 1, 5);
        grid.add(t4Badge, 2, 5);

        // Desglose de Documentaciones
        Label lblDocsSub = new Label("Detalle de Actualización de Documentaciones:");
        lblDocsSub.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");

        ScrollPane scrollDocs = new ScrollPane();
        scrollDocs.setFitToWidth(true);
        scrollDocs.setPrefHeight(150);
        scrollDocs.setStyle("-fx-background-color: transparent; -fx-border-color: #e2e8f0; -fx-border-radius: 6px;");

        this.boxConfDocsList = new VBox(8);
        this.boxConfDocsList.setPadding(new Insets(10));
        scrollDocs.setContent(boxConfDocsList);

        // Botones de Confirmación
        HBox actionsBar = new HBox(12);
        actionsBar.setAlignment(Pos.CENTER_RIGHT);

        Button btnModificar = new Button("← Modificar Selección");
        btnModificar.getStyleClass().add("btn-secondary");
        btnModificar.setOnAction(e -> irAPaso(2));

        Button btnRechazarA6 = new Button("No Confirmar (A6)");
        btnRechazarA6.getStyleClass().add("btn-danger");
        btnRechazarA6.setOnAction(e -> tomarConfirmacionSeleccion(false));

        this.btnConfirmarRecepcion = new Button("✔ Confirmar Recepción y Enviar Notificación (CU 29)");
        this.btnConfirmarRecepcion.getStyleClass().add("btn-success");
        this.btnConfirmarRecepcion.setOnAction(e -> tomarConfirmacionSeleccion(true));

        actionsBar.getChildren().addAll(btnModificar, btnRechazarA6, btnConfirmarRecepcion);

        card.getChildren().addAll(confHeader, grid, lblDocsSub, scrollDocs, actionsBar);
        root.getChildren().add(card);
        return root;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VISTA 4: RESULTADO Y NOTIFICACIÓN CU 29
    // ─────────────────────────────────────────────────────────────────────────

    private VBox crearVistaPaso4() {
        VBox root = new VBox(16);
        root.setAlignment(Pos.TOP_CENTER);

        VBox card = new VBox(14);
        card.getStyleClass().add("card-panel");
        VBox.setVgrow(card, Priority.ALWAYS);

        // Banner de Éxito
        HBox successBanner = new HBox(16);
        successBanner.setAlignment(Pos.CENTER_LEFT);
        successBanner.getStyleClass().addAll("alert-box", "alert-box-success");
        successBanner.setPadding(new Insets(14, 16, 14, 16));

        Label lblCheck = new Label("✔");
        lblCheck.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #059669;");

        VBox bannerText = new VBox(2);
        this.lblResultadoTitulo = new Label("¡Recepción Registrada con Éxito! (CU 28)");
        this.lblResultadoTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #065f46;");
        this.lblResultadoSubtitulo = new Label("Los estados fueron actualizados y se transmitió la notificación por correo (CU 29).");
        this.lblResultadoSubtitulo.setStyle("-fx-font-size: 13px; -fx-text-fill: #047857;");
        bannerText.getChildren().addAll(lblResultadoTitulo, lblResultadoSubtitulo);

        successBanner.getChildren().addAll(lblCheck, bannerText);

        // Bloque 1: Resumen de Trazabilidad Asentada (CU 28)
        HBox resSummaryBox = new HBox(20);
        resSummaryBox.setAlignment(Pos.CENTER_LEFT);
        resSummaryBox.setPadding(new Insets(12, 16, 12, 16));
        resSummaryBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

        VBox resColBolsin = new VBox(4);
        Label lblRBTag = new Label("BOLSÍN RECEPCIONADO");
        lblRBTag.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        HBox resBolsinRow = new HBox(8);
        resBolsinRow.setAlignment(Pos.CENTER_LEFT);
        this.lblResBolsin = new Label("PREC-XXXX");
        this.lblResBolsin.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label badgeResBolsin = new Label("Recibido en CM Destino");
        badgeResBolsin.getStyleClass().addAll("badge", "badge-recibido");
        resBolsinRow.getChildren().addAll(lblResBolsin, badgeResBolsin);
        resColBolsin.getChildren().addAll(lblRBTag, resBolsinRow);

        VBox resColRemito = new VBox(4);
        Label lblRRTag = new Label("REMITO ACTUALIZADO");
        lblRRTag.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        HBox resRemitoRow = new HBox(8);
        resRemitoRow.setAlignment(Pos.CENTER_LEFT);
        this.lblResRemito = new Label("REM-XXXX");
        this.lblResRemito.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        this.lblResEstadoRemito = new Label("Recibido y aceptado");
        this.lblResEstadoRemito.getStyleClass().addAll("badge", "badge-recibido");
        resRemitoRow.getChildren().addAll(lblResRemito, lblResEstadoRemito);
        resColRemito.getChildren().addAll(lblRRTag, resRemitoRow);

        VBox resColResp = new VBox(4);
        Label lblRRespTag = new Label("RESPONSABLE Y FECHA/HORA");
        lblRRespTag.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        this.lblResEmpleadoFecha = new Label("-");
        this.lblResEmpleadoFecha.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
        resColResp.getChildren().addAll(lblRRespTag, lblResEmpleadoFecha);

        resSummaryBox.getChildren().addAll(resColBolsin, new Separator(javafx.geometry.Orientation.VERTICAL),
                resColRemito, new Separator(javafx.geometry.Orientation.VERTICAL), resColResp);

        // Bloque 2: Comprobante de Correo Electrónico Enviado (CU 29)
        VBox emailCard = new VBox(10);
        emailCard.setPadding(new Insets(14, 16, 14, 16));
        emailCard.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8px; -fx-border-color: #cbd5e1; -fx-border-radius: 8px;");
        VBox.setVgrow(emailCard, Priority.ALWAYS);

        HBox emailCardHeader = new HBox(12);
        emailCardHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblEmailTag = new Label("✉ NOTIFICACIÓN ELECTRÓNICA CU 29 — SERVIDOR DE CORREO");
        lblEmailTag.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0f172a;");
        Region spacerEmail = new Region();
        HBox.setHgrow(spacerEmail, Priority.ALWAYS);
        Label badgeCorreoEnviado = new Label("✔ CORREO ENVIADO EXITOSAMENTE");
        badgeCorreoEnviado.getStyleClass().addAll("badge", "badge-recibido");
        emailCardHeader.getChildren().addAll(lblEmailTag, spacerEmail, badgeCorreoEnviado);

        GridPane emailMetaGrid = new GridPane();
        emailMetaGrid.setHgap(14);
        emailMetaGrid.setVgap(6);
        emailMetaGrid.setPadding(new Insets(8, 12, 8, 12));
        emailMetaGrid.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 6px; -fx-border-color: #e2e8f0; -fx-border-radius: 6px;");

        Label lblDeKey = new Label("De:");
        lblDeKey.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        Label lblDeVal = new Label("Sistema Central de Bolsines — SRT (CU 29)");
        lblDeVal.setStyle("-fx-text-fill: #0f172a;");

        Label lblParaKey = new Label("Para (CM Origen):");
        lblParaKey.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblEmailPara = new Label("-");
        this.lblEmailPara.setStyle("-fx-font-weight: bold; -fx-text-fill: #1d4ed8;");

        Label lblAsuntoKey = new Label("Asunto:");
        lblAsuntoKey.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblEmailAsunto = new Label("-");
        this.lblEmailAsunto.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label lblFechaKey = new Label("Fecha de envío:");
        lblFechaKey.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblEmailFecha = new Label("-");
        this.lblEmailFecha.setStyle("-fx-text-fill: #334155;");

        emailMetaGrid.add(lblDeKey, 0, 0);
        emailMetaGrid.add(lblDeVal, 1, 0);
        emailMetaGrid.add(lblParaKey, 0, 1);
        emailMetaGrid.add(lblEmailPara, 1, 1);
        emailMetaGrid.add(lblAsuntoKey, 0, 2);
        emailMetaGrid.add(lblEmailAsunto, 1, 2);
        emailMetaGrid.add(lblFechaKey, 0, 3);
        emailMetaGrid.add(lblEmailFecha, 1, 3);

        this.txtCuerpoCorreoCU29 = new TextArea();
        this.txtCuerpoCorreoCU29.setEditable(false);
        this.txtCuerpoCorreoCU29.setWrapText(true);
        this.txtCuerpoCorreoCU29.getStyleClass().addAll("notification-preview-box", "notification-code-text");
        this.txtCuerpoCorreoCU29.setPrefRowCount(9);
        VBox.setVgrow(txtCuerpoCorreoCU29, Priority.ALWAYS);

        emailCard.getChildren().addAll(emailCardHeader, emailMetaGrid, txtCuerpoCorreoCU29);

        // Botón para reiniciar flujo
        HBox actionsBar = new HBox(12);
        actionsBar.setAlignment(Pos.CENTER_RIGHT);

        this.btnNuevaRecepcion = new Button("↻ Registrar Nueva Recepción de Bolsín");
        this.btnNuevaRecepcion.getStyleClass().add("btn-primary");
        this.btnNuevaRecepcion.setOnAction(e -> {
            String avisoPendiente = this.ultimoBolsinRegistradoInfo;
            this.ultimoBolsinRegistradoInfo = null;
            tomarOpcionRegistrarRecepcionBolsin();
            if (avisoPendiente != null) {
                mostrarAvisoExitoPaso1(avisoPendiente);
            }
        });

        actionsBar.getChildren().add(btnNuevaRecepcion);

        card.getChildren().addAll(successBanner, resSummaryBox, emailCard, actionsBar);
        root.getChildren().add(card);
        return root;
    }

    private HBox crearFooterBar() {
        HBox footer = new HBox(16);
        footer.getStyleClass().add("footer-bar");
        footer.setAlignment(Pos.CENTER_LEFT);

        this.lblStatusMensaje = new Label("Sistema inicializado.");
        HBox.setHgrow(lblStatusMensaje, Priority.ALWAYS);

        this.lblStatusTimestamp = new Label(java.time.LocalDate.now().toString());

        footer.getChildren().addAll(lblStatusMensaje, lblStatusTimestamp);
        return footer;
    }

    private void actualizarStatus(String mensaje) {
        if (lblStatusMensaje != null) {
            lblStatusMensaje.setText(mensaje);
        }
    }
}
