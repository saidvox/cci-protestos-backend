INSERT INTO roles(nombre) VALUES ('ADMIN'),('ANALISTA'),('ENTIDAD');
INSERT INTO entidades_financieras(ruc,razon_social,contacto,email) VALUES ('20111111111','Financiera Demo Ica','Contacto Demo','contacto@demo.local');
INSERT INTO protestos(entidad_id,numero_documento,nombre_deudor,tipo_titulo,monto,moneda,fecha_protesto) VALUES ((SELECT id FROM entidades_financieras LIMIT 1),'00000000','Persona Ficticia','PAGARE',1250.00,'PEN',CURRENT_DATE - 30);

