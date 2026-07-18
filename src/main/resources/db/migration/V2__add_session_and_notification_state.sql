ALTER TABLE usuarios
    ADD COLUMN session_version integer NOT NULL DEFAULT 0,
    ADD COLUMN ultima_notificacion_vista_id bigint NOT NULL DEFAULT 0;

UPDATE usuarios
SET ultima_notificacion_vista_id = COALESCE((SELECT MAX(id) FROM auditoria), 0)
WHERE rol_id IN (
    SELECT id FROM roles WHERE nombre IN ('CCI_ADMIN', 'CCI_STAFF')
);
