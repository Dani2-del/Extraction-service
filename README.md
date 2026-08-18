# Extraction Service — Prueba Técnica

Servicio backend hecho en Spring Boot para la prueba técnica de Ingeniero de Software Jr. Permite gestionar productos mediante una API REST y extraer información de productos reales desde Automation Exercise (https://automationexercise.com/), procesando esas extracciones de forma asíncrona.

## Cómo ejecutarlo

Se necesita JDK 17 o superior y Maven.

```bash
mvn clean install
mvn spring-boot:run
```

Con eso la app queda corriendo en `http://localhost:8080`.

También quedan disponibles:

- Swagger UI en `http://localhost:8080/swagger-ui.html`
- Consola de la base de datos H2 en `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/extraction-service`, usuario `sa`, sin contraseña)

No hace falta ninguna credencial ni servicio pago para probarlo.

## Qué usé y por qué

Elegí Java 17 con Spring Boot 3.3 porque es lo que estoy aprendiendo y me daba todo lo necesario (REST, persistencia, procesamiento asíncrono) sin tener que sumar herramientas externas.

Para la base de datos usé H2 en modo archivo. Con dos o tres tablas no tenía sentido montar Postgres ni pedirle al evaluador que instale algo — con H2 el proyecto corre con un solo comando.

Para leer el HTML de Automation Exercise usé Jsoup. El detalle de cada producto es HTML estático, no depende de JavaScript, así que no hizo falta nada más pesado como Selenium.

El procesamiento asíncrono lo resolví con `@Async` de Spring más un `ThreadPoolTaskExecutor`. Me pareció suficiente para el tamaño del problema — meter una cola externa como RabbitMQ hubiera sido sobreingeniería para esta prueba.

Sumé Swagger (springdoc-openapi) para tener documentación de los endpoints sin escribirla a mano, y Lombok para no repetir getters y setters en cada clase.

## Cómo está armado

Hay dos grupos de endpoints.

`/products` es un CRUD normal sobre la tabla de productos. Un producto puede crearse a mano o llegar como resultado de una extracción — en ese segundo caso queda guardado su `externalId`, que es lo que uso para no duplicarlo si se vuelve a extraer.

`/extractions` es la parte más importante. Cuando se manda `POST /extractions` con una lista de ids, se crea de inmediato un job en estado `PENDING` y se responde con `202 Accepted` — sin esperar a que termine el procesamiento.

## Cómo funciona el procesamiento asíncrono

1. Al crear el job, se guarda en la base de datos y se dispara un método `@Async` en una clase aparte (`ExtractionAsyncService`). Lo separé en otra clase a propósito, porque si el método asíncrono se llama desde dentro de la misma clase que lo crea, Spring no logra interceptarlo correctamente.
2. Ese método pasa el job a `PROCESSING` y recorre los productos uno por uno, usando un pool de 3 a 5 hilos para no mandar demasiadas peticiones simultáneas al sitio externo.
3. Por cada producto se hace el scraping, se guarda el resultado (o se actualiza si ya existía) y se registra si salió bien o mal en un `ExtractionJobItem`. Si uno falla, no frena a los demás — se anota el error y se sigue con el siguiente.
4. Los contadores del job (`processed`, `successful`, `failed`) se van actualizando a medida que avanza, así que consultando `GET /extractions/{id}` se puede ver el progreso real mientras todavía está corriendo.
5. Al terminar, el job queda en `COMPLETED` si no hubo fallos, en `FAILED` si fallaron todos, o en `COMPLETED_WITH_ERRORS` si fue una mezcla.

Un detalle técnico que vale la pena explicar: no usé `@Transactional` en los métodos internos de `ExtractionAsyncService` porque se llaman entre sí dentro de la misma clase, y ahí el proxy de Spring no los intercepta — la anotación quedaría ahí sin hacer nada. Cada guardado se apoya en que `save()` de Spring Data JPA ya es transaccional por sí mismo, así que no hacía falta más.

## Endpoints

```
POST   /products
GET    /products
GET    /products/{id}
PATCH  /products/{id}
DELETE /products/{id}

POST   /extractions               -> 202 Accepted, { id, status }
GET    /extractions/{id}          -> estado y contadores del job
GET    /extractions/{id}/products -> productos que ese job extrajo con éxito
```

## Decisiones que tomé y por qué

Usé H2 en vez de Postgres para que cualquiera pueda correr el proyecto sin instalar nada más. Si tuviera que llevarlo a producción, cambiar a Postgres sería solo tocar la configuración y la dependencia del driver.

Separé `ExtractionJobItem` como su propia tabla en vez de guardar solo los números resumidos del job, porque así puedo saber exactamente qué producto falló y por qué, no solo cuántos.

El procesamiento lo hice secuencial dentro del hilo asíncrono, no lanzando un `CompletableFuture` por cada producto. Me pareció más simple de razonar y, como la concurrencia ya está limitada por el tamaño del pool, no hacía falta más para este alcance.

No implementé nada para evitar jobs duplicados — si mandas la misma extracción dos veces, se crean dos jobs distintos. Lo dejé fuera por tiempo.

Los selectores que usa el scraper para leer el HTML los definí revisando manualmente la página de detalle de un producto en Automation Exercise. Los probé contra el sitio real y funcionaron bien con los primeros productos que probé, pero si el sitio cambia su estructura habría que ajustarlos.

## Inteligencia artificial que usé

Usé Claude, de Anthropic, como asistente durante todo el desarrollo. Sí tenía experiencia previa con Spring Boot por mi formación a lo largo de mi carrera universitaria en el Tecnológico Comfenalco, y Claude me ayudó a entender la arquitectura en capas (controller, service, repository, entity), a armar el proyecto base y a corregir un bug real que me apareció al probarlo (un problema de transacciones con el procesamiento asíncrono, donde el hilo en segundo plano intentaba leer un dato que la base de datos todavía no había terminado de guardar).

También lo usé para entender, paso a paso, cada parte del código antes de subirlo, porque quería poder explicarlo yo mismo y no solo entregarlo. La idea no es solamente generar un código y listo, sino entender bloque por bloque, paso por paso, para tener una idea genuina de cómo funciona el proyecto — con el fin de que si se desean agregar más funciones o mejorar otras sea práctico saber por dónde empezar, y no estar perdido o a la deriva en el propio proyecto.

En mis 3 años estudiando en la Fundación Universitaria Tecnológico Comfenalco he aprendido que no necesariamente hay que sabérselas todas de un lenguaje de programación de pies a cabeza: se trata de aprender a leer lo que hay en el código, escribirlo, y entender cómo se complementa y conecta cada línea entre sí. La IA hay que usarla de forma sabia, tampoco hay que pedirle que te lo haga todo, porque esa no es la idea.

No incorporé IA dentro de la aplicación en sí — no la necesitaba para resolver el problema.

## Qué me faltó y qué mejoraría con más tiempo

- Agregar pruebas automatizadas, tanto unitarias para el scraper y el procesamiento asíncrono como de integración para los endpoints.
- Un Dockerfile o docker-compose para empaquetar todo, y de paso probar con Postgres.
- Reintentos automáticos si falla una petición al sitio externo por un problema temporal de red.
- Alguna forma de cancelar un job que ya está corriendo.
- Un límite más explícito de peticiones hacia el sitio externo, más allá del tamaño del pool de hilos.
- Paginación en `GET /products`, si la lista de productos creciera mucho.