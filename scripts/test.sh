#!/usr/bin/env bash

set -Eeuo pipefail

cleanup() {
    local test_exit_code=$?

    trap - EXIT

    echo
    echo "==> Stoppe die Testdatenbank ..."

    if ! docker compose stop postgres-test; then
        echo "WARNUNG: Die Testdatenbank konnte nicht gestoppt werden." >&2

        if [[ "$test_exit_code" -eq 0 ]]; then
            test_exit_code=1
        fi
    fi

    exit "$test_exit_code"
}

trap cleanup EXIT

echo "==> Starte die Testdatenbank ..."
docker compose up -d postgres-test

echo
echo "==> Führe die Maven-Tests im Docker-Container aus ..."
docker compose run --rm test
