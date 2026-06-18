INSERT INTO roles(nombre) VALUES ('ADMIN'),('ANALISTA'),('ENTIDAD');
INSERT INTO entidades_financieras(ruc,razon_social,contacto,email) VALUES ('20111111111','Financiera Demo Ica','Contacto Demo','contacto@demo.local');
-- Contraseña ficticia para ambiente académico: password
INSERT INTO usuarios(nombre_completo,email,password_hash,rol_id) VALUES
('Administrador Demo','admin@demo.local','$2a$12$/Uiay6kagSrjQWCKTF6tyOAOPOT7n727BA3c6lE/WXQNzpMaT7igu',(SELECT id FROM roles WHERE nombre='ADMIN')),
('Analista Demo','analista@demo.local','$2a$12$/Uiay6kagSrjQWCKTF6tyOAOPOT7n727BA3c6lE/WXQNzpMaT7igu',(SELECT id FROM roles WHERE nombre='ANALISTA'));
INSERT INTO usuarios(nombre_completo,email,password_hash,rol_id,entidad_id) VALUES ('Entidad Demo','entidad@demo.local','$2a$12$/Uiay6kagSrjQWCKTF6tyOAOPOT7n727BA3c6lE/WXQNzpMaT7igu',(SELECT id FROM roles WHERE nombre='ENTIDAD'),(SELECT id FROM entidades_financieras LIMIT 1));
INSERT INTO analistas(usuario_id,codigo) VALUES ((SELECT id FROM usuarios WHERE email='analista@demo.local'),'AN-001');
INSERT INTO protestos(entidad_id,numero_documento,nombre_deudor,tipo_titulo,monto,moneda,fecha_protesto) VALUES ((SELECT id FROM entidades_financieras LIMIT 1),'00000000','Persona Ficticia','PAGARE',1250.00,'PEN',CURRENT_DATE - 30);
