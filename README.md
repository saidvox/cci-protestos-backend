# CCI Protestos Backend

API academica en Spring Boot para el flujo de protestos de la Camara de Comercio de Ica.

## Ejecutar localmente

Requisitos:

- Java 21

Configuracion por defecto:

```properties
spring.datasource.url=jdbc:h2:file:./data/cci_protestos
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create
```

Comandos:

```bash
./mvnw spring-boot:run
./mvnw test
```

Swagger queda disponible en:

```text
http://localhost:8080/swagger-ui.html
```

El esquema se recrea desde las entidades JPA al iniciar. Los datos academicos iniciales se cargan desde `AcademicDataInitializer`.

Si quieres usar PostgreSQL local, ejecuta el backend definiendo estas variables:

```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/cci_protestos"
$env:DATABASE_USER="postgres"
$env:DATABASE_PASSWORD="postgres"
.\mvnw.cmd spring-boot:run
```

Usuarios demo disponibles: `admin@demo.local`, `staff@demo.local`, `analista@demo.local` y `deudor@demo.local`, todos con la clave `password`.
