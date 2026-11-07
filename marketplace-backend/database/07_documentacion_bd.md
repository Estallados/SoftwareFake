# Base de datos del Marketplace

Reconstrucción para PostgreSQL 13 o superior, a partir de las entidades, repositorios y servicios del backend.

## 1. Cómo ejecutarla

```sql
CREATE DATABASE marketplace ENCODING 'UTF8';
```

```powershell
cd marketplace-backend\database
foreach ($f in '01_schema_ddl','02_indexes','03_triggers','03b_triggers_validacion','04_procedures','05_seed_data','99_verificacion') {
  psql -v ON_ERROR_STOP=1 -U postgres -d marketplace -f "$f.sql"
  if (-not $?) { break }
}
```

Para **borrar los datos y volver a poblar** sin recrear el esquema:

```powershell
psql -v ON_ERROR_STOP=1 -U postgres -d marketplace -f 00_limpiar_datos.sql
psql -v ON_ERROR_STOP=1 -U postgres -d marketplace -f 05_seed_data.sql
```

`99_verificacion.sql` debe terminar con el aviso `VERIFICACION CORRECTA` y no deja cambios. `06_queries.sql` se ejecuta aparte, cuando quieras consultar.

**No ejecutes `05_seed_data.sql` en producción:** crea usuarios, incluido un administrador, con una contraseña conocida.

### Usuarios de prueba
La contraseña de todos es `Demo#2026`.

| Id | Correo | Rol | Para probar |
|---|---|---|---|
| 1 | `admin@marketplace.local` | ADMIN | Administración, dashboard y auditoría |
| 2 | `laura.gomez@marketplace.test` | ANFITRION | Publicaciones y reservas recibidas |
| 3 | `carlos.ruiz@marketplace.test` | ANFITRION | Publicaciones pausadas, en revisión y bloqueadas |
| 4 | `ana.torres@marketplace.test` | HUESPED | Reservas, reseñas y favoritos |
| 5 | `diego.mora@marketplace.test` | HUESPED | Reservar la cotización 8 y pagos rechazados |
| 6 | `sofia.perez@marketplace.test` | HUESPED, RESTRINGIDO | Rechazo de inicio de sesión |
| 7 | `pedro.diaz@marketplace.test` | HUESPED, sin verificar | Rechazo por correo sin verificar |

### La clave de cifrado
El correo se guarda cifrado y `correo_hash` se deriva de `AES_SECRET_KEY`, así que estos usuarios dependen de la clave. Hay dos opciones:

- **Usar la clave compartida del equipo** con la que se generaron (pídela al equipo; no se guarda en el repositorio). Defínela en la variable de entorno `AES_SECRET_KEY` antes de arrancar el backend.
- **Usar otra clave:** con ella definida en el entorno, genera y aplica los cambios después de `05_seed_data.sql`:

```powershell
java herramientas\RecifrarUsuariosSemilla.java > recifrar.sql
psql -v ON_ERROR_STOP=1 -U postgres -d marketplace -f recifrar.sql
```

## 2. Modelo

Hay 20 tablas: 19 entidades JPA más `alojamiento_amenidad` (`@ElementCollection`).

| Área | Tablas |
|---|---|
| Identidad | `usuario` |
| Catálogo | `alojamiento`, `alojamiento_amenidad`, `servicio_adicional`, `periodo_disponibilidad`, `historial_estado_alojamiento` |
| Configuración | `politica_cancelacion`, `configuracion_tarifa` |
| Reservas | `cotizacion`, `reserva`, `pago`, `cancelacion` |
| Interacción | `resena`, `favorito`, `conversacion`, `mensaje`, `notificacion` |
| Trazabilidad | `evento_auditoria`, `registro_exportacion`, `registro_idempotencia` |

Los nombres salen de la estrategia por defecto de Spring (`configuracionTarifa` → `configuracion_tarifa`, `precioNoche` → `precio_noche`). El modelo no usa `@ManyToMany` ni `@OneToMany`: las relaciones son `@ManyToOne` más la colección de amenidades.

### Tablas que no están, y por qué

| Tabla | Motivo |
|---|---|
| `imagen` | Los datos viven en Cloudinary y se cargan en memoria (`ImagenRepository`) |
| `token_recuperacion`, `codigo_verificacion` | Se guardan en memoria |
| `archivo_imagen` | Eliminada del código en el commit `598b01a` |

