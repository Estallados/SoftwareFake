# Marketplace de alojamientos — Backend (Spring Boot)

API REST con JWT. Java 17, Spring Boot 3.3, PostgreSQL, Maven.

## Ejecutar
1. Crear la base: `CREATE DATABASE marketplace;`
2. Variables de entorno (en IntelliJ: Run → Edit Configurations → Environment variables; revisen que los nombres no tengan espacios):

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Sí | Conexión a PostgreSQL |
| `AES_SECRET_KEY` | Sí | Base64 de 32 bytes. Cifra nombre y correo, y de ella se deriva la clave que firma los JWT. **Todo el equipo debe usar la misma** si comparten base; si cambia, los usuarios existentes quedan inservibles y los tokens emitidos dejan de valer |
| `MAIL_PASSWORD` | No | Contraseña de aplicación de Gmail de la cuenta remitente. Sin ella no se envían correos |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | No | Si no existe ningún ADMIN y `ADMIN_PASSWORD` está definida, se crea el admin inicial al arrancar |
| `CORS_ORIGINS` | No | Orígenes del front separados por coma (por defecto `http://localhost:4200`) |

   Generar una clave en PowerShell: `$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)`

   Las claves nunca se escriben en `application.properties` ni se suben a git.
3. `mvn spring-boot:run`
4. Swagger UI: http://localhost:8081/swagger-ui.html (botón **Authorize** para pegar el token)

## Paquetes (co.edu.unbosque)
`controller` · `service` · `repository` · `model` · `dto` · `security` · `configuration` · `exception` · `util`

## Roles
HUESPED, ANFITRION y ADMIN (enum `Role`). El registro público solo permite HUESPED (por defecto) o ANFITRION; el primer ADMIN lo crea el arranque (ver `ADMIN_PASSWORD`).

## Endpoints principales
| Área | Endpoint |
|---|---|
| Auth | `POST /auth/register`, `/auth/login`, `/auth/forgot-password`, `/auth/reset-password` |
| Perfil | `GET/PUT /users/me`, `PATCH /users/me/deactivate` |
| Publicaciones | `GET /listings` (filtros), `GET /listings/{id}`, `GET /listings/mine`, `POST /listings`, `PUT /listings/{id}`, `POST /listings/{id}/submit|pause|resume` |
| Disponibilidad | `GET/POST /listings/{id}/availability`, `DELETE /listings/{id}/availability/{periodId}` |
| Cotización | `POST /quotes` |
| Reservas | `POST /reservations` (header `Idempotency-Key`), `GET /reservations/mine|host|{id}`, `POST /reservations/{id}/cancel` |
| Notificaciones | `GET /notifications`, `PATCH /notifications/{id}/read` |
| Reseñas | `POST /reviews`, `GET /listings/{id}/reviews` |
| Admin | `/admin/users`, `/admin/listings` (+ approve/reject/block/unblock), `/admin/reviews/{id}/hidden`, `/admin/audit`, `/admin/dashboard` |

Filtros de búsqueda: `city, checkIn, checkOut, guests, type, minPrice, maxPrice, amenities, page, size`.

## Flujo del MVP
Anfitrión crea (BORRADOR) → envía a revisión → admin aprueba (ACTIVA) → anfitrión define disponibilidad → huésped busca → cotiza → reserva inmediata → (cancela con política) → reseña tras el check-out.
