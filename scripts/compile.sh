#!/usr/bin/env bash

set -Eeuo pipefail

echo "==> Kompiliere Routalyx lokal ..."

./mvnw -B -DskipTests compile

echo
echo "==> Kompilierung erfolgreich."
