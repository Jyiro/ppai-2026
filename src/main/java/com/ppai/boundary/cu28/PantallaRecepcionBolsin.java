package com.ppai.boundary.cu28;

import com.ppai.controller.cu28.GestorRecepcionBolsin;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Boundary PantallaRecepcionBolsin — CU 28: Registrar Recepción de Bolsín.
 * Cumple arquitectura BCE estricta: cero imports de dominio, iteradores o repositorio;
 * se comunica con GestorRecepcionBolsin exclusivamente mediante primitivos y String.
 */
public class PantallaRecepcionBolsin extends BorderPane {

    private static final String BADGE_OK = "badge-recibido";
    private static final String BADGE_WARN = "badge-parcial";
    private static final String BADGE_ERROR = "badge-rechazado";
    private static final String BADGE_INFO = "badge-redirigir";

    private final GestorRecepcionBolsin gestor;
    private boolean habilitada;

    // ── Componentes de Encabezado y Navegación ──
    private Label lblTitulo;
    private Label lblSubtitulo;
    private Label lblUsuarioActivo;
    private Label lblCmActiva;
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
    private ComboBox<String> cmbFiltroCMOrigen;
    private TableView<FilaBolsinVisual> tblBolsines;
    private ObservableList<FilaBolsinVisual> listaBolsinesData;
    private Button btnSeleccionarBolsin;
    private VBox boxAlertaPaso1;
    private Label lblMensajeAlertaPaso1;
    private boolean actualizandoFiltros;

    // ── Paso 2: Remitos, Documentación y Opciones ──
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
    private Button btnContinuarConfirmacion;
    private Label lblAvisoDocsNoConformes;

    // ── Paso 3: Confirmación ──
    private VBox viewPaso3;
    private Label lblConfPrecinto;
    private Label lblConfEstadoBolsin;
    private Label lblConfCMOrigen;
    private Label lblConfOpcionElegida;
    private VBox boxConfRemitosList;
    private VBox boxConfDocsList;
    private Label lblConfEmpleado;
    private Label lblConfFechaHora;
    private Label lblConfDestinatarioCU29;
    private Button btnConfirmarRecepcion;

    // ── Paso 4: Resultado y Notificación ──
    private VBox viewPaso4;
    private Label lblResultadoTitulo;
    private Label lblResultadoSubtitulo;
    private Label lblResBolsin;
    private Label lblResEstadoBolsin;
    private VBox boxResRemitosList;
    private Label lblResEmpleadoFecha;
    private Label lblEmailDe;
    private Label lblEmailPara;
    private Label lblEmailAsunto;
    private Label lblEmailFecha;
    private TextArea txtCuerpoCorreoCU29;
    private Button btnNuevaRecepcion;

    // ── Barra Inferior (Footer) ──
    private Label lblStatusMensaje;
    private Label lblStatusUsuario;

    // ── Estado Transitorio puramente visual (solo primitivos/String) ──
    private int[] precintosRecibidos = new int[0];
    private String[] cmsOrigenRecibidas = new String[0];
    private int precintoSeleccionadoEnTabla = -1;
    private int opcionSeleccionadaNumero = 0;
    private boolean opcionActualRequiereDocs = false;
    private String ultimoBolsinRegistradoInfo;

    public PantallaRecepcionBolsin(GestorRecepcionBolsin gestor) {
        this.gestor = gestor;
        this.habilitada = false;
        construirInterfaz();
    }

    // =========================================================================
    // MÉTODOS DEL DIAGRAMA DE SECUENCIA (100% primitivos / String)
    // =========================================================================

    /**
     * DS-CU28 pasos 1-3: Inicia el caso de uso, habilita la pantalla y delega en el gestor.
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
        this.precintoSeleccionadoEnTabla = -1;
        this.opcionSeleccionadaNumero = 0;
        this.opcionActualRequiereDocs = false;
        irAPaso(1);
        actualizarStatus("Pantalla habilitada. Esperando selección de bolsín.");
    }

    public boolean isHabilitada() {
        return habilitada;
    }

    /**
     * DS-CU28 paso 8: Muestra el usuario autenticado y su Comisión Médica activa.
     */
    public void mostrarCM(String nombreUsuario, String nombreEmpleado, String nombreCM) {
        String usr = (nombreUsuario != null && !nombreUsuario.isEmpty()) ? nombreUsuario : "-";
        String emp = (nombreEmpleado != null && !nombreEmpleado.isEmpty()) ? " (" + nombreEmpleado + ")" : "";
        String textoUsuario = "Usuario: " + usr + emp;

        if (lblUsuarioActivo != null) {
            lblUsuarioActivo.setText(textoUsuario);
            lblUsuarioActivo.setTooltip(new Tooltip(textoUsuario));
        }
        if (lblStatusUsuario != null) {
            lblStatusUsuario.setText(usr);
        }
        if (nombreCM != null && !nombreCM.isEmpty()) {
            String textoCm = "CM Destino: " + nombreCM;
            lblCmActiva.setText(textoCm);
            lblCmActiva.setTooltip(new Tooltip(textoCm));
            actualizarStatus("Comisión Médica activa: " + nombreCM);
        }
    }

    /**
     * DS-CU28 sec. 3 (fuera del loop): Recibe los números de precinto filtrados.
     */
    public void mostrarNumeroPrecinto(int[] precintos) {
        this.precintosRecibidos = (precintos != null) ? precintos : new int[0];
    }

    /**
     * DS-CU28 sec. 3 (fuera del loop): Recibe las CM origen de los bolsines filtrados.
     */
    public void mostrarCMOrigen(String[] cmsOrigen) {
        this.cmsOrigenRecibidas = (cmsOrigen != null) ? cmsOrigen : new String[0];
    }

