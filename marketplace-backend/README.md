# Marketplace de alojamientos — Backend (Spring Boot)

API REST con JWT. Java 17, Spring Boot 3.3, PostgreSQL, Maven.

## Ejecutar
1. Crear la base: `CREATE DATABASE marketplace;`
2. Variables de entorno (opcionales, hay valores de desarrollo): `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (min. 32 caracteres), `ADMIN_EMAIL`, `ADMIN_PASSWORD`.
3. `mvn spring-boot:run`
4. Swagger UI: http://localhost:8080/swagger-ui.html

Admin inicial por defecto: `admin@marketplace.local` / `Admin#12345` (cámbiala).

## Paquetes (co.edu.unbosque)
`controller` · `service` · `repository` · `model` · `dto` · `security` · `configuration` · `exception` · `util`

## Roles
GUEST (huésped), HOST (anfitrión), ADMIN. El registro público solo permite GUEST o HOST.

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