Esto refleja los cambios de la copia de trabajo, **aún sin commit**. Si los descartas, el backend volverá a necesitar `imagen`, `token_recuperacion` y `codigo_verificacion`, y este DDL se queda corto: ejecuta el de la sección 9.

### Decisiones que evitan romper el backend
- **`NOT NULL` solo donde todos los flujos asignan el valor.** Un borrador solo exige título, estado, anfitrión y fecha. `totp_habilitado` admite NULL porque `AdminInicializador` no lo asigna, y Hibernate envía NULL explícito, así que un `DEFAULT` no bastaría.
- **`reserva.id_cotizacion` no es `UNIQUE`.** Si un pago falla, `ReservaService.cerrar` libera la cotización y esta puede generar otra reserva.
- **`desglose` es `TEXT` y no `JSONB`.** La entidad lo mapea como `String`, y `JSONB` haría fallar los `INSERT`.
- **Enumeraciones con `VARCHAR` + `CHECK`**, porque las entidades usan `EnumType.STRING`.
- **`evento_auditoria.id_usuario` no es FK.** La entidad no lo relaciona y la auditoría debe sobrevivir al recurso.
- **La FK circular** `cotizacion` ↔ `reserva` se añade con `ALTER TABLE` al final, y la semilla inserta en orden y luego actualiza.

## 3. Normalización

| Forma | Estado | Detalle |
|---|---|---|
| **1FN** | Cumple, con una salvedad | Amenidades en tabla propia. `desglose` guarda JSON, pero es una copia congelada que solo se lee completa |
| **2FN** | Cumple | Las únicas claves compuestas son las de `alojamiento_amenidad`, sin atributos adicionales |
| **3FN** | Cumple, salvo las redundancias de abajo | |
| **BCNF** | Cumple donde se cumple 3FN | Los determinantes son claves candidatas: `correo_hash` en `usuario`, `nombre` en `politica_cancelacion`, y `(id_usuario, operacion, clave)` en idempotencia |

### Redundancias que se conservan

Eliminarlas exige cambiar entidades y consultas, así que se controlan con restricciones:

| Columna | Depende de | Por qué sigue | Cómo se controla |
|---|---|---|---|
| `noches` | Las fechas | Lo usan entidades y DTO | `CHECK` |
| `cotizacion.total` y `valor_*` | Precio y porcentajes | Copia congelada: no debe cambiar si cambia la tarifa | `CHECK` con la fórmula de `CotizacionService.calcular` |
| `reserva.total`, fechas | `id_cotizacion` | Copia congelada; una cotización puede generar más de una reserva | Lo comprueba `99_verificacion.sql` |
| `resena.id_alojamiento`, `resena.id_usuario` | `id_reserva` | Consultas por alojamiento sin JOIN | FK compuesta hacia `reserva` |
| `conversacion.id_anfitrion` | `id_alojamiento` | `buscarDeParticipante` | FK compuesta hacia `alojamiento` |
| `cancelacion.politica` | Política del alojamiento | Historial: la política puede cambiar | Copia intencional |

## 4. Integridad y ACID

| Propiedad | Cómo se garantiza |
|---|---|
| **Atomicidad** | `@Transactional` y `TransactionTemplate` en el backend. Cada `CALL` de procedimiento es atómico |
| **Consistencia** | PK, FK, `UNIQUE`, `CHECK`, `EXCLUDE` y 4 reglas con triggers |
| **Aislamiento** | `READ COMMITTED` (el predeterminado) más bloqueo `FOR UPDATE` en `buscarParaReservar` y `EXCLUDE` como última defensa |
| **Durabilidad** | WAL de PostgreSQL. No desactives `synchronous_commit` ni `fsync` |

### Operaciones críticas

| Operación | Transacción | Protección ante concurrencia |
|---|---|---|
| Cotizar | `cotizar` en una transacción | No reserva fechas; se revalida al reservar |
| Crear reserva | `reservarPendiente`: reserva y cotización juntas | `FOR UPDATE` sobre el alojamiento y `ex_reserva_sin_cruces` |
| Pago | **Fuera de transacción a propósito**, para no retener el bloqueo durante la llamada a Stripe | `registro_idempotencia` y cotización de un solo uso |
| Cerrar reserva | `cerrar`: estado, notificaciones y auditoría | Transacción propia |
| Cancelar y devolver | `cancelar`: reserva, cancelación y pago juntos | `uq_cancelacion_reserva` |
| Disponibilidad | Servicio con `@Transactional` | `ex_periodo_sin_cruces` |
| Auditoría | Inserción por `AuditoriaService` | Inmutable por trigger |

