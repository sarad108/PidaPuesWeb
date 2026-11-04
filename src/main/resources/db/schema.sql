-- =====================================================================
--  PIDAPUES - ESQUEMA DE LA BASE DE DATOS
--  Motor: MySQL 8.0 o superior
-- =====================================================================
--
--  1. QUÉ ES ESTE ARCHIVO
--  ----------------------
--  Es el script DDL de la base de datos "pidapues": crea la base de
--  datos, sus tablas y sus vistas.
--
--  Sigue el modelo relacional del PPI (imagen "PidaPues - Modelo
--  relacional PPI"), con los mismos nombres de tablas y columnas.
--
--
--  2. CÓMO EJECUTARLO
--  ------------------
--  Desde la terminal:
--      mysql -u root -p < schema.sql
--
--  O desde MySQL Workbench: abrir el archivo y ejecutarlo completo.
--
--  OJO: el script primero BORRA las tablas y vistas que tengan estos
--  nombres y luego las crea de nuevo. Si ya existe una base "pidapues"
--  con datos, esos datos se pierden.
--
--  También se puede crear desde la aplicación (EsquemaService.java):
--  al arrancar, el servidor revisa si faltan tablas y las crea solo.
--      --bd=revisar   muestra qué falta, sin cambiar nada
--      --bd=init      borra y crea todo de nuevo (pide confirmación)
--      /api/salud     indica si la base tiene todo lo necesario
--
--
--  3. QUÉ CONTIENE
--  ---------------
--  TABLAS DEL MODELO DEL PPI (14)
--      Acceso y personal   rol, usuario, empleado, intento_acceso
--      Menú                categoria, producto
--      Cliente y carrito   cliente, carrito, carrito_productos
--      Operación           mesa, pedido, detalle_pedido
--      Cobro               pago, factura
--
--  TABLAS DE APOYO (3, no hacen parte del modelo del PPI)
--      negocio     datos de la empresa que salen en la factura
--                  (razón social, NIT y cuenta).
--      inventario  existencias de cada producto; se descuentan cuando
--                  se aprueba el pago.
--      resena      reseña opcional del cliente (1 a 5 estrellas y un
--                  comentario). Solo una por pedido.
--
--  VISTAS (11, al final del archivo)
--      El backend no consulta las tablas directamente para mostrar
--      datos: lee siempre de una vista según el módulo (menú, cocina,
--      mesero, administrador).
--
--
--  4. MATRIZ DE TRAZABILIDAD
--  -------------------------
--  Relaciona cada requisito con el módulo que lo usa y con las tablas
--  o vistas que lo implementan.
--
--  RF001  Registro de pedidos                          [Cliente]
--         carrito, carrito_productos, pedido, detalle_pedido
--
--  RF002  Organización de pedidos                      [Mesero]
--         vista_mesero_pedidos
--
--  RF003  Visualización de ordenes en cocina           [Cocina]
--         vista_cocina_pedidos, vista_detalle_pedido
--
--  RF004  Actualización de estado                      [Cocina]
--         pedido.estado, hora_inicio_prep, hora_fin, id_cocina
--
--  RF005  Visualización de pedidos del cliente         [Cliente]
--         vista_pedidos_completos, vista_detalle_pedido
--
--  RF006  Registro de pago del pedido                  [Cliente]
--         pago, factura
--
--  RF007  Notificación de pago a cocina                [Cocina]
--         vista_cocina_pedidos (solo pedidos con pago aprobado)
--
--  RF008  Reportes de ventas                           [Admin]
--         vista_admin_historial
--
--  RF009  Consulta de tiempos de servicio              [Admin]
--         vista_admin_tiempos_servicio
--
--  RF010  Registro histórico de transacciones          [Admin]
--         vista_admin_historial
--
--  RF011  Gestión de mesas                             [Anfitrión]
--         mesa, vista_admin_mesas
--
--  NF004  Seguridad y control de acceso                [Todos]
--         usuario, empleado, rol, intento_acceso
--
--  Adicionales (no están en la lista de requisitos):
--
--  Reportes de ventas e inventario                     [Admin]
--         vista_reporte_pagos, vista_reporte_ventas_producto,
--         vista_reporte_inventario, inventario
--
--  Reseñas del cliente (opcional)                      [Cliente]
--         resena (se deja al ver la factura; endpoint GET /api/resenas)
--
-- =====================================================================

CREATE DATABASE IF NOT EXISTS pidapues
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE pidapues;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Vistas primero (dependen de las tablas) y luego tablas. Incluye las
-- tablas de la versión anterior del esquema (pedidos, pedido_items,
-- mesas, productos, intentos_acceso) para poder migrar re-ejecutando
-- este script. OJO: borra los datos existentes.
DROP VIEW IF EXISTS vista_cocina_pedidos;
DROP VIEW IF EXISTS vista_mesero_pedidos;
DROP VIEW IF EXISTS vista_admin_mesas;
DROP VIEW IF EXISTS vista_admin_historial;
DROP VIEW IF EXISTS vista_admin_tiempos_servicio;
DROP VIEW IF EXISTS vista_pedidos_completos;
DROP VIEW IF EXISTS vista_detalle_pedido;
DROP VIEW IF EXISTS vista_productos_menu;
DROP VIEW IF EXISTS vista_reporte_pagos;
DROP VIEW IF EXISTS vista_reporte_ventas_producto;
DROP VIEW IF EXISTS vista_reporte_inventario;

DROP TABLE IF EXISTS pedido_items;
DROP TABLE IF EXISTS pedidos;
DROP TABLE IF EXISTS mesas;
DROP TABLE IF EXISTS productos;
DROP TABLE IF EXISTS intentos_acceso;

DROP TABLE IF EXISTS resena;
DROP TABLE IF EXISTS factura;
DROP TABLE IF EXISTS pago;
DROP TABLE IF EXISTS detalle_pedido;
DROP TABLE IF EXISTS pedido;
DROP TABLE IF EXISTS carrito_productos;
DROP TABLE IF EXISTS carrito;
DROP TABLE IF EXISTS mesa;
DROP TABLE IF EXISTS inventario;
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS categoria;
DROP TABLE IF EXISTS cliente;
DROP TABLE IF EXISTS intento_acceso;
DROP TABLE IF EXISTS empleado;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS rol;
DROP TABLE IF EXISTS negocio;

-- ---------------------------------------------------------------------
-- ACCESO Y PERSONAL
-- ---------------------------------------------------------------------
CREATE TABLE rol (
  id_rol      INT NOT NULL AUTO_INCREMENT,
  nombre_rol  VARCHAR(25) NOT NULL,
  PRIMARY KEY (id_rol),
  UNIQUE KEY uq_rol_nombre (nombre_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Roles del personal: Cocina, Mesero, Administrador';

CREATE TABLE usuario (
  id_usuario         INT NOT NULL AUTO_INCREMENT,
  nombre_usuario     VARCHAR(25)  NOT NULL,
  contrasena         VARCHAR(255) NOT NULL COMMENT 'Hash. El valor ! significa "sin contraseña: entra solo con PIN"',
  pin                VARCHAR(255) NOT NULL COMMENT 'Hash scrypt del PIN (nunca el PIN en texto plano)',
  fecha_registro     DATE         NOT NULL,
  correo_electronico VARCHAR(50)  NOT NULL,
  PRIMARY KEY (id_usuario),
  UNIQUE KEY uq_usuario_nombre (nombre_usuario),
  UNIQUE KEY uq_usuario_correo (correo_electronico)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Credenciales de acceso al sistema';

CREATE TABLE empleado (
  id_empleado        INT NOT NULL AUTO_INCREMENT,
  id_usuario         INT NOT NULL,
  id_rol             INT NOT NULL,
  primer_nombre      VARCHAR(25) NOT NULL,
  primer_apellido    VARCHAR(25) NOT NULL,
  cedula             VARCHAR(15) NOT NULL,
  correo_electronico VARCHAR(50) NOT NULL,
  fecha_nacimiento   DATE NULL,
  genero             VARCHAR(15) NULL,
  activo             TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id_empleado),
  UNIQUE KEY uq_empleado_usuario (id_usuario),      -- USUARIO 1 : 0..1 EMPLEADO
  UNIQUE KEY uq_empleado_cedula (cedula),
  CONSTRAINT fk_empleado_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_empleado_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  INDEX idx_empleado_rol_activo (id_rol, activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Personal del restaurante (mesero, cocina, administrador)';

CREATE TABLE intento_acceso (
  id_intento  INT NOT NULL AUTO_INCREMENT,
  id_usuario  INT NOT NULL,
  fecha_hora  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  exitoso     TINYINT(1) NOT NULL,
  PRIMARY KEY (id_intento),
  CONSTRAINT fk_intento_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
    ON UPDATE CASCADE ON DELETE CASCADE,
  INDEX idx_intento_usuario_fecha (id_usuario, fecha_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Bitácora de intentos de acceso (NF004): fecha/hora y si fue exitoso';

-- ---------------------------------------------------------------------
-- MENÚ
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
  id_categoria      INT NOT NULL AUTO_INCREMENT,
  nombre_categoria  VARCHAR(25) NOT NULL,
  PRIMARY KEY (id_categoria),
  UNIQUE KEY uq_categoria_nombre (nombre_categoria)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Categorías del menú (comida, bebida, postre)';

CREATE TABLE producto (
  id_producto      INT NOT NULL AUTO_INCREMENT,
  nombre_producto  VARCHAR(25)   NOT NULL,
  id_categoria     INT           NOT NULL,
  descripcion      VARCHAR(100)  NOT NULL,
  precio_unidad    DECIMAL(10,2) NOT NULL COMMENT 'Precio en COP',
  disponible       TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '1 = visible en el menú, 0 = oculto',
  url_imagen       VARCHAR(255)  NULL,
  PRIMARY KEY (id_producto),
  CONSTRAINT chk_producto_precio CHECK (precio_unidad >= 0),
  CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  INDEX idx_producto_categoria_disp (id_categoria, disponible)   -- CATEGORIA 1 : N PRODUCTO
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Catálogo de productos del menú';

-- ---------------------------------------------------------------------
-- CLIENTE, MESA Y CARRITO
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
  id_cliente         INT NOT NULL AUTO_INCREMENT,
  nombre             VARCHAR(25) NOT NULL,
  primer_apellido    VARCHAR(25) NULL,
  segundo_apellido   VARCHAR(25) NULL,
  correo_electronico VARCHAR(50) NULL,
  cedula             VARCHAR(15) NULL,
  PRIMARY KEY (id_cliente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Clientes. El id 1 es "Consumidor final" (cliente que no deja datos)';

-- estado de la mesa (RF011):
--   libre   -> disponible para un nuevo pedido
--   pago    -> hay un pedido creado cuyo pago aún no se verifica
--   ocupada -> pago verificado y pedido en curso
CREATE TABLE mesa (
  id_mesa     INT NOT NULL AUTO_INCREMENT,
  num_mesa    INT NOT NULL,
  id_usuario  INT NULL COMMENT 'Usuario de la tableta de la mesa (opcional)',
  capacidad   INT NOT NULL DEFAULT 4,
  estado      VARCHAR(25) NOT NULL DEFAULT 'libre',
  PRIMARY KEY (id_mesa),
  UNIQUE KEY uq_mesa_num (num_mesa),
  UNIQUE KEY uq_mesa_usuario (id_usuario),           -- USUARIO 1 : 0..1 MESA
  CONSTRAINT chk_mesa_estado CHECK (estado IN ('libre','pago','ocupada')),
  CONSTRAINT chk_mesa_capacidad CHECK (capacidad > 0),
  CONSTRAINT fk_mesa_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
    ON UPDATE CASCADE ON DELETE SET NULL,
  INDEX idx_mesa_estado (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Mesas físicas del restaurante y su estado de ocupación';

-- Carrito del cliente en una mesa. Existe desde que el cliente pasa a
-- pagar hasta que el pago se verifica (ahí se convierte en pedido y se
-- elimina). id_mesa es único (MESA 1 : 0..1 CARRITO). id_cliente NO es
-- único porque "Consumidor final" (id 1) lo comparten varias mesas.
CREATE TABLE carrito (
  id_carrito      INT NOT NULL AUTO_INCREMENT,
  id_mesa         INT NOT NULL,
  id_cliente      INT NOT NULL,
  resumen_precio  DECIMAL(10,2) NOT NULL DEFAULT 0,
  cantidad        INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id_carrito),
  UNIQUE KEY uq_carrito_mesa (id_mesa),
  CONSTRAINT fk_carrito_mesa FOREIGN KEY (id_mesa) REFERENCES mesa(id_mesa)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_carrito_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
    ON UPDATE CASCADE ON DELETE CASCADE,
  INDEX idx_carrito_cliente (id_cliente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Carrito de compras por mesa (resumen de precio y cantidad total)';

CREATE TABLE carrito_productos (
  id_carrito   INT NOT NULL,
  id_producto  INT NOT NULL,
  cantidad     INT NOT NULL,
  PRIMARY KEY (id_carrito, id_producto),
  CONSTRAINT chk_carrito_productos_cantidad CHECK (cantidad > 0),
  CONSTRAINT fk_cp_carrito FOREIGN KEY (id_carrito) REFERENCES carrito(id_carrito)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_cp_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
    ON UPDATE CASCADE ON DELETE RESTRICT        -- CARRITO 1 : N CARRITO_PRODUCTOS
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Productos y cantidades dentro de cada carrito';

-- ---------------------------------------------------------------------
-- PEDIDO Y DETALLE
-- ---------------------------------------------------------------------
-- estado del pedido:
--   pendiente_pago  -> creado, pago sin verificar (NO es visible en cocina)
--   pendiente       -> pago aprobado, en cola de cocina
--   en_preparacion  -> cocina lo está preparando      (hora_inicio_prep, id_cocina)
--   listo           -> cocina terminó                 (hora_fin)
--   entregado       -> el mesero lo entregó           (fecha_entrega, id_mesero)
CREATE TABLE pedido (
  id_pedido         INT NOT NULL AUTO_INCREMENT,
  id_cliente        INT NOT NULL,
  id_mesa           INT NOT NULL,
  id_mesero         INT NULL COMMENT 'Empleado que entregó el pedido',
  id_cocina         INT NULL COMMENT 'Empleado de cocina que lo preparó',
  fecha_pedido      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  hora_inicio_prep  DATETIME NULL,
  hora_fin          DATETIME NULL,
  fecha_entrega     DATETIME NULL,
  total             DECIMAL(10,2) NOT NULL,
  estado            VARCHAR(25) NOT NULL DEFAULT 'pendiente_pago',
  PRIMARY KEY (id_pedido),
  CONSTRAINT chk_pedido_total CHECK (total >= 0),
  CONSTRAINT chk_pedido_estado CHECK (estado IN ('pendiente_pago','pendiente','en_preparacion','listo','entregado')),
  CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
    ON UPDATE CASCADE ON DELETE RESTRICT,                  -- CLIENTE 1 : 0..N PEDIDO
  CONSTRAINT fk_pedido_mesa FOREIGN KEY (id_mesa) REFERENCES mesa(id_mesa)
    ON UPDATE CASCADE ON DELETE RESTRICT,                  -- MESA 1 : N PEDIDO
  CONSTRAINT fk_pedido_mesero FOREIGN KEY (id_mesero) REFERENCES empleado(id_empleado)
    ON UPDATE CASCADE ON DELETE SET NULL,                  -- EMPLEADO 1 : 0..N (id_mesero)
  CONSTRAINT fk_pedido_cocina FOREIGN KEY (id_cocina) REFERENCES empleado(id_empleado)
    ON UPDATE CASCADE ON DELETE SET NULL,                  -- EMPLEADO 1 : 0..N (id_cocina)
  INDEX idx_pedido_estado (estado),
  INDEX idx_pedido_mesa (id_mesa),
  INDEX idx_pedido_fecha (fecha_pedido),
  INDEX idx_pedido_mesero (id_mesero),
  INDEX idx_pedido_cocina (id_cocina)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Pedidos por mesa, con su ciclo de vida y los empleados que los atendieron';

CREATE TABLE detalle_pedido (
  id_detalle_pedido  INT NOT NULL AUTO_INCREMENT,
  id_pedido          INT NOT NULL,
  id_producto        INT NOT NULL,
  observaciones      VARCHAR(100) NULL,
  cantidad           INT NOT NULL,
  precio_unitario    DECIMAL(10,2) NOT NULL COMMENT 'Precio al momento del pedido',
  subtotal           DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (id_detalle_pedido),
  CONSTRAINT chk_detalle_cantidad CHECK (cantidad > 0),
  CONSTRAINT fk_detalle_pedido FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido)
    ON UPDATE CASCADE ON DELETE CASCADE,                   -- PEDIDO 1 : N DETALLE_PEDIDO
  CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
    ON UPDATE CASCADE ON DELETE RESTRICT,                  -- PRODUCTO 1 : N DETALLE_PEDIDO
  INDEX idx_detalle_pedido (id_pedido),
  INDEX idx_detalle_producto (id_producto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Productos, cantidades y observaciones de cada pedido';

-- ---------------------------------------------------------------------
-- PAGO Y FACTURA
-- ---------------------------------------------------------------------
-- estado_pago: pendiente -> aprobado (o rechazado)
CREATE TABLE pago (
  id_pago              INT NOT NULL AUTO_INCREMENT,
  id_pedido            INT NOT NULL,
  metodo_pago          VARCHAR(25) NOT NULL,
  valor_total          DECIMAL(10,2) NOT NULL,
  estado_pago          VARCHAR(25) NOT NULL DEFAULT 'pendiente',
  fecha_hora           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Último movimiento del pago; al aprobarse, la fecha del pago',
  referencia_pasarela  VARCHAR(50) NULL,
  PRIMARY KEY (id_pago),
  UNIQUE KEY uq_pago_pedido (id_pedido),                   -- PEDIDO 1 : 0..1 PAGO
  CONSTRAINT chk_pago_metodo CHECK (metodo_pago IN ('efectivo','tarjeta','qr')),
  CONSTRAINT chk_pago_estado CHECK (estado_pago IN ('pendiente','aprobado','rechazado')),
  CONSTRAINT chk_pago_valor CHECK (valor_total >= 0),
  CONSTRAINT fk_pago_pedido FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido)
    ON UPDATE CASCADE ON DELETE CASCADE,
  INDEX idx_pago_estado (estado_pago)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Pago de cada pedido (método, estado y referencia de la pasarela)';

CREATE TABLE factura (
  id_factura         INT NOT NULL AUTO_INCREMENT,
  id_pago            INT NOT NULL,
  fecha              DATE NOT NULL,
  nombre_cliente     VARCHAR(60)   NOT NULL,
  documento_cliente  VARCHAR(15)   NOT NULL,
  correo_cliente     VARCHAR(50)   NULL,
  subtotal           DECIMAL(10,2) NOT NULL,
  impuestos          DECIMAL(10,2) NOT NULL DEFAULT 0,
  descuento          DECIMAL(10,2) NOT NULL DEFAULT 0,
  total              DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (id_factura),
  UNIQUE KEY uq_factura_pago (id_pago),                    -- PAGO 1 : 0..1 FACTURA
  CONSTRAINT fk_factura_pago FOREIGN KEY (id_pago) REFERENCES pago(id_pago)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Factura electrónica generada al aprobarse el pago';

-- ---------------------------------------------------------------------
-- Tabla de apoyo (no está en el modelo relacional): reseñas de los clientes.
-- Es opcional, se deja al ver la factura (1 a 5 estrellas y un comentario).
-- Solo una reseña por pedido. id_pedido queda en NULL en las reseñas de ejemplo
-- que crea el servidor cuando la tabla está vacía.
-- ---------------------------------------------------------------------
CREATE TABLE resena (
  id_resena       INT NOT NULL AUTO_INCREMENT,
  id_pedido       INT NULL,
  nombre_publico  VARCHAR(40)  NOT NULL COMMENT 'Como se muestra en el inicio, ej. "Laura P."',
  calificacion    TINYINT      NOT NULL,
  comentario      VARCHAR(300) NULL,
  fecha           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_resena),
  UNIQUE KEY uq_resena_pedido (id_pedido),                 -- PEDIDO 1 : 0..1 RESENA
  CONSTRAINT chk_resena_calificacion CHECK (calificacion BETWEEN 1 AND 5),
  CONSTRAINT fk_resena_pedido FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido)
    ON UPDATE CASCADE ON DELETE CASCADE,
  INDEX idx_resena_fecha (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Reseñas opcionales de los clientes (estrellas y comentario)';

-- ---------------------------------------------------------------------
-- Tabla de apoyo (NO está en el modelo relacional): existencias por producto.
-- Se descuenta al aprobarse el pago de un pedido y alimenta el reporte de
-- inventario del panel de administración.
-- ---------------------------------------------------------------------
CREATE TABLE inventario (
  id_producto    INT NOT NULL,
  stock_actual   INT NOT NULL DEFAULT 0,
  stock_minimo   INT NOT NULL DEFAULT 10 COMMENT 'Debajo de este valor el reporte marca "bajo mínimo"',
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id_producto),
  CONSTRAINT chk_inventario_stock CHECK (stock_actual >= 0 AND stock_minimo >= 0),
  CONSTRAINT fk_inventario_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
    ON UPDATE CASCADE ON DELETE CASCADE                    -- PRODUCTO 1 : 0..1 INVENTARIO
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Existencias y stock mínimo de cada producto';

-- ---------------------------------------------------------------------
-- Tabla de apoyo (NO está en el modelo relacional): datos fiscales del
-- negocio que se imprimen en la factura y se muestran al pagar.
-- ---------------------------------------------------------------------
CREATE TABLE negocio (
  id              TINYINT UNSIGNED NOT NULL DEFAULT 1,
  razon_social    VARCHAR(120) NOT NULL,
  nit             VARCHAR(40)  NOT NULL,
  direccion       VARCHAR(160) NOT NULL,
  regimen         VARCHAR(80)  NOT NULL,
  banco           VARCHAR(80)  NOT NULL,
  tipo_cuenta     VARCHAR(40)  NOT NULL,
  numero_cuenta   VARCHAR(60)  NOT NULL,
  titular_cuenta  VARCHAR(120) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT chk_negocio_id_unico CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Datos fiscales del restaurante (fila única)';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- Datos semilla
-- =====================================================================

INSERT INTO rol (id_rol, nombre_rol) VALUES
  (1, 'Cocina'), (2, 'Mesero'), (3, 'Administrador');

-- Cuentas de módulo: el sistema entra por PIN (uno por módulo). El PIN
-- real se toma de .env (PIN_COCINA, PIN_MESERO, PIN_ADMIN) y el servidor
-- lo guarda hasheado en usuario.pin cada vez que arranca, por eso aquí
-- va solo un marcador. Reemplaza estas filas por empleados reales cuando
-- quieras saber qué persona atendió cada pedido (cada uno con su PIN).
INSERT INTO usuario (id_usuario, nombre_usuario, contrasena, pin, fecha_registro, correo_electronico) VALUES
  (1, 'cocina', '!', 'PENDIENTE', CURDATE(), 'cocina@pidapues.local'),
  (2, 'mesero', '!', 'PENDIENTE', CURDATE(), 'mesero@pidapues.local'),
  (3, 'admin',  '!', 'PENDIENTE', CURDATE(), 'admin@pidapues.local');

INSERT INTO empleado (id_empleado, id_usuario, id_rol, primer_nombre, primer_apellido, cedula, correo_electronico, fecha_nacimiento, genero, activo) VALUES
  (1, 1, 1, 'Cocina',        'PidaPues', '1000000001', 'cocina@pidapues.local', NULL, NULL, 1),
  (2, 2, 2, 'Mesero',        'PidaPues', '1000000002', 'mesero@pidapues.local', NULL, NULL, 1),
  (3, 3, 3, 'Administrador', 'PidaPues', '1000000003', 'admin@pidapues.local',  NULL, NULL, 1);

INSERT INTO categoria (id_categoria, nombre_categoria) VALUES
  (1, 'Comida'), (2, 'Bebida'), (3, 'Postre');

INSERT INTO producto (id_producto, nombre_producto, id_categoria, descripcion, precio_unidad, disponible, url_imagen) VALUES
  (1, 'Hamburguesa Clasica', 1, 'Carne a la parrilla, queso cheddar, lechuga, tomate y salsa de la casa en pan brioche tostado.', 18000, 1, 'img/productos/hamburguesa.jpg'),
  (2, 'Pizza Pepperoni',     1, 'Masa artesanal horneada en piedra, salsa de tomate casera, doble mozzarella y pepperoni crocante.', 25000, 1, 'img/productos/pizza.jpg'),
  (3, 'Salchipapa',          1, 'Papa a la francesa, trozos de salchicha, nuestras 3 salsas de la casa y queso fundido.', 20000, 1, 'img/productos/salchipapa.jpg'),
  (4, 'Perro Caliente',      1, 'Salchicha jugosa, papas hilo, salsas de la casa (rosada, mostaza y BBQ) y queso rallado.', 15000, 1, 'img/productos/perro.jpg'),
  (5, 'Alitas BBQ',          1, '8 alitas de pollo horneadas con salsa BBQ ahumada, acompañadas de papas a la francesa.', 22000, 1, 'img/productos/alitas.jpg'),
  (6, 'Gaseosa 400ml',       2, 'A elegir entre Cola, Manzana o Uva, bien fría para acompañar tu pedido.', 5000, 1, 'img/productos/gaseosa.jpg'),
  (7, 'Limonada Natural',    2, 'Limonada fresca preparada al momento con limones naturales, endulzada al gusto.', 6000, 1, 'img/productos/limonada.jpg'),
  (8, 'Brownie con Helado',  3, 'Brownie tibio de chocolate servido con una bola de helado de vainilla.', 12000, 1, 'img/productos/brownie.jpg');

-- Existencias iniciales (ajústalas desde el panel de administración).
INSERT INTO inventario (id_producto, stock_actual, stock_minimo)
SELECT id_producto, 50, 10 FROM producto;

-- Cliente genérico para quien no deja sus datos (documento 222222222222).
INSERT INTO cliente (id_cliente, nombre, primer_apellido, segundo_apellido, correo_electronico, cedula)
VALUES (1, 'Consumidor final', NULL, NULL, NULL, '222222222222');

INSERT INTO mesa (num_mesa, capacidad, estado) VALUES
  (1, 4, 'libre'), (2, 4, 'libre'), (3, 4, 'libre'), (4, 4, 'libre'),
  (5, 4, 'libre'), (6, 4, 'libre'), (7, 4, 'libre'), (8, 4, 'libre'),
  (9, 4, 'libre'), (10, 4, 'libre'), (11, 4, 'libre'), (12, 4, 'libre');

INSERT INTO negocio (id, razon_social, nit, direccion, regimen, banco, tipo_cuenta, numero_cuenta, titular_cuenta)
VALUES (1, 'PidaPues S.A.S.', '900.123.456-7', 'Cra. 45 #26-85, Medellín, Antioquia', 'Responsable de IVA',
        'Bancolombia', 'Cuenta de Ahorros', '123-456789-01', 'PidaPues S.A.S.');

-- =====================================================================
-- VISTAS (las que consulta el backend)
-- =====================================================================

-- Menú del cliente: productos disponibles con el nombre de su categoría.
CREATE VIEW vista_productos_menu AS
SELECT
  pr.id_producto,
  c.nombre_categoria,
  pr.nombre_producto,
  pr.descripcion,
  pr.precio_unidad,
  pr.url_imagen,
  i.stock_actual
FROM producto pr
JOIN categoria c        ON c.id_categoria = pr.id_categoria
LEFT JOIN inventario i  ON i.id_producto = pr.id_producto
WHERE pr.disponible = 1
ORDER BY pr.id_producto;

-- Líneas de cada pedido con el nombre del producto (cocina, mesero, admin, factura).
CREATE VIEW vista_detalle_pedido AS
SELECT
  d.id_detalle_pedido,
  d.id_pedido,
  d.id_producto,
  pr.nombre_producto,
  d.cantidad,
  d.precio_unitario,
  d.subtotal,
  d.observaciones
FROM detalle_pedido d
JOIN producto pr ON pr.id_producto = d.id_producto;

-- Vista base: un renglón por pedido con mesa, pago y factura ya unidos.
-- pagado = 1 solo cuando el pago está aprobado (RF006/RF007).
CREATE VIEW vista_pedidos_completos AS
SELECT
  p.id_pedido,
  p.id_mesa,
  m.num_mesa,
  p.id_cliente,
  p.id_mesero,
  p.id_cocina,
  p.estado,
  p.total,
  p.fecha_pedido,
  p.hora_inicio_prep,
  p.hora_fin,
  p.fecha_entrega,
  TIMESTAMPDIFF(MINUTE, p.fecha_pedido, p.fecha_entrega) AS minutos_totales,
  pg.id_pago,
  pg.metodo_pago,
  pg.estado_pago,
  pg.fecha_hora AS fecha_pago,
  pg.referencia_pasarela,
  IF(pg.estado_pago = 'aprobado', 1, 0) AS pagado,
  f.id_factura,
  CONCAT('FE-', 5000 + f.id_factura) AS factura_numero,
  f.fecha AS factura_fecha,
  f.nombre_cliente,
  f.documento_cliente,
  f.correo_cliente,
  f.subtotal AS factura_subtotal,
  f.impuestos AS factura_impuestos,
  f.descuento AS factura_descuento,
  f.total AS factura_total
FROM pedido p
JOIN mesa m          ON m.id_mesa = p.id_mesa
LEFT JOIN pago pg    ON pg.id_pedido = p.id_pedido
LEFT JOIN factura f  ON f.id_pago = pg.id_pago;

-- RF003/RF004/RF007 — Cocina: solo pedidos con pago aprobado, en cola,
-- en preparación o listos (nunca "pendiente_pago" ni "entregado").
CREATE VIEW vista_cocina_pedidos AS
SELECT *
FROM vista_pedidos_completos
WHERE pagado = 1 AND estado IN ('pendiente', 'en_preparacion', 'listo')
ORDER BY fecha_pedido ASC;

-- RF002 — Mesero: pedidos que cocina dejó listos para llevar a la mesa.
CREATE VIEW vista_mesero_pedidos AS
SELECT *
FROM vista_pedidos_completos
WHERE pagado = 1 AND estado = 'listo'
ORDER BY hora_fin ASC;

-- RF008/RF010 — Admin: historial completo (ventas y transacciones).
CREATE VIEW vista_admin_historial AS
SELECT *
FROM vista_pedidos_completos
ORDER BY fecha_pedido DESC;

-- RF009 — Admin: tiempos de servicio de los pedidos pagados.
CREATE VIEW vista_admin_tiempos_servicio AS
SELECT
  id_pedido, num_mesa,
  fecha_pedido, hora_inicio_prep, hora_fin, fecha_entrega,
  minutos_totales
FROM vista_pedidos_completos
WHERE pagado = 1
ORDER BY fecha_pedido DESC;

-- RF011 — Estado de las mesas. El pedido activo de una mesa que no está
-- libre es el último que se creó en ella, y su última interacción es el
-- movimiento más reciente de ese pedido (creación, pago, inicio de
-- preparación o fin de preparación). Con eso el servidor libera las
-- mesas tras 30 minutos sin actividad.
CREATE VIEW vista_admin_mesas AS
SELECT
  m.id_mesa,
  m.num_mesa,
  m.capacidad,
  m.estado,
  CASE WHEN m.estado = 'libre' THEN NULL ELSE
    (SELECT p.id_pedido FROM pedido p
      WHERE p.id_mesa = m.id_mesa
      ORDER BY p.id_pedido DESC LIMIT 1)
  END AS id_pedido,
  CASE WHEN m.estado = 'libre' THEN NULL ELSE
    (SELECT GREATEST(
              p.fecha_pedido,
              IFNULL(p.hora_inicio_prep, p.fecha_pedido),
              IFNULL(p.hora_fin, p.fecha_pedido),
              IFNULL(pg.fecha_hora, p.fecha_pedido))
       FROM pedido p
       LEFT JOIN pago pg ON pg.id_pedido = p.id_pedido
      WHERE p.id_mesa = m.id_mesa
      ORDER BY p.id_pedido DESC LIMIT 1)
  END AS ultima_interaccion
FROM mesa m
ORDER BY m.num_mesa;

-- =====================================================================
-- VISTAS DE REPORTES (panel de administración: ventas e inventario)
-- =====================================================================

-- Un renglón por pago aprobado (ventas por día y por método de pago).
CREATE VIEW vista_reporte_pagos AS
SELECT
  pg.id_pedido,
  DATE(pg.fecha_hora) AS fecha,
  pg.metodo_pago,
  pg.valor_total
FROM pago pg
WHERE pg.estado_pago = 'aprobado';

-- Un renglón por producto vendido en pedidos con pago aprobado.
CREATE VIEW vista_reporte_ventas_producto AS
SELECT
  DATE(pg.fecha_hora) AS fecha,
  d.id_pedido,
  d.id_producto,
  pr.nombre_producto,
  c.nombre_categoria,
  d.cantidad,
  d.subtotal
FROM detalle_pedido d
JOIN pago pg      ON pg.id_pedido = d.id_pedido AND pg.estado_pago = 'aprobado'
JOIN producto pr  ON pr.id_producto = d.id_producto
JOIN categoria c  ON c.id_categoria = pr.id_categoria;

-- Existencias por producto y si están por debajo del mínimo.
CREATE VIEW vista_reporte_inventario AS
SELECT
  pr.id_producto,
  pr.nombre_producto,
  c.nombre_categoria,
  pr.disponible,
  IFNULL(i.stock_actual, 0) AS stock_actual,
  IFNULL(i.stock_minimo, 0) AS stock_minimo,
  IF(IFNULL(i.stock_actual, 0) <= IFNULL(i.stock_minimo, 0), 1, 0) AS bajo_minimo
FROM producto pr
JOIN categoria c        ON c.id_categoria = pr.id_categoria
LEFT JOIN inventario i  ON i.id_producto = pr.id_producto
ORDER BY pr.id_producto;