    /**
     * Completa la grilla de Paso 1 con los datos visuales enviados por el gestor en String[][].
     */
    public void mostrarInformacionAdicionalBolsines(String[][] filasTabla,
                                                   String[] catalogoCMsOrigen,
                                                   int totalSistema) {
        this.precintoSeleccionadoEnTabla = -1;
        this.opcionSeleccionadaNumero = 0;

        List<FilaBolsinVisual> filas = new ArrayList<>();
        int cantidad = Math.min(precintosRecibidos.length, cmsOrigenRecibidas.length);
        if (filasTabla != null && filasTabla.length > 0) {
            cantidad = filasTabla.length;
        }

        for (int i = 0; i < cantidad; i++) {
            int nroPrecinto = (i < precintosRecibidos.length) ? precintosRecibidos[i] : 0;
            String cmOrigen = (i < cmsOrigenRecibidas.length) ? cmsOrigenRecibidas[i] : "-";
            String cmDestino = "-";
            String remitos = "S/R";
            String fecha = "-";
            String estado = "-";
            if (filasTabla != null && i < filasTabla.length && filasTabla[i] != null) {
                String[] f = filasTabla[i];
                if (nroPrecinto == 0 && f.length > 0 && f[0] != null && !f[0].isEmpty()) {
                    nroPrecinto = Integer.parseInt(f[0]);
                }
                if (f.length > 1 && f[1] != null) cmOrigen = f[1];
                if (f.length > 2 && f[2] != null) cmDestino = f[2];
                if (f.length > 3 && f[3] != null) remitos = f[3];
                if (f.length > 4 && f[4] != null) fecha = f[4];
                if (f.length > 5 && f[5] != null) estado = f[5];
            }
            filas.add(new FilaBolsinVisual(nroPrecinto, cmOrigen, cmDestino, remitos, fecha, estado));
        }

        this.listaBolsinesData.setAll(filas);
        tblBolsines.refresh();

        if (lblCardSub != null) {
            lblCardSub.setText("Se encontraron " + filas.size() + " bolsín(es) pendientes de recepción "
                    + "(sobre " + totalSistema + " registrados).");
        }

        if (catalogoCMsOrigen != null) {
            String seleccionPrevia = cmbFiltroCMOrigen.getValue();
            actualizandoFiltros = true;
            try {
                cmbFiltroCMOrigen.setItems(FXCollections.observableArrayList(catalogoCMsOrigen));
                if (seleccionPrevia != null) {
                    cmbFiltroCMOrigen.setValue(seleccionPrevia);
                }
            } finally {
                actualizandoFiltros = false;
            }
        }

        ocultarAlertaPaso1();
        actualizarStatus("Bolsines disponibles: " + filas.size());
    }

    /**
     * DS-CU28 paso 10: Solicita al Encargado de Bolsines seleccionar un bolsín.
     */
    public void solicitarSeleccionBolsin() {
        btnSeleccionarBolsin.setDisable(tblBolsines.getSelectionModel().getSelectedItem() == null);
    }

    /**
     * DS-CU28 pasos 11-12: Envía al gestor únicamente el número de precinto (int).
     */
    public void tomarSeleccionBolsin(int numeroPrecinto) {
        this.precintoSeleccionadoEnTabla = numeroPrecinto;
        gestor.tomarSeleccionBolsin(numeroPrecinto);
    }

    public int getPrecintoSeleccionadoActual() {
        return precintoSeleccionadoEnTabla;
    }

    /**
     * DS-CU28 paso 15: Muestra todos los remitos y documentaciones recibidos del gestor en String[].
     *
     * @param resumenBolsin [precinto, cmOrigen, fechaEnvio, remitosCsv, cantRemitos, cantDocs]
     * @param filasDocs     Matriz [nroRemito, nroDoc, tipoDoc, asunto, estadoActual]
     */
    public void mostrarDatosDeRemito(String[] resumenBolsin, String[][] filasDocs) {
        if (resumenBolsin != null && resumenBolsin.length >= 6) {
            lblDetallePrecinto.setText(resumenBolsin[0]);
            lblDetalleCMOrigen.setText("CM Origen: " + resumenBolsin[1]);
            lblDetalleFechaEnvio.setText("Fecha de Envío: " + resumenBolsin[2]);
            lblDetalleNumeroRemito.setText(resumenBolsin[3] + " (" + resumenBolsin[4] + " remito/s)");
            lblDetalleCantDocs.setText(resumenBolsin[5]);
        }

        List<DocumentacionRow> rows = new ArrayList<>();
        if (filasDocs != null) {
            for (String[] f : filasDocs) {
                if (f != null && f.length >= 5) {
                    DocumentacionRow row = new DocumentacionRow(
                            Integer.parseInt(f[0]),
                            Integer.parseInt(f[1]),
                            f[2],
                            f[3],
                            f[4]
                    );
                    row.seleccionadaProperty().addListener((obs, oldVal, newVal) -> {
                        if (opcionSeleccionadaNumero > 0) {
                            refrescarPrevisualizacionPaso2();
                        }
                    });
                    rows.add(row);
                }
            }
        }
        listaDocsData.setAll(rows);
        tblDocumentacion.refresh();

        irAPaso(2);
        actualizarStatus("Detalle del bolsín " + (resumenBolsin != null && resumenBolsin.length > 0 ? resumenBolsin[0] : "") + " desplegado.");
    }

    /**
     * DS-CU28 paso 16: Muestra en pantalla las 4 opciones recibidas en String[][].
     * Fila: [numero, titulo, descripcion, impactoRemito, impactoDoc, requiereSeleccionDocs, habilitada]
     */
    public void mostrarOpcionesRecepcion(String[][] opciones) {
        construirCardsOpciones(opciones);
    }

    /**
     * DS-CU28 paso 17: Solicita al Encargado de Bolsines la selección de una opción.
     */
    public void solicitarSeleccionOpcion() {
        btnContinuarConfirmacion.setDisable(opcionSeleccionadaNumero <= 0);
    }

    /**
     * DS-CU28 pasos 18-19: Envía al gestor únicamente el número de opción y los números (int[]) no conformes.
     */
    public void tomarSeleccionOpcion(int numeroOpcion) {
        this.opcionSeleccionadaNumero = numeroOpcion;
        int[] docsNoConformes = obtenerNumerosDocsNoConformes();
        gestor.tomarSeleccionOpcion(numeroOpcion, docsNoConformes);
    }

