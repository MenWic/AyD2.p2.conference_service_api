# Conference Service API

Microservicio de congresos, instituciones, actividades, participantes, reservas, asistencia, certificados y reglas operativas del dominio académico del ecosistema de microservicios del proyecto.

Este servicio expone endpoints para administrar y consultar información funcional de congresos. La autenticación se basa en Bearer JWT emitido por IAM.

## Requisitos

- Java 21
- PostgreSQL local o remoto
- Gradle Wrapper incluido en el repo
- Docker opcional, solo para ejecutar tests de persistencia con Testcontainers

## Configuración local

Por defecto el servicio usa:

```properties
spring.application.name=conference-service
server.port=8082
spring.datasource.url=jdbc:postgresql://localhost:5432/conference_db
spring.datasource.username=conference_service_user
spring.datasource.password=conference_password
```

Variable recomendada para ambientes reales:

```bash
SECURITY_JWT_SECRET=<mismo-secret-usado-por-iam>
```

Conference no emite tokens. Valida tokens JWT emitidos por IAM usando el mismo secret mientras el proyecto mantenga JWT simétrico.

Para ejecución con Eureka:

```bash
EUREKA_SERVER_URL=http://localhost:8761/eureka/
```

## Archivos de configuración local

Este repositorio espera dos archivos locales no versionados:

```text
src/main/resources/application.properties
src/main/resources/application-discovery.properties
```

`application.properties` se usa para ejecución individual del servicio.

`application-discovery.properties` se usa cuando el servicio debe registrarse como cliente de Eureka.

Estos archivos no deben subirse al repositorio si contienen credenciales, secretos, URLs locales o configuración sensible.

## Base de datos local

Crear la base de datos y usuario esperados por `application.properties`.

Base esperada:

```text
conference_db
```

Usuario esperado:

```text
conference_service_user
```

Luego, dentro de la base `conference_db`, asegurar permisos sobre el schema público.

Las migraciones se ejecutan con Flyway al arrancar el servicio.

## Compilar

Windows:

```powershell
.\gradlew clean compileJava
```

Linux/macOS:

```bash
./gradlew clean compileJava
```

## Levantar el servicio individualmente

Usar este modo cuando se quiere ejecutar Conference solo, por desarrollo o debug, sin Eureka Server ni Gateway.

Windows:

```powershell
.\gradlew bootRun
```

Linux/macOS:

```bash
./gradlew bootRun
```

URL base directa:

```text
http://localhost:8082
```

En este modo Eureka queda deshabilitado por defecto:

```properties
eureka.client.enabled=false
```

## Levantar el servicio como cliente Eureka

Usar este modo cuando ya esté levantado el Eureka Server y el servicio deba registrarse para ser consumido por el Gateway.

Orden recomendado:

```text
1. Eureka Server
2. IAM Service con perfil discovery
3. Conference Service con perfil discovery
4. Wallet Service con perfil discovery
5. API Gateway
6. Frontend / App
```

Windows:

```powershell
.\gradlew bootRun --args="--spring.profiles.active=discovery"
```

Linux/macOS:

```bash
./gradlew bootRun --args="--spring.profiles.active=discovery"
```

Con variable explícita de Eureka:

Windows PowerShell:

```powershell
$env:EUREKA_SERVER_URL="http://localhost:8761/eureka/"
.\gradlew bootRun --args="--spring.profiles.active=discovery"
```

Linux/macOS:

```bash
EUREKA_SERVER_URL=http://localhost:8761/eureka/ ./gradlew bootRun --args="--spring.profiles.active=discovery"
```

El perfil `discovery` usa:

```properties
eureka.client.enabled=true
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=false
eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
```

Conference por ahora solo se registra en Eureka. Las llamadas a otros microservicios deben pasar por `integration/port` e `integration/client` cuando el contrato lo exija.

## Tests

Ejecutar toda la suite:

Windows:

```powershell
.\gradlew test
```

Linux/macOS:

```bash
./gradlew test
```

Si no hay Docker/Testcontainers disponible en local, ejecutar tests omitiendo tests de persistencia:

Windows:

```powershell
.\gradlew test -PskipPersistenceTests=true
```

Linux/macOS:

```bash
./gradlew test -PskipPersistenceTests=true
```

Los tests de persistencia usan PostgreSQL Testcontainers y requieren Docker activo.

## Coverage con JaCoCo

Generar reporte local sin tests de persistencia:

Windows:

```powershell
.\gradlew test jacocoTestReport jacocoTestCoverageVerification -PskipPersistenceTests=true
```

Linux/macOS:

```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification -PskipPersistenceTests=true
```

Quality gate completo, con Testcontainers activo:

Windows:

```powershell
.\gradlew test jacocoTestReport jacocoTestCoverageVerification
```

Linux/macOS:

```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

Reportes generados:

```text
build/reports/jacoco/test/html/index.html
build/reports/jacoco/test/jacocoTestReport.xml
```

## OpenAPI / Swagger

Con el servicio levantado:

Swagger UI:

```text
http://localhost:8082/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8082/v3/api-docs
```

Los endpoints protegidos usan Bearer JWT. En Swagger UI se debe usar el botón **Authorize** y pegar el access token con formato Bearer.

## Actuator

Endpoints habilitados:

```text
http://localhost:8082/actuator/health
http://localhost:8082/actuator/info
```

## Áreas funcionales esperadas

El microservicio Conference concentrará progresivamente funcionalidades como:

```text
- Institutions
- Congresses
- Rooms
- Activities
- Calls for papers
- Papers / submissions
- Scientific committee
- Registrations / reservations
- Attendance
- Certificates
- Conference reports
```

Para request/response, errores, roles y ejemplos, usar Swagger UI cuando el microservicio tenga los controladores implementados.

## Ejecución rápida recomendada

Modo individual:

```bash
./gradlew bootRun
```

Modo con Eureka:

```bash
./gradlew bootRun --args="--spring.profiles.active=discovery"
```

Tests locales sin Docker:

```bash
./gradlew test -PskipPersistenceTests=true
```

Tests completos con Docker:

```bash
./gradlew test
```
