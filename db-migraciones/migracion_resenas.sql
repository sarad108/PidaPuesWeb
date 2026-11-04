-- =====================================================================
-- PidaPues — MIGRACIÓN: reseñas de clientes (NO borra tus datos)
-- ---------------------------------------------------------------------
-- Es opcional: el servidor ya crea esta tabla solo cuando arranca. Sirve si quiero crearla a mano
-- y se puede correr varias veces sin problema.
--   mysql -u root -p < src/migracion_resenas.sql
-- (las reseñas de ejemplo las mete el servidor para que las tildes no se dañen en Windows)
-- =====================================================================
USE pidapues;

CREATE TABLE IF NOT EXISTS resena (
  id_resena       INT NOT NULL AUTO_INCREMENT,
  id_pedido       INT NULL,
  nombre_publico  VARCHAR(40)  NOT NULL COMMENT 'Como se muestra en el inicio, ej. "Laura P."',
  calificacion    TINYINT      NOT NULL,
  comentario      VARCHAR(300) NULL,
  fecha           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_resena),
  UNIQUE KEY uq_resena_pedido (id_pedido),
  CONSTRAINT chk_resena_calificacion CHECK (calificacion BETWEEN 1 AND 5),
  CONSTRAINT fk_resena_pedido FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido)
    ON UPDATE CASCADE ON DELETE CASCADE,
  INDEX idx_resena_fecha (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Reseñas opcionales de los clientes (estrellas y comentario)';
