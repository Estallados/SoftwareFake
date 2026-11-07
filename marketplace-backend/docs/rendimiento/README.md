# Informe de pruebas de carga y estrés

Backend del marketplace, medido el 6 y 7 de noviembre de 2026 sobre el commit `56ec57f`. Para correr las pruebas, consulta [GUIA_PRUEBA_ESTRES.md](GUIA_PRUEBA_ESTRES.md).

## Conclusión

- **Soporta una carga normal:** 400 usuarios simultáneos (193 rps) con p95 de 342 ms y 0 errores.
- **Se rompe entre 225 y 250 rps:** el fallo es abrupto, y el sistema pasa de responder en 122 ms a agotar el tiempo de espera de 10 s.
- **Las reservas son seguras bajo concurrencia:** no hubo dobles reservas ni cobros duplicados.
- **El mayor problema es el N+1:** una página del catálogo lanza 123 consultas SQL. En producción, con latencia real hacia Neon, la capacidad puede ser muy inferior a la medida aquí.

**Estos números no son la capacidad de producción.** Sirven para comparar y encontrar cuellos de botella, por las limitaciones que se explican más abajo.

## Criterio de aceptación

El repositorio no define objetivos numéricos de rendimiento. Se asumió **p95 < 500 ms y errores < 1 %**. Si el proyecto tiene otro objetivo, hay que reevaluar los resultados.

## Resultados

| Escenario | Resultado | Veredicto |
|---|---|---|
| Carga, 50 usuarios (25 rps) | p95 91 ms, 0 errores | Cumple |
| Carga, 200 usuarios (100 rps) | p95 118 ms, 0 errores | Cumple |
| Carga, 400 usuarios (193 rps) | p95 342 ms, 0 errores | Cumple |
| Estrés | Estable hasta 225 rps; colapsa a 250 rps | Límite encontrado |
| Pico de 600 rps durante 30 s | 74,5 % de errores; 12,8 % en el minuto siguiente; normal en el segundo | No cumple durante el pico; se recupera solo |
| Resistencia con rampa, 15 min a 150 rps | 136.000 peticiones, 0 errores, memoria estable | Cumple |
| Resistencia en frío, 150 rps de golpe | 57,7 % de errores; casi 7 min para estabilizarse | No cumple |
| Login | Estable hasta 80 rps; máximo cerca de 115 rps | Límite de CPU |
| Contraseñas incorrectas masivas | 200 intentos/s dejan 62 % de errores en todo el sistema | No cumple |
| Doble reserva | 20 de 20 rondas con un solo ganador | Cumple |
| Idempotencia | 20 de 20 rondas con una sola reserva | Cumple |

### Estrés por escalones

| rps pedidos | rps atendidos | p50 | p95 | Errores | Hilos Tomcat | En cola por conexión |
|---|---|---|---|---|---|---|
| 100 | 100,8 | 22 ms | 88 ms | 0 % | 10 | 0 |
| 200 | 201,8 | 26 ms | 113 ms | 0 % | 14 | 3 |
| 225 | 226,8 | 30 ms | 122 ms | 0 % | 31 | 19 |
| 250 | 215,4 | 6.942 ms | 10.003 ms | 37,1 % | 200 | 194 |

Después de cortar el exceso de tráfico, el sistema volvió a p95 de 77 ms a 50 rps. La recuperación fue inmediata en esa corrida, pero tardó hasta un minuto en el pico.

### Efecto de la configuración

| Configuración | Último escalón estable | Observación |
|---|---|---|
| Pool de 10 conexiones (por defecto) | 225 rps | Referencia |
| Pool de 30 conexiones | 300 rps | Mejora del 33 %; a 350 rps se degrada |
| Sin `open-in-view` | 100 rps según el p95 | **Empeora:** p95 de 883 ms a 150 rps |

Cada configuración se midió una sola vez, así que estas diferencias son orientativas.

### Consultas SQL por petición

| Endpoint | Consultas |
|---|---|
| `GET /alojamientos/disponibles` (20 resultados) | **123** |
| `GET /alojamientos?ciudad=...` (12 resultados) | **75** |
| `GET /reservas/mias` (10 resultados) | **54** |
| `GET /alojamientos/{id}/resenas` | 8 |
| `GET /alojamientos/{id}` | 7 |
| `GET /alojamientos/{id}/disponibilidad` | 3 |
| `GET /usuarios/perfil` | 2 |

