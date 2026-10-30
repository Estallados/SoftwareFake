# Marketplace de alojamientos — Backend (Spring Boot)

API REST con JWT. Java 17, Spring Boot 3.3, PostgreSQL, Maven.

## Ejecutar
1. Crear la base: `CREATE DATABASE marketplace;`
2. Variables de entorno (en IntelliJ: Run → Edit Configurations → Environment variables; revisen que los nombres no tengan espacios):

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Sí | Conexión a PostgreSQL |
| `AES_SECRET_KEY` | Sí | Base64 de 32 bytes. Cifra el correo y el secreto TOTP, y de ella se deriva la clave que firma los JWT. **Todo el equipo debe usar la misma** si comparten base; si cambia, los usuarios existentes quedan inservibles y los tokens emitidos dejan de valer |
| `MAIL_PASSWORD` | No | Contraseña de aplicación de Gmail de la cuenta remitente. Sin ella no se envían correos |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | No | Si no existe ningún ADMIN y `ADMIN_PASSWORD` está definida, se crea el admin inicial al arrancar |
| `CORS_ORIGINS` | No | Orígenes del front separados por coma (por defecto `http://localhost:4200`) |

   Generar una clave en PowerShell: `$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)`

   Las claves nunca se escriben en `application.properties` ni se suben a git.
3. `mvn spring-boot:run`
4. Swagger UI: http://localhost:8081/swagger-ui.html (botón **Authorize** para pegar el token)

## Paquetes (co.edu.unbosque)
`controller` · `service` · `repository` · `entity` · `dto` · `security` · `configuration` · `exception` · `util`

## Roles
HUESPED, ANFITRION y ADMIN (enum `Role`). El registro público solo permite HUESPED (por defecto) o ANFITRION; el primer ADMIN lo crea el arranque (ver `ADMIN_PASSWORD`).

## Endpoints principales
| Área | Endpoint |
|---|---|
| Auth | `POST /auth/register`, `/auth/login`, `/auth/recuperar`, `/auth/restablecer`, `/auth/reactivar` |
| Perfil | `GET/PUT /usuarios/perfil` (solo nombre y teléfono), `PATCH /usuarios/perfil/desactivar` (responde el nuevo `estado`) |
| Usuarios (ADMIN) | `POST /usuarios/crearadministrador`, `GET /usuarios/listar`, `GET /usuarios/obtener/{id}`, `GET /usuarios/obtenercorreo` |
| Salud | `GET /actuator/health` (público) |
| Alojamientos | `GET /alojamientos` (búsqueda pública), `GET /alojamientos/politicas`, `GET /alojamientos/{id}`, `GET /alojamientos/mios`, `GET /alojamientos/{id}/historial`, `POST /alojamientos`, `PUT /alojamientos/{id}`, `POST /alojamientos/{id}/enviarrevision|pausar|reanudar` |
| Servicios adicionales | `POST /alojamientos/{id}/servicios`, `PUT|DELETE /alojamientos/{id}/servicios/{idServicio}` |
| Disponibilidad | `GET|POST /alojamientos/{id}/disponibilidad`, `GET /alojamientos/{id}/disponibilidad/consultar?llegada&salida`, `PUT|DELETE /alojamientos/{id}/disponibilidad/{idPeriodo}` |
| Cotización y reservas | `POST /cotizaciones`, `POST /reservas` (cabecera `Idempotency-Key`), `GET /reservas/mias`, `GET /reservas/anfitrion`, `GET /reservas/{id}`, `POST /reservas/{id}/cancelar` (cabecera `Idempotency-Key`) |
| Notificaciones | `GET /notificaciones`, `PATCH /notificaciones/{id}/leida` |
| Reseñas | `POST /resenas`, `GET /alojamientos/{id}/resenas` |
| Admin | `GET /admin/usuarios`, `PATCH /admin/usuarios/{id}/restringir|liberar`, `GET /admin/alojamientos?estado=`, `POST /admin/alojamientos/{id}/aprobar|rechazar|bloquear|desbloquear`, `PATCH /admin/resenas/{id}/ocultar|mostrar`, `GET|PUT /admin/tarifas`, `GET /admin/auditoria`, `GET /admin/auditoria/{id}` |
| Dashboard y reportes | `GET /admin/dashboard`, `GET /admin/reportes/{usuarios|alojamientos|reservas|ingresos|demanda}?formato=pdf|excel` |
| Opcionales | `GET /favoritos`, `POST|DELETE /favoritos/{idAlojamiento}`, `POST|GET /conversaciones`, `GET|POST /conversaciones/{id}/mensajes`, `POST /usuarios/segundofactor/activar|confirmar|desactivar` |

