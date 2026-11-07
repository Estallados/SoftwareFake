-- =============================================================================
-- Marketplace - 08_produccion.sql
-- Ajustes de la base de producción. Se ejecuta UNA vez, como dueño de la base
-- (o superusuario), después de 01 a 04. Se puede repetir sin problema.
--   psql -v ON_ERROR_STOP=1 -U postgres -d marketplace -f 08_produccion.sql
--
-- Después:
--   1. Asigna la contraseña sin dejarla en ningún archivo:
--        psql -U postgres -d marketplace -c "\password marketplace_app"
--   2. En el backend: DB_USERNAME=marketplace_app y DB_PASSWORD=<esa contraseña>.
--   3. Arranca la JVM en la misma zona horaria de la base:
--        -Duser.timezone=America/Bogota   (o la variable TZ=America/Bogota)
--
-- Por qué un usuario propio: el dueño de una tabla puede desactivar sus
-- triggers (ALTER TABLE ... DISABLE TRIGGER) y vaciarla con TRUNCATE. Si el
-- backend se conecta como dueño, la inmutabilidad de la auditoría depende solo
-- de que nadie lo haga. Con este usuario, la base lo impide.
-- =============================================================================
SET client_encoding = 'UTF8';

-- ---- Zona horaria -----------------------------------------------------------
-- Las columnas son TIMESTAMP sin zona y los triggers y procedimientos usan
-- CURRENT_DATE y LOCALTIMESTAMP; el backend usa LocalDate.now() de la JVM.
-- Deben coincidir o las reglas de fechas (reseñas, cancelaciones, estancias
-- completadas) se desfasan un día. Aplica a las conexiones nuevas.
DO $$
BEGIN
    EXECUTE format('ALTER DATABASE %I SET timezone TO %L', current_database(), 'America/Bogota');
END;
$$;

-- ---- Esquema public cerrado ---------------------------------------------------
-- Antes de PostgreSQL 15 cualquier usuario puede crear objetos en public y
-- suplantar funciones a través del search_path.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- ---- Usuario del backend ------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'marketplace_app') THEN
        CREATE ROLE marketplace_app LOGIN;
    END IF;
END;
$$;
ALTER ROLE marketplace_app NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;

DO $$
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO marketplace_app', current_database());
END;
$$;
GRANT USAGE ON SCHEMA public TO marketplace_app;

-- Datos: lectura y escritura. Los triggers deciden qué UPDATE y DELETE proceden.
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO marketplace_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO marketplace_app;
-- Auditoría e historial: solo se agregan filas.
REVOKE UPDATE, DELETE, TRUNCATE ON evento_auditoria, historial_estado_alojamiento FROM marketplace_app;

-- Las tablas que se creen en el futuro con el mismo dueño reciben los mismos permisos.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO marketplace_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO marketplace_app;

-- Los procedimientos de 04_procedures.sql son de mantenimiento: el backend no los
-- llama. Solo los ejecuta el dueño. (Las funciones de los triggers no se tocan:
-- fn_es_carga_datos se invoca con los permisos del usuario que hace el INSERT.)
REVOKE EXECUTE ON ALL PROCEDURES IN SCHEMA public FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION fn_reservas_usuario(BIGINT, INTEGER, INTEGER) FROM PUBLIC;

-- ---- Comprobación -----------------------------------------------------------
DO $$
DECLARE
    v_dueno BOOLEAN;
BEGIN
    SELECT bool_or(tableowner = 'marketplace_app') INTO v_dueno
      FROM pg_tables WHERE schemaname = 'public';
    IF v_dueno THEN
        RAISE WARNING 'marketplace_app es dueño de alguna tabla: puede desactivar triggers. Cambia el dueño (ALTER TABLE ... OWNER TO).';
    END IF;
    RAISE NOTICE 'Producción configurada. Falta: \password marketplace_app y DB_USERNAME en el backend.';
END;
$$;
