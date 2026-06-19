ALTER TABLE solicitudes ADD COLUMN documento_deudor VARCHAR(30);
ALTER TABLE solicitudes ADD COLUMN monto_protestado NUMERIC(12, 2);
ALTER TABLE cargas_excel ADD COLUMN nombre_almacenado VARCHAR(255);
