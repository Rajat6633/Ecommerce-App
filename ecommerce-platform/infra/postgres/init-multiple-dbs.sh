#!/bin/bash
# ============================================================================
#  Creates one database per microservice from POSTGRES_MULTIPLE_DATABASES
#  (comma-separated). Runs once on first container start via
#  /docker-entrypoint-initdb.d. Each DB is owned by POSTGRES_USER.
# ============================================================================
set -euo pipefail

create_database() {
    local db="$1"
    echo "  -> creating database '$db'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        SELECT 'CREATE DATABASE "$db"'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
        GRANT ALL PRIVILEGES ON DATABASE "$db" TO "$POSTGRES_USER";
EOSQL
}

if [ -n "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
    echo "==> Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
    IFS=',' read -ra DBS <<< "$POSTGRES_MULTIPLE_DATABASES"
    for db in "${DBS[@]}"; do
        create_database "$(echo "$db" | xargs)"   # xargs trims whitespace
    done
    echo "==> All databases created."
fi
