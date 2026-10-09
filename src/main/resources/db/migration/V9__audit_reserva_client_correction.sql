-- Extensión aditiva: conserva el historial previo y las restricciones de mesas.
ALTER TABLE historial_reservas ADD COLUMN cliente_anterior_id BIGINT;
ALTER TABLE historial_reservas ADD COLUMN cliente_nuevo_id BIGINT;
ALTER TABLE historial_reservas ADD COLUMN observaciones_anteriores VARCHAR(500);
ALTER TABLE historial_reservas ADD COLUMN observaciones_nuevas VARCHAR(500);
ALTER TABLE historial_reservas ADD CONSTRAINT fk_historial_cliente_anterior
    FOREIGN KEY (cliente_anterior_id) REFERENCES clientes(id);
ALTER TABLE historial_reservas ADD CONSTRAINT fk_historial_cliente_nuevo
    FOREIGN KEY (cliente_nuevo_id) REFERENCES clientes(id);

ALTER TABLE historial_reservas DROP CONSTRAINT ck_historial_tipo_evento;
ALTER TABLE historial_reservas ADD CONSTRAINT ck_historial_tipo_evento CHECK
    (tipo_evento IN ('CREACION', 'CAMBIO_ESTADO', 'CHECK_IN', 'REASIGNACION', 'CORRECCION_CLIENTE'));
ALTER TABLE historial_reservas DROP CONSTRAINT ck_historial_mesas_por_evento;
ALTER TABLE historial_reservas ADD CONSTRAINT ck_historial_mesas_por_evento CHECK (
    (tipo_evento = 'CREACION' AND mesa_anterior_id IS NULL AND mesa_nueva_id IS NOT NULL)
    OR (tipo_evento IN ('CAMBIO_ESTADO', 'CORRECCION_CLIENTE') AND mesa_anterior_id IS NULL AND mesa_nueva_id IS NULL)
    OR (tipo_evento = 'CHECK_IN' AND mesa_anterior_id IS NOT NULL AND mesa_nueva_id IS NOT NULL)
    OR (tipo_evento = 'REASIGNACION' AND mesa_anterior_id IS NOT NULL AND mesa_nueva_id IS NOT NULL AND mesa_anterior_id <> mesa_nueva_id)
);
ALTER TABLE historial_reservas ADD CONSTRAINT ck_historial_correccion_cliente CHECK (
    (tipo_evento = 'CORRECCION_CLIENTE'
      AND cliente_anterior_id IS NOT NULL AND cliente_nuevo_id IS NOT NULL
      AND estado_anterior IS NOT NULL AND estado_anterior = 'SOLICITADA' AND estado_nuevo = 'SOLICITADA'
      AND motivo IS NOT NULL AND LENGTH(TRIM(motivo)) > 0)
    OR (tipo_evento <> 'CORRECCION_CLIENTE' AND cliente_anterior_id IS NULL AND cliente_nuevo_id IS NULL
      AND observaciones_anteriores IS NULL AND observaciones_nuevas IS NULL)
);
CREATE INDEX idx_historial_cliente_anterior ON historial_reservas(cliente_anterior_id);
CREATE INDEX idx_historial_cliente_nuevo ON historial_reservas(cliente_nuevo_id);