    /**
     * DS-CU28 paso 20: Despliega confirmación multi-remito completa desde primitivos/String del gestor.
     *
     * @param cabecera         [precinto, cmOrigen, estadoBolsin, tituloOpcion, empleado, fechaHora, destinatarioCorreo, sevBolsin]
     * @param remitosPrevistos [nroRemito, estadoRemitoPrevisto, severidad]
     * @param docsPrevistos    [nroRemito, nroDoc, tipoDoc, asunto, estadoPrevio, estadoNuevo, severidad]
     */
    public void solicitarConfirmacionSeleccion(String[] cabecera,
                                             String[][] remitosPrevistos,
                                             String[][] docsPrevistos) {
        if (cabecera != null && cabecera.length >= 8) {
            lblConfPrecinto.setText(cabecera[0]);
            lblConfCMOrigen.setText(cabecera[1]);
            lblConfEstadoBolsin.setText(cabecera[2]);
            aplicarEstiloBadgePorSeveridad(lblConfEstadoBolsin, cabecera[7]);
            lblConfOpcionElegida.setText(cabecera[3]);
            lblConfEmpleado.setText(cabecera[4]);
            lblConfFechaHora.setText(cabecera[5]);
            lblConfDestinatarioCU29.setText(cabecera[6]);
        }

        boxConfRemitosList.getChildren().clear();
        if (remitosPrevistos != null) {
            for (String[] rem : remitosPrevistos) {
                if (rem == null || rem.length < 3) continue;
                HBox remRow = new HBox(10);
                remRow.setAlignment(Pos.CENTER_LEFT);
                Label lblNro = new Label("Remito N° " + rem[0]);
                lblNro.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");
                Label badgeEst = new Label(rem[1]);
                aplicarEstiloBadgePorSeveridad(badgeEst, rem[2]);
                remRow.getChildren().addAll(lblNro, badgeEst);
                boxConfRemitosList.getChildren().add(remRow);
            }
        }

        boxConfDocsList.getChildren().clear();
        if (docsPrevistos != null) {
            for (String[] doc : docsPrevistos) {
                if (doc == null || doc.length < 7) continue;
                HBox itemBox = new HBox(10);
                itemBox.setAlignment(Pos.CENTER_LEFT);

                Label lblRemDoc = new Label("R." + doc[0] + " · #" + doc[1] + " [" + doc[2] + "]");
                lblRemDoc.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");

                Label lblAsunto = new Label(doc[3]);
                lblAsunto.setStyle("-fx-text-fill: #1e293b;");
                HBox.setHgrow(lblAsunto, Priority.ALWAYS);

                Label lblPrevio = new Label(doc[4] + " ➔");
                lblPrevio.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

                Label lblNuevo = new Label(doc[5]);
                aplicarEstiloBadgePorSeveridad(lblNuevo, doc[6]);

                itemBox.getChildren().addAll(lblRemDoc, lblAsunto, lblPrevio, lblNuevo);
                boxConfDocsList.getChildren().add(itemBox);
            }
        }

        irAPaso(3);
        actualizarStatus("Esperando confirmación final de la operación.");
    }

    /**
     * DS-CU28 pasos 21-22: Captura la confirmación booleana y delega al gestor.
     */
    public void tomarConfirmacionSeleccion(boolean confirmada) {
        gestor.tomarConfirmacionSeleccion(confirmada);
    }

    /**
     * DS-CU28 paso 33: Informa el resultado y el comprobante de notificación de todos los remitos.
     *
     * @param comprobanteCorreo   [destinatario, email, remitente, asunto, fechaFmt, cuerpo, estadoBolsin, remitosResumen, precinto, responsable]
     * @param remitosActualizados Matriz [nroRemito, estadoFinal, severidad]
     */
    public void informarCorrectoEnvioNotificacion(String[] comprobanteCorreo, String[][] remitosActualizados) {
        if (comprobanteCorreo != null && comprobanteCorreo.length >= 10) {
            String destinatario = comprobanteCorreo[0];
            String email = comprobanteCorreo[1];
            String remitente = comprobanteCorreo[2];
            String asunto = comprobanteCorreo[3];
            String fecha = comprobanteCorreo[4];
            String cuerpo = comprobanteCorreo[5];
            String estadoBolsin = comprobanteCorreo[6];
            String remitosResumen = comprobanteCorreo[7];
            String precinto = comprobanteCorreo[8];
            String responsable = comprobanteCorreo[9];

            lblResultadoTitulo.setText("¡Recepción Registrada con Éxito!");
            lblResultadoSubtitulo.setText("El bolsín " + precinto + " (" + estadoBolsin
                    + ") fue registrado y se envió la notificación electrónica a " + destinatario + ".");

            lblResBolsin.setText(precinto);
            lblResEstadoBolsin.setText(estadoBolsin);
            lblResEmpleadoFecha.setText(responsable + " • " + fecha);
            lblEmailDe.setText(remitente);
            lblEmailPara.setText(destinatario + " <" + email + ">");
            lblEmailAsunto.setText(asunto);
            lblEmailFecha.setText(fecha);
            txtCuerpoCorreoCU29.setText(cuerpo);

            this.ultimoBolsinRegistradoInfo = "Bolsín " + precinto + " (Remitos: " + remitosResumen
                    + ") registrado correctamente.";
        }

        boxResRemitosList.getChildren().clear();
        if (remitosActualizados != null) {
            for (String[] rem : remitosActualizados) {
                if (rem == null || rem.length < 3) continue;
                HBox remRow = new HBox(8);
                remRow.setAlignment(Pos.CENTER_LEFT);
                Label lblNro = new Label("Remito " + rem[0]);
                lblNro.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");
                Label badgeEst = new Label(rem[1]);
                aplicarEstiloBadgePorSeveridad(badgeEst, rem[2]);
                remRow.getChildren().addAll(lblNro, badgeEst);
                boxResRemitosList.getChildren().add(remRow);
            }
        }

        irAPaso(4);
        actualizarStatus("Recepción registrada y notificación enviada.");

        if (comprobanteCorreo != null && comprobanteCorreo.length >= 9
                && javafx.application.Platform.isFxApplicationThread()
                && this.getScene() != null
                && this.getScene().getWindow() != null
                && this.getScene().getWindow().isShowing()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(this.getScene().getWindow());
            alert.setTitle("Recepción Registrada");
            alert.setHeaderText("✔ Bolsín " + comprobanteCorreo[8] + " registrado con éxito");
            alert.setContentText(
                    "Estado del Bolsín: " + comprobanteCorreo[6] + "\n"
                            + "Remitos: " + comprobanteCorreo[7] + "\n"
                            + "Notificación enviada a: " + comprobanteCorreo[0] + " (" + comprobanteCorreo[1] + ")");
            alert.showAndWait();
        }
    }

