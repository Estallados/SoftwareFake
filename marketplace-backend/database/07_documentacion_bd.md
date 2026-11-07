# Base de datos del Marketplace

Reconstrucción para PostgreSQL 13 o superior, a partir de las entidades, repositorios y servicios del backend.

## 1. Cómo ejecutarla

```sql
CREATE DATABASE marketplace ENCODING 'UTF8';
```

**Base de prueba** (con datos semilla y verificación):

```powershell
cd marketplace-backend\database
foreach ($f in '01_schema_ddl','02_indexes','03_triggers','03b_triggers_validacion','04_procedures') {
  psql -v ON_ERROR_STOP=1 --single-transaction -U postgres -d marketplace -f "$f.sql"
  if (-not $?) { break }
}
# La semilla trae reservas ya confirmadas y pagos ya reembolsados: se carga en "modo carga".
$env:PGOPTIONS = '-c marketplace.carga_datos=on'
psql -v ON_ERROR_STOP=1 -U postgres -d marketplace -f 05_seed_data.sql
Remove-Item Env:PGOPTIONS
psql -v ON_ERROR_STOP=1 -U postgres -d marketplace -f 99_verificacion.sql
```

**Producción:** solo `01` a `04` y después `08_produccion.sql` (zona horaria, permisos y usuario propio del backend; las instrucciones están en su encabezado). `03`, `03b`, `04` y `08` se pueden ejecutar sobre una base que ya tiene datos y repetir sin problema.

Para **borrar los datos y volver a poblar** una base de prueba sin recrear el esquema:

```powershell
psql -v ON_ERROR_STOP=1 -U postgres -d marketplace -f 00_limpiar_datos.sql
$env:PGOPTIONS = '-c marketplace.carga_datos=on'
psql -v ON_ERROR_STOP=1 -U postgres -d marketplace -f 05_seed_data.sql
Remove-Item Env:PGOPTIONS
```

`99_verificacion.sql` debe terminar con el aviso `VERIFICACION CORRECTA` y no deja cambios. Depende de los ids de la semilla. `06_queries.sql` se ejecuta aparte, cuando quieras consultar.

**No ejecutes `05_seed_data.sql`, `00_limpiar_datos.sql` ni `99_verificacion.sql` en producción:** la semilla crea usuarios, incluido un administrador, con una contraseña conocida.

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
| **Consistencia** | PK, FK, `UNIQUE`, `CHECK`, `EXCLUDE` y 20 triggers (sección 6) |
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
| `trg_mensaje_inmutable` | `mensaje`: UPDATE, DELETE | Los mensajes no se editan ni se borran |
| `trg_cotizacion_proteger`, `trg_reserva_proteger`, `trg_pago_proteger`, `trg_cancelacion_proteger`, `trg_resena_proteger`, `trg_conversacion_proteger` | UPDATE, DELETE | No se borran, y sus datos fijos no cambian. Solo cambia lo que cambia el backend: estados, `cotizacion.id_reserva`, `pago.detalle` y el reembolso. Usan una función genérica, `fn_proteger_registro`, que recibe las columnas protegidas como argumentos |
| `trg_reserva_transicion` | `reserva`: UPDATE de `estado` | Solo transiciones válidas, y `CONFIRMADA` exige un pago `APROBADO` |
| `trg_pago_transicion` | `pago`: UPDATE | Solo transiciones válidas; el reembolso solo existe en un pago `REEMBOLSADO` y no cambia después |
| `trg_resena_estancia_completada` | `resena`: INSERT | Solo sobre estancias completadas |
| `trg_cancelacion_reserva_cancelada` | `cancelacion`: INSERT, **diferido hasta el COMMIT** | La reserva queda `CANCELADA`. Es diferido porque el backend inserta la cancelación antes del UPDATE de la reserva y `sp_cancelar_reserva` lo hace al revés |

