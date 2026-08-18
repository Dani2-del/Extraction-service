# Extraction Service — Prueba Técnica

Servicio backend en Spring Boot que gestiona productos mediante una API REST,
extrae información de productos desde [Automation Exercise](https://automationexercise.com/)
y procesa las extracciones de forma asíncrona.

> **Estado:** proyecto base generado. Pendiente: verificar los selectores CSS
> del scraper contra el HTML real del sitio y completar pruebas. Ver sección
> "Pendientes" al final.

## Cómo ejecutar la aplicación

Requisitos: JDK 17+ y Maven 3.9+ (o usar el wrapper `./mvnw` si se agrega).

```bash
mvn clean install
mvn spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080`.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Consola H2 (BD embebida en archivo, en `./data/`): `http://localhost:8080/h2-console`
  (JDBC URL: `jdbc:h2:file:./data/extraction-service`, usuario `sa`, sin contraseña)

No se requiere ninguna credencial ni servicio pago para evaluar la solución.

## Tecnologías utilizadas

- **Java 17 + Spring Boot 3.3** — framework maduro, con soporte de primera
  clase para REST, persistencia y procesamiento asíncrono sin dependencias
  externas adicionales.
- **Spring Data JPA + H2** — persistencia simple y evaluable sin instalar
  nada; el modelo es sencillo (2-3 tablas) y no justifica un motor más pesado
  para esta prueba. H2 se usa en modo archivo para que los datos sobrevivan
  a un reinicio durante la evaluación.
- **Jsoup** — librería estándar de la comunidad Java para parsear HTML;
  suficiente porque el detalle de producto de Automation Exercise es HTML
  estático (no requiere ejecutar JavaScript), por lo que no fue necesario
  un navegador headless (Selenium/Playwright).
- **Spring `@Async` + `ThreadPoolTaskExecutor`** — mecanismo nativo de Spring
  para procesamiento asíncrono, sin necesidad de una cola externa
  (RabbitMQ/Kafka) que sería sobreingeniería para el alcance de esta prueba.
- **springdoc-openapi** — documentación Swagger automática a partir de las
  anotaciones de los controllers.
- **Lombok** — reduce boilerplate en entidades y DTOs.

## Descripción general de la solución

El servicio expone dos grupos de endpoints:

**Gestión de productos** (`/products`) — CRUD estándar sobre la tabla
`products`. Los productos pueden crearse manualmente o ser el resultado de
una extracción (en cuyo caso `externalId` queda poblado y sirve como clave
de deduplicación/actualización).

**Extracciones** (`/extractions`) — al solicitar `POST /extractions` con una
lista de `productIds`, se crea inmediatamente un `ExtractionJob` en estado
`PENDING` y se devuelve `202 Accepted` con su id. El procesamiento real
ocurre en segundo plano.

## Estrategia de procesamiento asíncrono

1. `POST /extractions` persiste el job (`PENDING`) y dispara un método
   `@Async` en un bean separado (`ExtractionAsyncService`), evitando el
   problema de auto-invocación de proxies de Spring.
2. El método async pasa el job a `PROCESSING` y recorre los `productIds`
   uno por uno, usando el pool `extractionExecutor` (tamaño 3–5 hilos) para
   limitar la concurrencia contra la fuente externa.
3. Por cada producto: se hace scraping, se persiste (upsert por
   `externalId`) y se registra un `ExtractionJobItem` con su resultado
   (`SUCCESS`/`FAILED`). Un fallo individual **no detiene** el resto —
   se captura, se registra el error y se continúa con el siguiente id.
4. Los contadores (`processed`, `successful`, `failed`) del job se
   actualizan progresivamente, por lo que `GET /extractions/{id}` refleja
   el avance real mientras el job sigue `PROCESSING`.
5. Al terminar todos los productos, el job pasa a:
   - `COMPLETED` si no hubo fallos,
   - `FAILED` si todos fallaron,
   - `COMPLETED_WITH_ERRORS` si hubo una mezcla de éxitos y fallos.

Las transacciones se apoyan en que `JpaRepository.save()` ya es
transaccional por sí mismo (no se usó `@Transactional` en métodos que se
invocan vía `this` dentro de la misma clase, porque el proxy de Spring no
intercepta esas llamadas — ver comentario en `ExtractionAsyncService`).

## Endpoints principales

```
POST   /products
GET    /products
GET    /products/{id}
PATCH  /products/{id}
DELETE /products/{id}

POST   /extractions              -> 202 Accepted { id, status }
GET    /extractions/{id}         -> estado y contadores del job
GET    /extractions/{id}/products -> productos extraídos exitosamente por ese job
```

## Decisiones y trade-offs

- **H2 en archivo en vez de Postgres/Docker Compose**: prioriza que
  cualquier evaluador pueda levantar el proyecto con un solo comando, sin
  instalar ni configurar una base externa. Trade-off: no refleja un entorno
  de producción real; migrar a Postgres solo requiere cambiar
  `application.yml` y la dependencia del driver.
- **`ExtractionJobItem` como entidad separada** en vez de guardar solo los
  contadores agregados: permite auditar qué producto falló y por qué,
  a costa de una tabla adicional.
- **Procesamiento secuencial dentro del hilo async** (no `CompletableFuture`
  por producto): más simple de razonar y suficiente dado que la
  concurrencia real ya está acotada por el tamaño del pool de extracción;
  con más tiempo se podría paralelizar productos individuales dentro de un
  mismo job.
- **No se implementó idempotencia/deduplicación de jobs duplicados**: un
  `POST /extractions` repetido con los mismos ids crea un job nuevo. Quedó
  fuera del alcance mínimo por tiempo.
- **Selectores CSS del scraper**: definidos por inspección manual de la
  estructura conocida de Automation Exercise (`.product-information`,
  párrafos `Category:`/`Availability:`/`Condition:`/`Brand:`). Si el sitio
  cambia su marcado, requieren ajuste.

## Inteligencia artificial utilizada

Se usó Claude (Anthropic) como asistente durante el análisis del enunciado
y la generación del esqueleto inicial del proyecto (estructura de paquetes,
entidades, repositorios, controllers y la configuración del executor
asíncrono). No se incorporó IA dentro de la solución en tiempo de
ejecución — no era necesaria para resolver el problema planteado.

## Pendientes / qué mejoraría con más tiempo

- Verificar y ajustar los selectores del scraper contra el HTML real vigente
  del sitio (no se tuvo acceso a internet en el entorno donde se generó este
  esqueleto).
- Agregar pruebas automatizadas (unitarias para `ExtractionAsyncService` y
  el scraper con HTML de prueba fijo; de integración para los controllers
  con `MockMvc`).
- Dockerfile / docker-compose para empaquetar la app y, opcionalmente,
  Postgres.
- Reintentos con backoff ante errores temporales de red al hacer scraping.
- Endpoint o mecanismo de cancelación de jobs en curso.
- Rate limiting explícito hacia el sitio externo (más allá del límite de
  hilos del pool).
- Paginación en `GET /products`.
