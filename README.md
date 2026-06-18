# CCI Protestos Backend

API REST académica para la gestión de protestos, solicitudes, documentos, entidades financieras, analistas, reportes y auditoría.

## Requisitos

- Java 21
- PostgreSQL 15 o superior
- Maven Wrapper incluido

## Configuración

1. Cree la base `cci_protestos`.
2. Defina `JWT_SECRET` con al menos 32 caracteres aleatorios.
3. Ajuste `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `CORS_ALLOWED_ORIGIN` y `STORAGE_LOCATION` según su entorno.
4. Use `src/main/resources/application-example.properties` como referencia; no almacene secretos reales en Git.

```powershell
$env:JWT_SECRET='secreto-local-de-desarrollo-con-32-caracteres-minimo'
$env:DATABASE_URL='jdbc:postgresql://localhost:5432/cci_protestos'
.\mvnw.cmd spring-boot:run
```

Flyway crea el esquema y datos ficticios. Las cuentas demo usan `admin@demo.local`, `analista@demo.local` y `entidad@demo.local`; la contraseña académica es `password`. Cámbiela fuera de un entorno local.

## Verificación y API

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

Swagger UI: `http://localhost:8080/swagger-ui.html`. El login devuelve un JWT que debe enviarse como `Authorization: Bearer <token>`.

Los documentos se validan y almacenan localmente. En esta fase, las hojas Excel solo se validan y registran; no se importan filas de negocio.