Principios comunes:
- **Validan, no corrigen.** Ningún trigger reescribe un valor: si algo no cuadra, la operación se rechaza. Así lo que Hibernate tiene en memoria es siempre lo que quedó guardado, y un error no pasa en silencio.
- **Mismos códigos de error en todos:** `restrict_violation` (modificar o borrar algo prohibido), `check_violation` (regla de negocio) y `foreign_key_violation` (falta el registro relacionado). Spring los recibe como `DataIntegrityViolationException`.
- **Se pueden repetir** sobre una base con datos (`CREATE OR REPLACE` y `DROP TRIGGER IF EXISTS`).

No se crearon triggers de `fecha_actualizacion`, historial o auditoría automática porque los servicios ya lo hacen con datos que un trigger no conoce.

### Validaciones al insertar (`03b_triggers_validacion.sql`)

Son una segunda barrera frente a SQL directo: el backend ya valida lo mismo y responde con sus propios mensajes.

| Trigger | Tabla | Coherencia interna (siempre) | Estado vigente (no en modo carga) |
|---|---|---|---|
| `trg_validar_cotizacion` | `cotizacion` | `precio_base`, `valor_limpieza` y `valor_servicio` con el redondeo de `CotizacionService.calcular` | Alojamiento activo, capacidad, precio y porcentajes vigentes (como `TarifaService`) |
| `trg_validar_reserva` | `reserva` | Huésped, alojamiento, fechas, huéspedes y total iguales a los de la cotización | Nace en `PENDIENTE_PAGO`; alojamiento activo y ajeno; huésped `ACTIVO`; cotización sin usar (bloqueada con `FOR UPDATE`) |
| `trg_validar_pago` | `pago` | Proveedor válido, `valor` igual al total de la reserva y sin datos de reembolso salvo si está `REEMBOLSADO` | No nace `REEMBOLSADO` |
| `trg_validar_cancelacion` | `cancelacion` | Reserva no pendiente; cancela el huésped o un administrador; `valor_reembolso` con el redondeo de `CancelacionService`; `estado_final_reserva` = `CANCELADA` | |
| `trg_validar_mensaje` | `mensaje` | El autor participa en la conversación | |

**Modo carga.** Para cargar datos históricos (la semilla o una migración) se activa `marketplace.carga_datos=on` en la sesión (`PGOPTIONS` o `SET LOCAL`). Omite las reglas del estado vigente, pero no la coherencia interna. No es un control de seguridad, porque cualquier sesión puede activarlo: la seguridad la dan los permisos de `08_produccion.sql`.

**Consecuencia aceptada:** si el anfitrión cambia el precio o el administrador la tarifa justo mientras alguien cotiza, esa cotización se rechaza y hay que volver a cotizar.

No validan el vencimiento de la cotización: compararía la hora de la base con la del backend. `08_produccion.sql` iguala la zona horaria de la base; la JVM debe usar la misma (`-Duser.timezone=America/Bogota`).

## 7. Procedimientos

Son para mantenimiento desde SQL; **el backend no los llama**.

| Rutina | Uso | Límite |
|---|---|---|
| `sp_cancelar_reserva` | Cancela con las reglas de `CancelacionService` | Rechaza pagos de Stripe: esos solo desde la API |
| `sp_registrar_devolucion_simulada` | Devuelve un pago simulado | Rechaza pagos de Stripe; solo reservas `CANCELADA` y hasta el valor de su cancelación |
| `sp_actualizar_estado_reserva` | Pone en revisión o resuelve una revisión | Solo administradores activos; confirmar exige un pago aprobado |
| `sp_agregar_disponibilidad` | Agrega un periodo | Anfitrión o administrador |
| `sp_purgar_idempotencia` | Borra registros antiguos; el backend nunca los limpia | Conserva los `EN_PROCESO` |
| `fn_reservas_usuario` | Consulta paginada de reservas | Es función porque un procedimiento no devuelve filas |

No interfieren con las transacciones de Spring: no hacen `COMMIT` y Spring no los invoca. En producción, `08_produccion.sql` le quita al usuario del backend el permiso de ejecutarlos.

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
- `03_triggers.sql`, `03b_triggers_validacion.sql`, `04_procedures.sql` y `08_produccion.sql` (PL/pgSQL), incluida su revisión del 2026-11-07.
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
