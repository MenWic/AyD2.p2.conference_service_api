-- ============================================================
-- PostgreSQL bootstrap — Conference Service
-- Proyecto AyD2 P2B
--
-- Ejecutado automáticamente por el contenedor conference-postgres
-- al iniciarse por primera vez (volumen vacío).
-- ============================================================

\set ON_ERROR_STOP on

\echo '==> Creando/asegurando usuario conference_service_user...'

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'conference_service_user') THEN
        CREATE ROLE conference_service_user LOGIN PASSWORD 'conference_password';
    ELSE
        ALTER ROLE conference_service_user WITH LOGIN PASSWORD 'conference_password';
    END IF;
END
$$;

\echo '==> Asegurando ownership de conference_db...'

ALTER DATABASE conference_db OWNER TO conference_service_user;

\echo '==> Aplicando permisos en conference_db...'

GRANT CONNECT ON DATABASE conference_db TO conference_service_user;
GRANT USAGE, CREATE ON SCHEMA public TO conference_service_user;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO conference_service_user;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO conference_service_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO conference_service_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO conference_service_user;

\echo '==> Bootstrap Conference completado.'
