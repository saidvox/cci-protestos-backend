# CCI Protestos Backend

API Spring Boot para el flujo de protestos de la Camara de Comercio de Ica.

## Desarrollo local

Requisitos: Java 21 y Docker Desktop.

```powershell
docker compose up -d postgres
$env:APP_DEMO_DATA_ENABLED="true"
.\mvnw.cmd spring-boot:run
```

La API usa PostgreSQL, Flyway y validacion del esquema de Hibernate. Swagger esta disponible localmente en `http://localhost:8080/swagger-ui.html`.

Los usuarios de demostracion solo se crean cuando `APP_DEMO_DATA_ENABLED=true`. Son `admin@demo.local`, `staff@demo.local`, `analista@demo.local` y `deudor@demo.local`, con la clave `password`.

## Pruebas

```powershell
.\mvnw.cmd test
```

## Produccion

La imagen se construye con el `Dockerfile` de la raiz y se ejecuta con el perfil `prod`. Variables obligatorias:

- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
- `JWT_SECRET` de al menos 64 bytes aleatorios
- `CORS_ALLOWED_ORIGIN`
- `APP_BOOTSTRAP_ADMIN_EMAIL` y, solo durante el primer arranque, `APP_BOOTSTRAP_ADMIN_PASSWORD`

Variables recomendadas:

- `APP_BOOTSTRAP_ADMIN_NAME`
- `STORAGE_LOCATION=/app/storage`
- `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError`

El perfil productivo fuerza cookies seguras, deshabilita datos demo y Swagger, oculta errores internos y deja el esquema exclusivamente bajo Flyway. La contrasena de bootstrap no modifica una cuenta que ya exista y debe retirarse del entorno despues del primer acceso.
