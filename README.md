# CCI Protestos Backend

API Spring Boot para el flujo de protestos de la Camara de Comercio de Ica.

## Ejecutar localmente

Requisitos:

- Java 21
- Docker Desktop

Configuracion por defecto:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cci_protestos
spring.datasource.username=cci_user
spring.datasource.password=cci_password
spring.jpa.hibernate.ddl-auto=update
```

Comandos:

```bash
docker compose up -d postgres
./mvnw spring-boot:run
./mvnw test
```

Swagger queda disponible en:

```text
http://localhost:8080/swagger-ui.html
```

El contenedor Docker solo levanta PostgreSQL. El backend se ejecuta localmente con Maven y crea/actualiza las tablas desde las entidades JPA al iniciar. Los datos iniciales se cargan desde `ApplicationDataInitializer`.

Si necesitas cambiar credenciales o usar otra base, ejecuta el backend definiendo estas variables:

```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/cci_protestos"
$env:DATABASE_USER="cci_user"
$env:DATABASE_PASSWORD="cci_password"
.\mvnw.cmd spring-boot:run
```

Para borrar la base local y empezar desde cero:

```bash
docker compose down -v
docker compose up -d postgres
```

Usuarios demo disponibles: `admin@demo.local`, `staff@demo.local`, `analista@demo.local` y `deudor@demo.local`, todos con la clave `password`.