Filtros de búsqueda: `ciudad, llegada, salida, huespedes, tipo, precioMinimo, precioMaximo, amenidades, pagina, tamanio`. Todas las listas son paginadas (`pagina`, `tamanio`, máximo 100) y responden `{contenido, pagina, tamanio, totalElementos, totalPaginas}`.

### Tiempo real (WebSocket + STOMP)
Conexión en `ws://localhost:8081/ws` (sin SockJS) con el token en el frame CONNECT: `Authorization: Bearer <jwt>`. Sin token válido el servidor responde un frame ERROR y cierra.

| Suscribirse a | Llega |
|---|---|
| `/user/queue/mensajes` | `MensajeDTO` de cada mensaje nuevo en tus conversaciones (también los tuyos, para sincronizar otras pestañas) |
| `/user/queue/notificaciones` | `NotificacionDTO` de cada notificación nueva (para la campana) |

Los mensajes se envían por REST (`POST /conversaciones/{id}/mensajes`); por el WebSocket solo se reciben. Solo se permite suscribirse a `/user/queue/...`.

Pago de prueba: sin `STRIPE_SECRET_KEY` se usa un simulador con los métodos de prueba de Stripe (`pm_card_visa` aprobado, `pm_card_chargeDeclined` rechazado, `pm_card_error` error). Con una clave `sk_test_...` se usa el modo de prueba real de Stripe. Una clave que no sea de prueba se rechaza siempre.

## Decisiones de diseño
- **RF-001 (contradice su literal):** el registro público solo crea HUESPED o ANFITRION. El primer ADMIN lo crea `AdminInicializador` al arrancar (variables `ADMIN_EMAIL` / `ADMIN_PASSWORD`) y los siguientes los crea otro administrador con `POST /usuarios/crearadministrador`.
- **Recuperación de contraseña simulada:** `POST /auth/recuperar` no envía correo; el token (válido 30 minutos, de un solo uso) se escribe en el log del backend con la etiqueta `[SIMULACION DE CORREO]`. Se consume con `POST /auth/restablecer`.
- **Auditoría (RNF-023..026):** cada evento se guarda en la tabla `evento_auditoria` (solo inserción) y se escribe como una línea JSON en el log `AUDITORIA`. Se guarda en una transacción propia, así quedan también las operaciones rechazadas.
- **Idempotencia (RNF-013, 014):** `IdempotenciaService` protege operaciones con la cabecera `Idempotency-Key` (reservas, pagos y cancelaciones, cuando existan).
- **Códigos de los services:** 0 éxito, 1 no encontrado, 2 datos inválidos; los códigos propios se documentan en el Javadoc de cada método. Los services nuevos devuelven `Resultado<T>` (código + dato + mensaje) y `RespuestaUtil.desde(...)` los traduce a HTTP: 1→404, 2→400, 3→403, 4 y 5→409, 6→402, 7→502.
- **Nombres:** se conservan los del proyecto: `Alojamiento` (la "publicación" del documento), rutas `/alojamientos`, roles `HUESPED`, `ANFITRION`, `ADMIN`, y `EstadoUsuario` (ACTIVO, INACTIVO, RESTRINGIDO).
- **Disponibilidad:** el anfitrión registra períodos; las fechas reservadas no se guardan aparte, se derivan de las reservas activas (bloqueo automático, RF-020). Una reserva sin pagar retiene las fechas 15 minutos.
- **Reserva:** cotización vigente 30 minutos → bloqueo pesimista del alojamiento y revalidación → reserva PENDIENTE_PAGO → pago de prueba fuera de la transacción → CONFIRMADA de inmediato o CANCELADA si el pago falla.
- **Tarifas (CA-085):** limpieza 5 % y servicio 10 % por defecto; el administrador los cambia en `/admin/tarifas` y cada alojamiento puede tener los suyos.

Trazabilidad por módulo: [`docs/trazabilidad.md`](docs/trazabilidad.md).

## Flujo del MVP
Anfitrión crea (BORRADOR) → envía a revisión → admin aprueba (ACTIVA) → anfitrión define disponibilidad → huésped busca → cotiza → reserva inmediata → (cancela con política) → reseña tras el check-out.