### Riesgos del backend detectados (no se cambió el código)
1. **Doble cancelación simultánea:** `CancelacionService.cancelar` no bloquea la reserva. `uq_cancelacion_reserva` impide la segunda fila, pero la llamada de devolución a Stripe ocurre antes del commit. Recomendación: leer la reserva con `FOR UPDATE`.
2. **Devolución y commit:** si Stripe devuelve el dinero y luego falla el commit, la devolución queda hecha sin registro local. Recomendación: conciliar por referencia.
3. **Pago de más de 15 minutos:** una pendiente deja de ocupar fechas a los 15 minutos. Si otra reserva toma esas fechas y el primer pago se aprueba después, `ex_reserva_sin_cruces` rechaza la confirmación ya cobrada. Es muy improbable, pero exigiría una devolución manual.
4. **Bloquear un alojamiento:** pasa las pendientes a `EN_REVISION`. En el minuto que tarda en cancelarse una pendiente vencida, puede chocar con una reserva confirmada; el bloqueo falla y basta con reintentarlo.

## 5. Índices

`02_indexes.sql` explica junto a cada índice la consulta que atiende. Hay índices simples, compuestos, parciales (pendientes, no leídas) y por expresión (`LOWER(ciudad)`, porque `AlojamientoEspecificacion` compara en minúsculas). Los `UNIQUE` y `EXCLUDE` ya crean los suyos, y no se duplican.

## 6. Triggers

| Trigger | Tabla y evento | Finalidad |
|---|---|---|
| `trg_evento_inmutable`, `trg_evento_sin_truncate` | `evento_auditoria`: UPDATE, DELETE, TRUNCATE | Auditoría inmutable también frente a SQL directo |
| `trg_historial_inmutable`, `trg_historial_sin_truncate` | `historial_estado_alojamiento`: igual | Historial inmutable |
| `trg_reserva_transicion` | `reserva`: UPDATE de `estado` | Solo transiciones válidas |
| `trg_pago_transicion` | `pago`: UPDATE de `estado` | Solo transiciones válidas |
| `trg_resena_estancia_completada` | `resena`: INSERT | Solo sobre estancias completadas |

Los de arriba no modifican filas, así que no hay recursión ni diferencias con lo que Hibernate cree que escribió. No se crearon triggers de `fecha_actualizacion`, historial o auditoría automática porque los servicios ya lo hacen con datos que un trigger no conoce. Tampoco uno que exija la reserva cancelada al insertar en `cancelacion`: Hibernate ejecuta los `INSERT` antes que los `UPDATE`, y fallaría.

### Validaciones al insertar (`03b_triggers_validacion.sql`)

Son una segunda barrera frente a SQL directo: el backend ya valida lo mismo y responde con sus propios mensajes. Lo que calculan coincide con lo que envía el backend, así que Hibernate no queda desincronizado.

| Trigger | Tabla | Valida | Calcula |
|---|---|---|---|
| `trg_validar_cotizacion` | `cotizacion` | Alojamiento activo, capacidad y fechas | `noches`, `precio_noche`, `total` |
| `trg_validar_reserva` | `reserva` | En `PENDIENTE_PAGO`: alojamiento activo, rol y estado del huésped, cotización propia y sin usar | `noches`, y fechas y total de la cotización |
| `trg_validar_pago` | `pago` | Reserva existente y proveedor válido | `valor` |
| `trg_validar_cancelacion` | `cancelacion` | Reserva no pendiente; cancela el huésped o un administrador | `valor_reembolso`, `estado_final_reserva` |
| `trg_validar_mensaje` | `mensaje` | El autor participa en la conversación | `contenido` sin espacios sobrantes |

No validan el vencimiento de la cotización: compararía la hora de la base con la del backend, que pueden estar en zonas horarias distintas.

## 7. Procedimientos

Son para mantenimiento desde SQL; **el backend no los llama**.