    /**
     * Obs. 1: Cancela la operación actual y regresa al Paso 1.
     */
    public void cancelar() {
        this.precintoSeleccionadoEnTabla = -1;
        this.opcionSeleccionadaNumero = 0;
        this.opcionActualRequiereDocs = false;
        this.ultimoBolsinRegistradoInfo = null;
        gestor.cancelarRecepcion();
        irAPaso(1);
        actualizarStatus("Operación cancelada. Regresando al listado de bolsines.");
    }

    /**
     * Muestra una alerta informativa o de flujo alternativo en Paso 1.
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
    // CONSTRUCCIÓN DE LA INTERFAZ GRÁFICA
    // =========================================================================

    private void construirInterfaz() {
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            this.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("[PANTALLA] No se pudo cargar styles.css: " + e.getMessage());
        }

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
        irAPaso(1);
    }

    private VBox crearTopContainer() {
        VBox topBox = new VBox();

        HBox headerBar = new HBox(10);
        headerBar.getStyleClass().add("header-bar");
        headerBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(1);
        titleBox.setMinWidth(210);
        this.lblTitulo = new Label("Recepción de Bolsines");
        this.lblTitulo.getStyleClass().add("header-title");

        this.lblSubtitulo = new Label("Sistema de Seguimiento y Trazabilidad de Remitos y Documentación Física");
        this.lblSubtitulo.getStyleClass().add("header-subtitle");
        titleBox.getChildren().addAll(lblTitulo, lblSubtitulo);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        this.lblUsuarioActivo = new Label("Usuario: -");
        this.lblUsuarioActivo.getStyleClass().add("session-chip");
        this.lblUsuarioActivo.setMaxWidth(260);

        this.lblCmActiva = new Label("CM Destino: -");
        this.lblCmActiva.getStyleClass().add("session-chip");
        this.lblCmActiva.setMaxWidth(260);

        this.btnCancelarGlobal = new Button("✕ Cancelar");
        this.btnCancelarGlobal.getStyleClass().addAll("btn-danger");
        this.btnCancelarGlobal.setMinWidth(Region.USE_PREF_SIZE);
        this.btnCancelarGlobal.setOnAction(e -> cancelar());

        headerBar.getChildren().addAll(titleBox, lblUsuarioActivo, lblCmActiva, btnCancelarGlobal);
        this.stepperBox = crearStepper();

        topBox.getChildren().addAll(headerBar, stepperBox);
        return topBox;
    }

    private HBox crearStepper() {
        HBox box = new HBox(12);
        box.getStyleClass().add("stepper-container");

        String[] titulos = {
                "1. Bolsines Pendientes",
                "2. Remitos y Documentación",
                "3. Confirmación de Recepción",
                "4. Resultado y Notificación"
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

        HBox cardHeader = new HBox(12);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        VBox cardTitles = new VBox(2);
        Label lblCardTitle = new Label("Bolsines Pendientes de Recepción");
        lblCardTitle.getStyleClass().add("card-title");
        this.lblCardSub = new Label("Seleccione del listado el bolsín recibido para inspeccionar sus remitos y documentaciones.");
        this.lblCardSub.getStyleClass().add("card-subtitle");
        cardTitles.getChildren().addAll(lblCardTitle, lblCardSub);
        cardHeader.getChildren().add(cardTitles);

        HBox filterBox = new HBox(12);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.setPadding(new Insets(10, 14, 10, 14));
        filterBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

        Label lblFiltrarTag = new Label("Filtros de Búsqueda:");
        lblFiltrarTag.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");

        this.txtFiltroPrecinto = new TextField();
        this.txtFiltroPrecinto.setPromptText("Buscar por N° de precinto (ej. 7041)...");
        this.txtFiltroPrecinto.setPrefWidth(240);
        this.txtFiltroPrecinto.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());

        this.cmbFiltroCMOrigen = new ComboBox<>();
        this.cmbFiltroCMOrigen.setPromptText("Todas las CM Origen");
        this.cmbFiltroCMOrigen.setPrefWidth(240);
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
            gestor.limpiarFiltros();
        });

        filterBox.getChildren().addAll(lblFiltrarTag, txtFiltroPrecinto, cmbFiltroCMOrigen, btnLimpiarFiltros);

        this.boxAlertaPaso1 = new VBox();
        this.boxAlertaPaso1.getStyleClass().addAll("alert-box", "alert-box-warning");
        this.lblMensajeAlertaPaso1 = new Label();
        this.boxAlertaPaso1.getChildren().add(lblMensajeAlertaPaso1);
        this.boxAlertaPaso1.setVisible(false);
        this.boxAlertaPaso1.setManaged(false);

        this.listaBolsinesData = FXCollections.observableArrayList();
        this.tblBolsines = new TableView<>(listaBolsinesData);
        this.tblBolsines.setPlaceholder(new Label("No se encontraron bolsines pendientes para esta Comisión Médica."));
        this.tblBolsines.setMinHeight(240);
        this.tblBolsines.setPrefHeight(340);
        this.tblBolsines.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(tblBolsines, Priority.ALWAYS);

        TableColumn<FilaBolsinVisual, String> colPrecinto = new TableColumn<>("N° de Precinto");
        colPrecinto.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().precinto)));
        colPrecinto.setPrefWidth(120);
        colPrecinto.setStyle("-fx-font-weight: bold;");

        TableColumn<FilaBolsinVisual, String> colOrigen = new TableColumn<>("CM de Origen");
        colOrigen.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().cmOrigen));
        colOrigen.setPrefWidth(210);

        TableColumn<FilaBolsinVisual, String> colDestino = new TableColumn<>("CM de Destino");
        colDestino.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().cmDestino));
        colDestino.setPrefWidth(230);

        TableColumn<FilaBolsinVisual, String> colRemito = new TableColumn<>("Remitos Asociados");
        colRemito.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().remitos));
        colRemito.setPrefWidth(145);

        TableColumn<FilaBolsinVisual, String> colFecha = new TableColumn<>("Fecha de Envío");
        colFecha.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fecha));
        colFecha.setPrefWidth(135);

        TableColumn<FilaBolsinVisual, String> colEstado = new TableColumn<>("Estado Actual");
        colEstado.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().estado));
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
            this.precintoSeleccionadoEnTabla = (newVal != null) ? newVal.precinto : -1;
            btnSeleccionarBolsin.setDisable(newVal == null);
        });

        tblBolsines.setOnMouseClicked(event -> {
            FilaBolsinVisual sel = tblBolsines.getSelectionModel().getSelectedItem();
            if (event.getClickCount() == 2 && sel != null) {
                tomarSeleccionBolsin(sel.precinto);
            }
        });

        HBox actionsBar = new HBox(12);
        actionsBar.setAlignment(Pos.CENTER_RIGHT);

        this.btnSeleccionarBolsin = new Button("Seleccionar Bolsín e Inspeccionar Contenido →");
        this.btnSeleccionarBolsin.getStyleClass().addAll("btn-primary");
        this.btnSeleccionarBolsin.setDisable(true);
        this.btnSeleccionarBolsin.setOnAction(e -> {
            FilaBolsinVisual sel = tblBolsines.getSelectionModel().getSelectedItem();
            if (sel != null) {
                tomarSeleccionBolsin(sel.precinto);
            }
        });

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
        String origen = cmbFiltroCMOrigen.getValue();
        gestor.filtrarBolsines(precinto, origen);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VISTA 2: INSPECCIÓN DE REMITOS Y SELECCIÓN DE OPCIÓN DE RECEPCIÓN
    // ─────────────────────────────────────────────────────────────────────────

    private VBox crearVistaPaso2() {
        VBox root = new VBox(10);
        root.setAlignment(Pos.TOP_CENTER);

        VBox card = new VBox(10);
        card.getStyleClass().add("card-panel");
        VBox.setVgrow(card, Priority.ALWAYS);

        HBox summaryBox = new HBox(16);
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        summaryBox.setPadding(new Insets(8, 14, 8, 14));
        summaryBox.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8px; -fx-border-color: #cbd5e1; -fx-border-radius: 8px;");

        VBox colBolsin = new VBox(2);
        Label lblT1 = new Label("BOLSÍN SELECCIONADO (PRECINTO)");
        lblT1.setStyle("-fx-font-size: 9.5px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        this.lblDetallePrecinto = new Label("-");
        this.lblDetallePrecinto.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        this.lblDetalleCMOrigen = new Label("CM Origen: -");
        this.lblDetalleCMOrigen.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        colBolsin.getChildren().addAll(lblT1, lblDetallePrecinto, lblDetalleCMOrigen);

        VBox colRemito = new VBox(2);
        Label lblT2 = new Label("REMITOS ADJUNTOS");
        lblT2.setStyle("-fx-font-size: 9.5px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        this.lblDetalleNumeroRemito = new Label("-");
        this.lblDetalleNumeroRemito.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        this.lblDetalleFechaEnvio = new Label("Enviado: -");
        this.lblDetalleFechaEnvio.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        colRemito.getChildren().addAll(lblT2, lblDetalleNumeroRemito, lblDetalleFechaEnvio);

        VBox colDocsInfo = new VBox(2);
        Label lblT3 = new Label("TOTAL DE DOCUMENTACIONES");
        lblT3.setStyle("-fx-font-size: 9.5px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        this.lblDetalleCantDocs = new Label("0");
        this.lblDetalleCantDocs.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
        Label lblT3Sub = new Label("Documentos declarados en los remitos");
        lblT3Sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        colDocsInfo.getChildren().addAll(lblT3, lblDetalleCantDocs, lblT3Sub);

        summaryBox.getChildren().addAll(colBolsin, new Separator(javafx.geometry.Orientation.VERTICAL),
                colRemito, new Separator(javafx.geometry.Orientation.VERTICAL), colDocsInfo);

        VBox tableDocsSection = new VBox(6);
        VBox.setVgrow(tableDocsSection, Priority.ALWAYS);
        HBox docsHeaderBox = new HBox(10);
        docsHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label lblDocsTitle = new Label("Documentación Contenida en los Remitos:");
        lblDocsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12.5px; -fx-text-fill: #1e293b;");

        this.lblAvisoDocsNoConformes = new Label("");
        this.lblAvisoDocsNoConformes.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        docsHeaderBox.getChildren().addAll(lblDocsTitle, lblAvisoDocsNoConformes);

        this.listaDocsData = FXCollections.observableArrayList();
        this.tblDocumentacion = new TableView<>(listaDocsData);
        this.tblDocumentacion.setMinHeight(145);
        this.tblDocumentacion.setPrefHeight(180);
        this.tblDocumentacion.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        this.tblDocumentacion.setEditable(true);
        VBox.setVgrow(this.tblDocumentacion, Priority.ALWAYS);

        TableColumn<DocumentacionRow, Boolean> colCheck = new TableColumn<>("¿Conforme?");
        colCheck.setCellValueFactory(cellData -> cellData.getValue().seleccionadaProperty());
        colCheck.setCellFactory(CheckBoxTableCell.forTableColumn(colCheck));
        colCheck.setPrefWidth(85);
        colCheck.setEditable(true);

        TableColumn<DocumentacionRow, Integer> colRemitoNro = new TableColumn<>("N° Remito");
        colRemitoNro.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getNumeroRemito()));
        colRemitoNro.setPrefWidth(85);

        TableColumn<DocumentacionRow, Integer> colDocNro = new TableColumn<>("N° Doc");
        colDocNro.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getNumero()));
        colDocNro.setPrefWidth(70);

        TableColumn<DocumentacionRow, String> colTipoDoc = new TableColumn<>("Tipo de Documento");
        colTipoDoc.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTipo()));
        colTipoDoc.setPrefWidth(190);

        TableColumn<DocumentacionRow, String> colAsunto = new TableColumn<>("Asunto / Carátula");
        colAsunto.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAsunto()));
        colAsunto.setPrefWidth(270);

        TableColumn<DocumentacionRow, String> colEstadoActual = new TableColumn<>("Estado Actual");
        colEstadoActual.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEstadoActual()));
        colEstadoActual.setPrefWidth(135);
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

        TableColumn<DocumentacionRow, DocumentacionRow> colEstadoResultante = new TableColumn<>("Estado Resultante Previsto");
        colEstadoResultante.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        colEstadoResultante.setPrefWidth(170);
        colEstadoResultante.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(DocumentacionRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item.getEstadoResultante());
                    aplicarEstiloBadgePorSeveridad(badge, item.getSeveridadResultante());
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        tblDocumentacion.getColumns().addAll(colCheck, colRemitoNro, colDocNro, colTipoDoc, colAsunto, colEstadoActual, colEstadoResultante);
        tableDocsSection.getChildren().addAll(docsHeaderBox, tblDocumentacion);

        Label lblOpcionesHeader = new Label("Seleccione la Opción de Recepción correspondiente:");
        lblOpcionesHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 12.5px; -fx-text-fill: #1e293b;");

        this.boxOpcionesCards = new VBox(6);
        this.grupoOpciones = new ToggleGroup();
        this.radioOpcionesList = new ArrayList<>();

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
            if (opcionSeleccionadaNumero > 0) {
                tomarSeleccionOpcion(opcionSeleccionadaNumero);
            }
        });

        actionsBar.getChildren().addAll(btnVolver, btnCancelar, btnContinuarConfirmacion);

        card.getChildren().addAll(summaryBox, tableDocsSection, lblOpcionesHeader, boxOpcionesCards, actionsBar);
        root.getChildren().add(card);
        return root;
    }

    private void construirCardsOpciones(String[][] opciones) {
        boxOpcionesCards.getChildren().clear();
        grupoOpciones.getToggles().clear();
        radioOpcionesList.clear();

        if (opciones == null || opciones.length == 0) {
            return;
        }

        for (String[] opc : opciones) {
            if (opc == null || opc.length < 7) continue;

            int numero = Integer.parseInt(opc[0]);
            String titulo = opc[1];
            String descripcion = opc[2];
            String impactoRemito = opc[3];
            String impactoDoc = opc[4];
            boolean requiereDocs = Boolean.parseBoolean(opc[5]);
            boolean habilitadaOpc = Boolean.parseBoolean(opc[6]);
            boolean deshabilitada = !habilitadaOpc;

            HBox card = new HBox(10);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("option-card");

            RadioButton rb = new RadioButton();
            rb.setToggleGroup(grupoOpciones);
            rb.setUserData(new int[]{ numero, requiereDocs ? 1 : 0 });
            rb.setDisable(deshabilitada);
            radioOpcionesList.add(rb);

            Label badgeNum = new Label("Opción " + numero);
            badgeNum.setStyle("-fx-font-weight: bold; -fx-font-size: 10.5px; -fx-text-fill: #2563eb; -fx-background-color: #eff6ff; -fx-padding: 2px 7px; -fx-background-radius: 4px;");

            VBox textBox = new VBox(1);
            HBox titleRow = new HBox(8);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            Label lblTituloOpc = new Label(titulo);
            lblTituloOpc.setStyle("-fx-font-weight: bold; -fx-font-size: 12.5px; -fx-text-fill: " + (deshabilitada ? "#94a3b8;" : "#0f172a;"));
            titleRow.getChildren().addAll(badgeNum, lblTituloOpc);

            if (deshabilitada) {
                Label badgeRestringida = new Label("Disponible únicamente en Comisión Médica Central");
                badgeRestringida.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #b91c1c; -fx-background-color: #fef2f2; -fx-padding: 1px 6px; -fx-background-radius: 4px;");
                titleRow.getChildren().add(badgeRestringida);
                card.getStyleClass().add("option-card-disabled");
            }

            Label lblDesc = new Label(descripcion);
            lblDesc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748b;");

            HBox badgesImpacto = new HBox(8);
            badgesImpacto.setPadding(new Insets(1, 0, 0, 0));
            Label tagRemito = new Label("Remito: " + impactoRemito);
            tagRemito.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #475569; -fx-background-color: #f1f5f9; -fx-padding: 1px 6px; -fx-background-radius: 4px;");
            Label tagDoc = new Label("Documentación: " + impactoDoc);
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
                    int[] data = (int[]) rb.getUserData();
                    this.opcionSeleccionadaNumero = data[0];
                    this.opcionActualRequiereDocs = (data[1] == 1);
                    btnContinuarConfirmacion.setDisable(false);
                    refrescarPrevisualizacionPaso2();
                } else {
                    card.getStyleClass().remove("option-card-selected");
                }
            });

            boxOpcionesCards.getChildren().add(card);
        }

        if (!radioOpcionesList.isEmpty()) {
            radioOpcionesList.get(0).setSelected(true);
        }
    }

    private void refrescarPrevisualizacionPaso2() {
        int[] noConformes = obtenerNumerosDocsNoConformes();
        String[][] previews = gestor.previsualizarEstadosDocumentacion(opcionSeleccionadaNumero, noConformes);

        Map<Integer, String[]> porNumero = new HashMap<>();
        if (previews != null) {
            for (String[] p : previews) {
                if (p != null && p.length >= 3) {
                    porNumero.put(Integer.parseInt(p[0]), p);
                }
            }
        }
        for (DocumentacionRow row : listaDocsData) {
            String[] prev = porNumero.get(row.getNumero());
            if (prev != null) {
                row.actualizarPrevisualizacion(prev[1], prev[2]);
            }
        }
        tblDocumentacion.refresh();

        String[] aviso = gestor.obtenerAvisoOpcion(opcionSeleccionadaNumero);
        if (lblAvisoDocsNoConformes != null && aviso != null && aviso.length >= 2) {
            lblAvisoDocsNoConformes.setText(aviso[0]);
            String color = switch (aviso[1]) {
                case "WARN" -> "#b45309";
                case "ERROR" -> "#b91c1c";
                case "INFO" -> "#1d4ed8";
                default -> "#047857";
            };
            lblAvisoDocsNoConformes.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        }
    }

    private int[] obtenerNumerosDocsNoConformes() {
        if (!opcionActualRequiereDocs) {
            return new int[0];
        }
        List<Integer> ids = new ArrayList<>();
        for (DocumentacionRow row : listaDocsData) {
            if (!row.isSeleccionada()) {
                ids.add(row.getNumero());
            }
        }
        int[] res = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            res[i] = ids.get(i);
        }
        return res;
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

        HBox confHeader = new HBox(12);
        confHeader.setAlignment(Pos.CENTER_LEFT);
        VBox titles = new VBox(2);
        Label lblTitle = new Label("Confirmación de Recepción de Bolsín");
        lblTitle.getStyleClass().add("card-title");
        Label lblSub = new Label("Verifique el resumen de cambios de estado antes de confirmar la recepción.");
        lblSub.getStyleClass().add("card-subtitle");
        titles.getChildren().addAll(lblTitle, lblSub);
        confHeader.getChildren().add(titles);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

        Label t0 = new Label("Bolsín a Recepcionar (Precinto):");
        t0.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblConfPrecinto = new Label("-");
        this.lblConfPrecinto.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");
        this.lblConfEstadoBolsin = new Label("-");
        grid.add(t0, 0, 0);
        grid.add(lblConfPrecinto, 1, 0);
        grid.add(lblConfEstadoBolsin, 2, 0);

        Label t1 = new Label("Comisión Médica Origen:");
        t1.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblConfCMOrigen = new Label("-");
        grid.add(t1, 0, 1);
        grid.add(lblConfCMOrigen, 1, 1);

        Label tOpc = new Label("Opción Seleccionada:");
        tOpc.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblConfOpcionElegida = new Label("-");
        this.lblConfOpcionElegida.setStyle("-fx-font-weight: bold; -fx-text-fill: #1d4ed8;");
        grid.add(tOpc, 0, 2);
        grid.add(lblConfOpcionElegida, 1, 2, 2, 1);

        Label t2 = new Label("Remitos Asociados:");
        t2.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.boxConfRemitosList = new VBox(4);
        grid.add(t2, 0, 3);
        grid.add(boxConfRemitosList, 1, 3, 2, 1);

        Label t3 = new Label("Empleado Responsable:");
        t3.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblConfEmpleado = new Label("-");
        this.lblConfFechaHora = new Label("-");
        this.lblConfFechaHora.setStyle("-fx-text-fill: #64748b;");
        grid.add(t3, 0, 4);
        grid.add(lblConfEmpleado, 1, 4);
        grid.add(lblConfFechaHora, 2, 4);

        Label t4 = new Label("Destinatario Notificación:");
        t4.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblConfDestinatarioCU29 = new Label("-");
        Label t4Badge = new Label("Se enviará email a la CM Origen");
        t4Badge.getStyleClass().addAll("badge", "badge-enviado");
        grid.add(t4, 0, 5);
        grid.add(lblConfDestinatarioCU29, 1, 5);
        grid.add(t4Badge, 2, 5);

        Label lblDocsSub = new Label("Detalle de Actualización de Documentaciones:");
        lblDocsSub.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");

        ScrollPane scrollDocs = new ScrollPane();
        scrollDocs.setFitToWidth(true);
        scrollDocs.setPrefHeight(150);
        scrollDocs.setStyle("-fx-background-color: transparent; -fx-border-color: #e2e8f0; -fx-border-radius: 6px;");

        this.boxConfDocsList = new VBox(8);
        this.boxConfDocsList.setPadding(new Insets(10));
        scrollDocs.setContent(boxConfDocsList);

        HBox actionsBar = new HBox(12);
        actionsBar.setAlignment(Pos.CENTER_RIGHT);

        Button btnModificar = new Button("← Modificar Selección");
        btnModificar.getStyleClass().add("btn-secondary");
        btnModificar.setOnAction(e -> irAPaso(2));

        Button btnRechazarConf = new Button("No Confirmar");
        btnRechazarConf.getStyleClass().add("btn-danger");
        btnRechazarConf.setOnAction(e -> tomarConfirmacionSeleccion(false));

        this.btnConfirmarRecepcion = new Button("✔ Confirmar Recepción y Enviar Notificación");
        this.btnConfirmarRecepcion.getStyleClass().add("btn-success");
        this.btnConfirmarRecepcion.setOnAction(e -> tomarConfirmacionSeleccion(true));

        actionsBar.getChildren().addAll(btnModificar, btnRechazarConf, btnConfirmarRecepcion);

        card.getChildren().addAll(confHeader, grid, lblDocsSub, scrollDocs, actionsBar);
        root.getChildren().add(card);
        return root;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VISTA 4: RESULTADO Y COMPROBANTE DE NOTIFICACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    private VBox crearVistaPaso4() {
        VBox root = new VBox(16);
        root.setAlignment(Pos.TOP_CENTER);

        VBox card = new VBox(14);
        card.getStyleClass().add("card-panel");
        VBox.setVgrow(card, Priority.ALWAYS);

        HBox successBanner = new HBox(16);
        successBanner.setAlignment(Pos.CENTER_LEFT);
        successBanner.getStyleClass().addAll("alert-box", "alert-box-success");
        successBanner.setPadding(new Insets(14, 16, 14, 16));

        Label lblCheck = new Label("✔");
        lblCheck.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #059669;");

        VBox bannerText = new VBox(2);
        this.lblResultadoTitulo = new Label("¡Recepción Registrada con Éxito!");
        this.lblResultadoTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #065f46;");
        this.lblResultadoSubtitulo = new Label("Los estados fueron actualizados y se transmitió la notificación por correo.");
        this.lblResultadoSubtitulo.setStyle("-fx-font-size: 13px; -fx-text-fill: #047857;");
        bannerText.getChildren().addAll(lblResultadoTitulo, lblResultadoSubtitulo);
        successBanner.getChildren().addAll(lblCheck, bannerText);

        HBox resSummaryBox = new HBox(20);
        resSummaryBox.setAlignment(Pos.CENTER_LEFT);
        resSummaryBox.setPadding(new Insets(12, 16, 12, 16));
        resSummaryBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

        VBox resColBolsin = new VBox(4);
        Label lblRBTag = new Label("BOLSÍN RECEPCIONADO (PRECINTO)");
        lblRBTag.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        HBox resBolsinRow = new HBox(8);
        resBolsinRow.setAlignment(Pos.CENTER_LEFT);
        this.lblResBolsin = new Label("-");
        this.lblResBolsin.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        this.lblResEstadoBolsin = new Label("-");
        this.lblResEstadoBolsin.getStyleClass().addAll("badge", BADGE_OK);
        resBolsinRow.getChildren().addAll(lblResBolsin, lblResEstadoBolsin);
        resColBolsin.getChildren().addAll(lblRBTag, resBolsinRow);

        VBox resColRemitos = new VBox(4);
        Label lblRRTag = new Label("REMITOS ACTUALIZADOS");
        lblRRTag.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        this.boxResRemitosList = new VBox(4);
        resColRemitos.getChildren().addAll(lblRRTag, boxResRemitosList);

        VBox resColResp = new VBox(4);
        Label lblRRespTag = new Label("RESPONSABLE Y FECHA/HORA");
        lblRRespTag.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        this.lblResEmpleadoFecha = new Label("-");
        this.lblResEmpleadoFecha.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
        resColResp.getChildren().addAll(lblRRespTag, lblResEmpleadoFecha);

        resSummaryBox.getChildren().addAll(resColBolsin, new Separator(javafx.geometry.Orientation.VERTICAL),
                resColRemitos, new Separator(javafx.geometry.Orientation.VERTICAL), resColResp);

        VBox emailCard = new VBox(10);
        emailCard.setPadding(new Insets(14, 16, 14, 16));
        emailCard.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8px; -fx-border-color: #cbd5e1; -fx-border-radius: 8px;");
        VBox.setVgrow(emailCard, Priority.ALWAYS);

        HBox emailCardHeader = new HBox(12);
        emailCardHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblEmailTag = new Label("✉ COMPROBANTE DE NOTIFICACIÓN ELECTRÓNICA");
        lblEmailTag.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0f172a;");
        Region spacerEmail = new Region();
        HBox.setHgrow(spacerEmail, Priority.ALWAYS);
        Label badgeCorreoEnviado = new Label("✔ CORREO ENVIADO EXITOSAMENTE");
        badgeCorreoEnviado.getStyleClass().addAll("badge", BADGE_OK);
        emailCardHeader.getChildren().addAll(lblEmailTag, spacerEmail, badgeCorreoEnviado);

        GridPane emailMetaGrid = new GridPane();
        emailMetaGrid.setHgap(14);
        emailMetaGrid.setVgap(6);
        emailMetaGrid.setPadding(new Insets(8, 12, 8, 12));
        emailMetaGrid.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 6px; -fx-border-color: #e2e8f0; -fx-border-radius: 6px;");

        Label lblDeKey = new Label("De:");
        lblDeKey.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        this.lblEmailDe = new Label("-");
        this.lblEmailDe.setStyle("-fx-text-fill: #0f172a;");

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
        emailMetaGrid.add(lblEmailDe, 1, 0);
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

        this.lblStatusUsuario = new Label("");
        footer.getChildren().addAll(lblStatusMensaje, lblStatusUsuario);
        return footer;
    }

    private void actualizarStatus(String mensaje) {
        if (lblStatusMensaje != null) {
            lblStatusMensaje.setText(mensaje);
        }
    }

    private static void aplicarEstiloBadgePorSeveridad(Label badge, String severidad) {
        badge.getStyleClass().removeAll("badge", BADGE_OK, BADGE_WARN, BADGE_ERROR, BADGE_INFO);
        badge.getStyleClass().add("badge");
        if ("WARN".equals(severidad)) {
            badge.getStyleClass().add(BADGE_WARN);
        } else if ("ERROR".equals(severidad)) {
            badge.getStyleClass().add(BADGE_ERROR);
        } else if ("INFO".equals(severidad)) {
            badge.getStyleClass().add(BADGE_INFO);
        } else {
            badge.getStyleClass().add(BADGE_OK);
        }
    }

    /** Modelo visual interno puramente basado en primitivos/String para Paso 1. */
    private static final class FilaBolsinVisual {
        final int precinto;
        final String cmOrigen;
        final String cmDestino;
        final String remitos;
        final String fecha;
        final String estado;

        FilaBolsinVisual(int precinto, String cmOrigen, String cmDestino, String remitos, String fecha, String estado) {
            this.precinto = precinto;
            this.cmOrigen = cmOrigen;
            this.cmDestino = cmDestino;
            this.remitos = remitos;
            this.fecha = fecha;
            this.estado = estado;
        }
    }
}