Con la mezcla de tráfico usada, 225 rps equivalen a unas 12.000 consultas por segundo (estimación a partir de esta tabla).

## Hallazgos

### 1. Consultas N+1 en listados (prioridad alta)

`PublicacionService.convertirDTO` (`PublicacionService.java:971`) consulta por separado servicios, política, promedio, reseñas, reservas y anfitrión de cada alojamiento. `ReservaService.convertirDTO` (`ReservaService.java:302`) hace lo mismo con cada reserva.

Aquí la base está en la misma máquina, así que este costo casi no se nota en la latencia. Con red, las consultas secuenciales se pagan una por una. Esta estimación solo suma la latencia de red, y no es una medición:

| Latencia hacia la base | Catálogo (123) | Búsqueda (75) | Mis reservas (54) |
|---|---|---|---|
| 1 ms | 0,12 s | 0,08 s | 0,05 s |
| 5 ms | 0,6 s | 0,4 s | 0,3 s |
| 50 ms | 6,2 s | 3,8 s | 2,7 s |

**Recomendación:** cargar los datos de la página en bloque, con consultas `IN (...)` agrupadas por `id_alojamiento`, o con una proyección que traiga los conteos y promedios en una sola consulta.

### 2. Colapso abrupto al saturarse (prioridad alta)

Al pasar el límite, todas las peticiones hacen cola, incluso las más baratas. El cliente abandona a los 10 s, pero el servidor sigue trabajando en esas peticiones, y por eso no sale rápido de la saturación. Hikari espera hasta 30 s por una conexión y no hay límite de peticiones en cola.

**Recomendación:** bajar `spring.datasource.hikari.connection-timeout` a 2 o 3 s, responder 503 cuando se agote, y limitar la cola con `server.tomcat.accept-count`. Conviene validar estos valores con una nueva prueba.

### 3. Sin protección contra abuso del login (prioridad alta)

No hay ningún limitador de intentos en el código. Cada intento consume unos 66 ms de CPU por BCrypt, aunque la contraseña sea incorrecta, y además escribe en auditoría. Con 100 intentos/s el p95 de una lectura pasa de 10 ms a 172 ms. Con 200 intentos/s cae el servicio completo, también para quien no está iniciando sesión.

**Recomendación:** limitar por IP y por cuenta. BCrypt no se debe debilitar: su costo es intencional.

### 4. Arranque en frío (prioridad media)

Con la JVM recién iniciada, 150 rps de golpe saturaron el sistema durante casi 7 minutos. La misma tasa alcanzada en 5 minutos dio 0 errores. Un reinicio o despliegue con tráfico activo puede provocar esta situación.

**Recomendación:** calentar la instancia antes de enviarle tráfico y liberar el tráfico gradualmente.

### 5. Pool de conexiones por defecto (prioridad media)

No hay configuración de Hikari ni de Tomcat en `application.properties`. Subir a 30 conexiones mejoró la capacidad un 33 % en H2. Antes de aumentarlo en producción hay que revisar el límite de conexiones del plan de Neon.

**No conviene desactivar `open-in-view` todavía.** Con el N+1 actual empeoró el rendimiento, porque multiplica las veces que se pide una conexión al pool (hipótesis, no medida). Hay que volver a probarlo después de corregir el hallazgo 1.

### 6. Correo síncrono en el login (no medido)

`CorreoUtil.enviarCorreo` se llama dentro de la petición, sin `@Async`. Un huésped mantiene ocupado un hilo mientras responde Gmail. Esto no se midió porque el correo estaba apagado, así que el impacto real falta por comprobar.

### 7. `/actuator/health` depende del correo (prioridad baja)

Con el servidor SMTP inalcanzable, el endpoint respondió `DOWN` aunque la API funcionaba. Si un monitor reinicia el servicio según este endpoint, una caída de Gmail podría provocar reinicios innecesarios.

**Recomendación:** `management.health.mail.enabled=false`.

## Concurrencia de reservas

