CREATE TABLE IF NOT EXISTS comision_medica (
    codigo INT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    direccion VARCHAR(255),
    email VARCHAR(255),
    telefono VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS tipo_documento (
    nombre VARCHAR(255) PRIMARY KEY,
    descripcion VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS estado (
    nombre VARCHAR(100) NOT NULL,
    ambito VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    PRIMARY KEY (nombre, ambito)
);

CREATE TABLE IF NOT EXISTS empleado (
    nombre VARCHAR(255) PRIMARY KEY,
    comision_medica_codigo INT NOT NULL,
    FOREIGN KEY (comision_medica_codigo) REFERENCES comision_medica (codigo)
);

CREATE TABLE IF NOT EXISTS usuario (
    nombre VARCHAR(255) PRIMARY KEY,
    descripcion VARCHAR(255),
    empleado_nombre VARCHAR(255) NOT NULL,
    FOREIGN KEY (empleado_nombre) REFERENCES empleado (nombre)
);

CREATE TABLE IF NOT EXISTS sesion (
    usuario_nombre VARCHAR(255) NOT NULL,
    fecha_hora_inicio TIMESTAMP NOT NULL,
    fecha_hora_fin TIMESTAMP NULL,
    PRIMARY KEY (usuario_nombre, fecha_hora_inicio),
    FOREIGN KEY (usuario_nombre) REFERENCES usuario (nombre)
);

CREATE TABLE IF NOT EXISTS bolsin (
    numero_bolsin INT PRIMARY KEY,
    numero_precinto INT NOT NULL UNIQUE,
    fecha TIMESTAMP NOT NULL,
    peso DOUBLE PRECISION NOT NULL,
    cm_origen_codigo INT NOT NULL,
    cm_destino_codigo INT NOT NULL,
    FOREIGN KEY (cm_origen_codigo) REFERENCES comision_medica (codigo),
    FOREIGN KEY (cm_destino_codigo) REFERENCES comision_medica (codigo)
);

CREATE TABLE IF NOT EXISTS cambio_estado_bolsin (
    bolsin_numero INT NOT NULL,
    fecha_hora_inicio TIMESTAMP NOT NULL,
    fecha_hora_fin TIMESTAMP NULL,
    estado_nombre VARCHAR(100) NOT NULL,
    estado_ambito VARCHAR(100) NOT NULL,
    empleado_nombre VARCHAR(255) NULL,
    PRIMARY KEY (bolsin_numero, fecha_hora_inicio),
    FOREIGN KEY (bolsin_numero) REFERENCES bolsin (numero_bolsin),
    FOREIGN KEY (estado_nombre, estado_ambito) REFERENCES estado (nombre, ambito),
    FOREIGN KEY (empleado_nombre) REFERENCES empleado (nombre)
);

CREATE TABLE IF NOT EXISTS remito (
    numero INT PRIMARY KEY,
    fecha TIMESTAMP NOT NULL,
    bolsin_numero INT NOT NULL,
    cm_origen_codigo INT NOT NULL,
    cm_destino_codigo INT NOT NULL,
    estado_nombre VARCHAR(100) NULL,
    estado_ambito VARCHAR(100) NULL,
    FOREIGN KEY (bolsin_numero) REFERENCES bolsin (numero_bolsin),
    FOREIGN KEY (cm_origen_codigo) REFERENCES comision_medica (codigo),
    FOREIGN KEY (cm_destino_codigo) REFERENCES comision_medica (codigo),
    FOREIGN KEY (estado_nombre, estado_ambito) REFERENCES estado (nombre, ambito)
);

CREATE TABLE IF NOT EXISTS documentacion (
    numero INT PRIMARY KEY,
    asunto VARCHAR(500) NOT NULL,
    fecha_pase TIMESTAMP NOT NULL,
    tipo_documento_nombre VARCHAR(255) NOT NULL,
    cm_origen_codigo INT NOT NULL,
    FOREIGN KEY (tipo_documento_nombre) REFERENCES tipo_documento (nombre),
    FOREIGN KEY (cm_origen_codigo) REFERENCES comision_medica (codigo)
);

CREATE TABLE IF NOT EXISTS detalle_remito (
    remito_numero INT NOT NULL,
    documentacion_numero INT NOT NULL,
    area_cmc_destino VARCHAR(255),
    PRIMARY KEY (remito_numero, documentacion_numero),
    FOREIGN KEY (remito_numero) REFERENCES remito (numero),
    FOREIGN KEY (documentacion_numero) REFERENCES documentacion (numero)
);

CREATE TABLE IF NOT EXISTS cambio_estado_documentacion (
    documentacion_numero INT NOT NULL,
    fecha_hora_inicio TIMESTAMP NOT NULL,
    fecha_hora_fin TIMESTAMP NULL,
    estado_nombre VARCHAR(100) NOT NULL,
    estado_ambito VARCHAR(100) NOT NULL,
    empleado_nombre VARCHAR(255) NULL,
    PRIMARY KEY (documentacion_numero, fecha_hora_inicio),
    FOREIGN KEY (documentacion_numero) REFERENCES documentacion (numero),
    FOREIGN KEY (estado_nombre, estado_ambito) REFERENCES estado (nombre, ambito),
    FOREIGN KEY (empleado_nombre) REFERENCES empleado (nombre)
);
