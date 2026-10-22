# Routalyx – Development Log

## 22. Oktober 2026

### Aktiver Arbeitszweig

`feat/docker-test-infrastructure`

### Git-Stand

- Commit `a017a7b`: `test: cover vehicle API validation`
- Die API-Validierungstests wurden separat auf `feat/api-validation` committet.
- Die Infrastrukturänderungen werden auf `feat/docker-test-infrastructure` vorbereitet.
- Die Infrastrukturänderungen sind noch nicht committet.

### Umgesetzte Änderungen

- Docker Compose um eine separate PostgreSQL-Testdatenbank `postgres-test` erweitert.
- Einen `test`-Dienst für Maven-Tests im Container ergänzt.
- Einen `app`-Dienst zum Bauen und Starten der Spring-Boot-Anwendung ergänzt.
- Datenbankverbindungen über Umgebungsvariablen konfigurierbar gemacht.
- Ein Multi-Stage-Dockerfile für Build und Laufzeit angelegt.
- `scripts/test.sh` zum Starten der Testdatenbank, Ausführen der Tests und anschließenden Stoppen angelegt.
- `.dockerignore` zum Ausschluss lokaler Konfigurationen, Git-Daten, Build-Artefakte und nicht benötigter Dateien angelegt.
- `docs/LEARNING_JOURNEY.md` als fortlaufende Lerndokumentation angelegt.

### Verifikation

- `bash -n scripts/test.sh`: erfolgreich.
- `git diff --check`: keine Fehler gemeldet.
- `docker compose config --quiet`: erfolgreich.
- Maven-Testlauf im Docker-Container: 18 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungene Tests.
- Maven-Ergebnis: `BUILD SUCCESS`.
- `postgres-test` wurde nach dem Testlauf gestoppt.
- Docker-Image für die Anwendung wurde erfolgreich gebaut.
- Die Spring-Boot-Anwendung wurde gestartet.
- `GET /actuator`: HTTP 200.
- `GET /api/v1/vehicles`: HTTP 200.
- `.env` wird von Git ignoriert.

### Offene Punkte

- Fehlerfall des Testskripts gezielt prüfen.
- Docker-Build und Laufzeitkonfiguration abschließend reviewen.
- Flyway-Metadatenwarnung HTTP 401 untersuchen.
- Spring-Warnung zu `spring.jpa.open-in-view` bewusst bewerten.
- Alle Änderungen und neuen Dateien abschließend prüfen.
- Infrastrukturänderungen sinnvoll gruppieren, testen und committen.
- Dokumentation nach dem Infrastruktur-Commit mit dem tatsächlichen Git-Stand abgleichen.

### Nächster Schritt

Die vollständigen Änderungen prüfen, die vorgesehenen Dateien gezielt stagen, den Staging-Diff kontrollieren und anschließend einen separaten Infrastruktur-Commit erstellen.