- **Doble reserva:** 100 huéspedes pidieron a la vez las mismas fechas del mismo alojamiento, en 20 rondas. Cada ronda tuvo 1 respuesta 201 y 99 respuestas 409.
- **Idempotencia:** la misma `Idempotency-Key` se envió 30 veces a la vez, en 20 rondas. Cada ronda creó una sola reserva.
- **Comprobación en la base:** 40 reservas confirmadas, 40 pagos aprobados y 0 cruces de fechas.

Las 56 líneas de ERROR del log son la violación de `uq_idempotencia`, y el backend las convierte en un 409. Es el comportamiento esperado, aunque ensucia el log.

H2 no tiene la restricción `ex_reserva_sin_cruces`, así que aquí la protección la dio solo el bloqueo `FOR UPDATE` del backend. En PostgreSQL existe además esa defensa.

## Cómo se hizo

| Parte | Detalle |
|---|---|
| Máquina | 12 núcleos y 22 GB de RAM; generador, backend y base en el mismo equipo |
| Backend | Código de `56ec57f`, compilado con JDK 21, 1 GB de heap, pool de 10 conexiones |
| Base de datos | H2 en memoria, en proceso aparte y por TCP, con los índices de `02_indexes.sql` y los `UNIQUE` de `01_schema_ddl.sql` |
| Volumen | 2.201 usuarios, 5.000 alojamientos, 30.000 reservas, 40.000 notificaciones y 100.000 eventos de auditoría |
| Generador | `carga.mjs`, en Node 22 y sin dependencias; el retraso del generador se mantuvo por debajo de 60 ms |
| Servicios externos | Stripe, correo, Cloudinary y Nominatim apagados; pagos con el simulador |
| Aislamiento | Cada escenario empezó con base nueva y calentamiento de 30 s |

Se usó un generador propio porque en esta máquina no estaban k6, JMeter ni PostgreSQL, y su descarga quedó bloqueada por los permisos del entorno. No se usó la base de Neon porque es compartida.

## Limitaciones

- **H2 no es PostgreSQL.** En el colapso, H2 consumía unos 6 núcleos frente a 3 del backend, así que la cifra de 225 rps depende en buena parte de H2.
- **Sin latencia de red.** Es la diferencia más importante frente a producción (ver hallazgo 1).
- **Índices incompletos.** H2 no admite índices por expresión, y faltan `idx_alojamiento_ciudad` e `idx_alojamiento_tipo`. Los parciales se crearon como índices completos.
- **Todo corre en una máquina.** Generador, backend y base compiten por la misma CPU. Con el pool de 30, cerca de 350 rps, la CPU de la máquina ya estaba casi agotada.
- **Servicios externos sin medir.** Un pago real con Stripe o el envío de correos añadirían tiempo que aquí no existe.
- **Los resultados varían entre corridas.** A 250 rps, una corrida anterior dio 0 % de errores con p95 de 3,5 s, y la de este informe dio 37,1 %. Ese límite debe leerse como un rango.
- **Resistencia corta.** 15 minutos no bastan para descartar fugas de memoria lentas. Haría falta una corrida de varias horas.
- **WebSocket y subida de imágenes no se probaron.**

Las primeras corridas de concurrencia y estrés se descartaron: el esquema generado en H2 no coincidía con `01_schema_ddl.sql` y provocaba errores 500 falsos en las reservas. Se corrigió en `SembrarDatos.java` y se repitieron las pruebas.

## Siguiente paso

Repetir `carga` y `estres` en un PostgreSQL con latencia parecida a la de producción, después de corregir el N+1. Esa medición sí permite estimar la capacidad real.

## Archivos

| Archivo | Contenido |
|---|---|
| `carga.mjs` | Generador de tráfico y escenarios |
| `ejecutar.sh` | Corre uno o varios escenarios |
| `entorno.sh` | Compila, levanta y detiene H2 y el backend |
| `SembrarDatos.java` | Datos de prueba, tokens y ajuste del esquema |
| `consultas.mjs` | Cuenta las consultas SQL por endpoint |
| `resultados/*.json` | Resultados de cada escenario, con su telemetría |
| `resultados/errores_*.txt` | Errores del backend; un archivo vacío significa que no hubo |
