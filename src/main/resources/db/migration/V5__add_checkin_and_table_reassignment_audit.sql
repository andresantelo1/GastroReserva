ALTER TABLE mesas
    ADD CONSTRAINT ck_mesas_estado_operativo
        CHECK (estado IN ('DISPONIBLE', 'OCUPADA'));

ALTER TABLE historial_reservas
    ADD COLUMN tipo_evento VARCHAR(30);
ALTER TABLE historial_reservas
    ADD COLUMN mesa_anterior_id BIGINT;
ALTER TABLE historial_reservas
    ADD COLUMN mesa_nueva_id BIGINT;

UPDATE historial_reservas
SET tipo_evento = CASE
    WHEN estado_anterior IS NULL THEN 'CREACION'
    ELSE 'CAMBIO_ESTADO'
END;

UPDATE historial_reservas h
SET mesa_nueva_id = (
    SELECT r.mesa_id
    FROM reservas r
    WHERE r.id = h.reserva_id
)
WHERE h.tipo_evento = 'CREACION';

ALTER TABLE historial_reservas
    ALTER COLUMN tipo_evento SET NOT NULL;
ALTER TABLE historial_reservas
    ADD CONSTRAINT fk_historial_mesa_anterior
        FOREIGN KEY (mesa_anterior_id) REFERENCES mesas (id);
ALTER TABLE historial_reservas
    ADD CONSTRAINT fk_historial_mesa_nueva
        FOREIGN KEY (mesa_nueva_id) REFERENCES mesas (id);
ALTER TABLE historial_reservas
    ADD CONSTRAINT ck_historial_tipo_evento
        CHECK (tipo_evento IN ('CREACION', 'CAMBIO_ESTADO', 'CHECK_IN', 'REASIGNACION'));
ALTER TABLE historial_reservas
    ADD CONSTRAINT ck_historial_mesas_por_evento
        CHECK (
            (tipo_evento = 'CREACION'
                AND mesa_anterior_id IS NULL
                AND mesa_nueva_id IS NOT NULL)
            OR (tipo_evento = 'CAMBIO_ESTADO'
                AND mesa_anterior_id IS NULL
                AND mesa_nueva_id IS NULL)
            OR (tipo_evento = 'CHECK_IN'
                AND mesa_anterior_id IS NOT NULL
                AND mesa_nueva_id IS NOT NULL)
            OR (tipo_evento = 'REASIGNACION'
                AND mesa_anterior_id IS NOT NULL
                AND mesa_nueva_id IS NOT NULL
                AND mesa_anterior_id <> mesa_nueva_id)
        );

CREATE INDEX idx_historial_mesa_anterior
    ON historial_reservas (mesa_anterior_id);
CREATE INDEX idx_historial_mesa_nueva
    ON historial_reservas (mesa_nueva_id);