| Rutina | Uso | Límite |
|---|---|---|
| `sp_cancelar_reserva` | Cancela con las reglas de `CancelacionService` | Rechaza pagos de Stripe: esos solo desde la API |
| `sp_registrar_devolucion_simulada` | Devuelve un pago simulado | Rechaza pagos de Stripe |
| `sp_actualizar_estado_reserva` | Pone en revisión o resuelve una revisión | Solo administradores |
| `sp_agregar_disponibilidad` | Agrega un periodo | Anfitrión o administrador |
| `sp_purgar_idempotencia` | Borra registros antiguos; el backend nunca los limpia | Conserva los `EN_PROCESO` |
| `fn_reservas_usuario` | Consulta paginada de reservas | Es función porque un procedimiento no devuelve filas |

No interfieren con las transacciones de Spring: no hacen `COMMIT` y Spring no los invoca.

## 8. Validación realizada

**PostgreSQL no se pudo ejecutar en el equipo donde se generó esto.** Las pruebas se hicieron con H2, ya incluido en el proyecto, traduciendo lo que H2 no admite. Por tanto:

| Qué | Cómo se probó | Resultado |
|---|---|---|
| Nombres y tipos frente a las entidades | Hibernate con `ddl-auto=validate` | Correcto |
| `01_schema_ddl.sql` (sin `EXCLUDE`) | Ejecutado en H2 | Correcto |
| `05_seed_data.sql` | Cargado con FK y `CHECK` activos | Correcto |
| `06_queries.sql` | Las 29 consultas ejecutadas | Correcto |
| `02_indexes.sql` | Ejecutado en H2 | 33 de 39; los otros 6 usan sintaxis solo de PostgreSQL |
| Flujos de la API con la semilla | Login, reservar, cancelar con devolución, registro | Correcto |
| `RecifrarUsuariosSemilla.java` | Con otra clave, login y descifrado reales | Correcto |
| Suite del backend (251 pruebas) | Sobre este esquema | 233 pasan; 16 por datos de prueba incompletos; 2 ya fallaban antes |

- **Las 16 de datos de prueba:** usan `new Reserva()` y `new Usuario()` sin campos que el código de producción sí asigna. Con esas columnas relajadas solo en la copia de prueba, esos flujos pasan. No afecta a la suite normal, que genera su propio esquema con Hibernate.
- **Las 2 que ya fallaban:** `sinClientIdDeGoogleElInicioConGoogleResponde503` y `coincideConElVectorDePruebaDelRfc6238`.

### Sin probar en un PostgreSQL real
- `03_triggers.sql` y `04_procedures.sql` (PL/pgSQL).
- Las restricciones `EXCLUDE` y los 6 índices específicos.
- `99_verificacion.sql`, que existe precisamente para comprobar todo lo anterior.

**El trabajo no está validado por completo hasta ejecutar la sección 1 en PostgreSQL y ver `VERIFICACION CORRECTA`.**

## 9. Tablas opcionales

Solo si descartas los cambios sin commit que sacaron estos datos de la base. Ejecuta esto después de `01_schema_ddl.sql`:

```sql
CREATE TABLE imagen (
    id_imagen       BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    url             VARCHAR(2048) NOT NULL,
    id_alojamiento  BIGINT NOT NULL REFERENCES alojamiento (id_alojamiento) ON DELETE CASCADE ON UPDATE RESTRICT,
    principal       BOOLEAN,
    ruta_archivo    VARCHAR(512)
);
CREATE INDEX idx_imagen_alojamiento ON imagen (id_alojamiento, id_imagen);

CREATE TABLE token_recuperacion (
    id_token          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_usuario        BIGINT NOT NULL REFERENCES usuario (id_usuario) ON DELETE CASCADE ON UPDATE RESTRICT,
    token_hash        VARCHAR(64) NOT NULL UNIQUE,
    fecha_expiracion  TIMESTAMP(6) NOT NULL,
    usado             BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_token_usuario ON token_recuperacion (id_usuario) WHERE usado = FALSE;

CREATE TABLE codigo_verificacion (
    id_codigo         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    id_usuario        BIGINT NOT NULL REFERENCES usuario (id_usuario) ON DELETE CASCADE ON UPDATE RESTRICT,
    codigo_hash       VARCHAR(64) NOT NULL,
    fecha_creacion    TIMESTAMP(6) NOT NULL,
    fecha_expiracion  TIMESTAMP(6) NOT NULL,
    intentos          INTEGER NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    usado             BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_codigo_usuario ON codigo_verificacion (id_usuario) WHERE usado = FALSE;
```

Este bloque no se ha ejecutado, y `99_verificacion.sql` espera 20 tablas: con estas serían 23.
