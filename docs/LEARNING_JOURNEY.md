# Routalyx – Learning Journey

**Stand:** 22. Oktober 2026
**Projekt:** Routalyx – Flottenmanagement und Analytics
**Aktueller Entwicklungsbranch:** `feat/docker-test-infrastructure`
**API-Validierung:** separat committet unter `a017a7b`

## 1. Projektziel und Lernansatz

Routalyx dient als Praxisprojekt zur Entwicklung einer wartbaren Java-Anwendung und zum systematischen Aufbau von DevOps-Kenntnissen.

Der Lernweg verbindet Softwareentwicklung, automatisierte Tests, Datenbankmigrationen, Containerisierung und Git-basierte Zusammenarbeit. Änderungen werden in kleinen Arbeitspaketen umgesetzt und anhand von Befehlen, Testergebnissen und Git-Diffs überprüft.

## 2. Technische Grundlage

Der dokumentierte Projektstand umfasst:

- Java 21
- Spring Boot 4.0.8
- Maven Wrapper (`./mvnw`)
- PostgreSQL 17 Alpine
- Docker Compose
- Flyway für Datenbankmigrationen
- Git mit getrennten Feature-Branches für API-Validierung und Infrastruktur

Die Fahrzeugfunktionalität verwendet REST-Endpunkte und Integrationstests. Die Datenbankstruktur wird über Flyway-Migrationen verwaltet.

## 3. Datenbankkonfiguration mit Umgebungsvariablen

Die Spring-Konfiguration verwendet folgende Eigenschaften:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Für URL und Benutzername existieren Standardwerte. Beim Passwort kann auf `POSTGRES_PASSWORD` zurückgegriffen werden.

**Warum?** Dieselbe Anwendung kann dadurch lokal oder in einem Container laufen, ohne die Datenbankverbindung fest in den Quellcode einzutragen.

Zugangsdaten gehören nicht in Git. Die lokale `.env`-Datei wird durch eine Git-Ignore-Regel ausgeschlossen. Auf die Ausgabe vollständig aufgelöster Compose-Konfigurationen ist zu verzichten, wenn sie Geheimnisse enthalten könnte.

## 4. Trennung von Entwicklungs- und Testdatenbank

Docker Compose enthält getrennte PostgreSQL-Dienste:

- `postgres`: reguläre Entwicklungsdatenbank `routalyx`
- `postgres-test`: Testdatenbank `routalyx_test`

Der Testdienst verwendet eigene Zugangsdaten. Die Anwendung im Testcontainer verbindet sich über den Compose-Dienstnamen `postgres-test` und Port 5432.

**Lernpunkt:** Innerhalb des Compose-Netzwerks wird ein Dienst über seinen Dienstnamen erreicht. `localhost` im Testcontainer würde dagegen auf den Testcontainer selbst zeigen und nicht auf PostgreSQL.

Der Healthcheck mit `pg_isready` prüft die Erreichbarkeit der Datenbank. `depends_on` mit `condition: service_healthy` verhindert, dass Compose den Testdienst startet, bevor die Testdatenbank als gesund gemeldet wird.

## 5. Dockerfile und Multi-Stage-Build

Das neue Dockerfile verwendet zwei Build-Stufen:

1. `maven:3.9-eclipse-temurin-21` kompiliert und paketiert die Anwendung.
2. `eclipse-temurin:21-jre` führt das erzeugte JAR aus.

Dadurch muss das Maven-Build-Image nicht Teil des finalen Laufzeit-Images sein.

Der Container startet die Anwendung über `java -jar /app/app.jar` und stellt Port 8080 als Container-Port bereit.

**Status:** Dockerfile angelegt. Eine separate Verifikation des finalen Images und seiner Laufzeit steht noch aus.

### Docker-Build-Kontext und `.dockerignore`

Die Datei `.dockerignore` schließt Dateien und Verzeichnisse aus, die der Docker-Build nicht benötigt. Dazu gehören lokale `.env`-Dateien, `.git/`, `target/`, IDE-Konfigurationen sowie die Verzeichnisse `docs/` und `scripts/`.

**Warum?** Der Build-Kontext soll möglichst klein bleiben. Lokale Zugangsdaten und unnötige Dateien sollen nicht an den Docker-Build übergeben werden.

Die `.dockerignore` wurde angelegt. Ihre endgültige Wirkung und der vollständige Build-Kontext sind noch abschließend zu prüfen.

## 6. Automatisierter Testlauf

Das Skript `scripts/test.sh` führt folgende Schritte aus:

1. `postgres-test` im Hintergrund starten.
2. Die Maven-Tests mit `docker compose run --rm test` ausführen.
3. Beim Beenden des Skripts über `trap` die Testdatenbank stoppen.
4. Den Test-Exitcode erhalten, damit Fehler nicht als Erfolg ausgegeben werden.

Der einmalige Testcontainer wird durch `--rm` entfernt. Der PostgreSQL-Testcontainer wird nur gestoppt und nicht automatisch gelöscht.

**Warum?** Das spart laufende Ressourcen, während der Container für spätere Testläufe wiederverwendet werden kann. Die reguläre Entwicklungsdatenbank wird vom Testskript nicht gestoppt.

### Verifikation am 22. Oktober 2026

- Shell-Syntaxprüfung mit `bash -n scripts/test.sh`: erfolgreich, keine Ausgabe.
- `git diff --check`: keine Fehler gemeldet.
- `docker compose config --quiet`: kein Fehler gemeldet.
- Vollständiger Maven-Testlauf im Docker-Container: 18 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungene Tests.
- Maven-Ergebnis: `BUILD SUCCESS`.
- Nach dem Testlauf wurde `postgres-test` erfolgreich gestoppt.

Diese Ergebnisse beziehen sich auf den dokumentierten Lauf. Ein automatisierter Testlauf nach absichtlich provoziertem Testfehler steht noch aus.

## 7. Validierung der API

Die Integrationstests prüfen, ob die API ungültige Eingaben mit HTTP 400 (Bad Request) zurückweist.

Zwei Fälle werden abgedeckt:

- `shouldRejectLicensePlateLongerThanTwentyCharacters()`: Ein Kennzeichen mit 21 Zeichen wird abgewiesen.
- `shouldRejectMissingStatus()`: Eine Anfrage ohne das erforderliche Feld `status` wird abgewiesen.

Die Tests befinden sich in `src/test/java/io/routalyx/api/fleet/VehicleApiIntegrationTest.java`.

**Lernpunkt:** Integrationstests sichern den API-Vertrag ab. Sie prüfen das Verhalten über die HTTP-Schnittstelle und nicht nur eine einzelne Java-Methode.

**Git-Status:** Die API-Validierungstests wurden mit Commit `a017a7b` (`test: cover vehicle API validation`) separat committet. Die Infrastrukturänderungen werden auf dem Branch `feat/docker-test-infrastructure` vorbereitet.
## 8. Beobachtete Warnungen und offene Punkte

Beim Maven-Lauf wurde ein HTTP-401-Fehler beim Abruf von Flyway-Metadaten von GitHub gemeldet. Der Build war dennoch erfolgreich. Die Ursache ist noch zu untersuchen.

Spring meldet außerdem, dass `spring.jpa.open-in-view` standardmäßig aktiviert ist. Die passende Einstellung soll bewusst geprüft werden.

Weitere offene Aufgaben:

- Docker-Image bauen und den Containerstart separat prüfen.
- Das Testskript auch bei fehlgeschlagenen Tests verifizieren.
- Die Compose-Konfiguration und die Änderungen am API-Test vollständig überprüfen.
- Die Dokumentation mit dem tatsächlichen Git-Stand synchronisieren.
- Den Branch vor dem Commit erneut prüfen und die Änderungen sinnvoll gruppieren.

## 9. Git- und Qualitätsprozess

Vor einem Commit werden mindestens folgende Prüfungen durchgeführt:

```bash
git status --short --branch
git diff --check
./scripts/test.sh
```

Neue, noch nicht versionierte Dateien müssen zusätzlich geprüft werden, da sie nicht im normalen `git diff` erscheinen.

Ein Commit speichert Änderungen lokal im Repository. Ein Push überträgt sie zum konfigurierten Remote-Repository. Beides sind getrennte Schritte.

## 10. Lernstand

**Umgesetzt und verifiziert:**
- getrennte Compose-Dienste für Entwicklungs- und Testdatenbank
- Umgebungsvariablen für Datenbankverbindungen
- Docker-basierten Maven-Testlauf
- Shell-Skript mit automatischem Cleanup
- erfolgreicher Testlauf mit 18 Tests
- Multi-Stage-Dockerfile angelegt und Anwendungsimage erfolgreich gebaut
- Spring-Boot-Anwendung im Container gestartet
- Actuator- und Fahrzeug-API-Endpunkte mit HTTP 200 geprüft
- `.dockerignore` angelegt
- API-Validierungstests separat committet

**Noch offen:**
- Docker-Image und Containerstart separat verifizieren
- Fehlerfall des Testskripts prüfen
- beobachtete Warnungen untersuchen
- Änderungen reviewen und committen
- CI/CD mit GitHub Actions aufbauen
- Fehlerfall des Testskripts prüfen
- Docker-Build-Kontext und `.dockerignore` abschließend überprüfen
- Flyway-Metadatenwarnung HTTP 401 untersuchen
- Spring-Warnung zu `open-in-view` bewerten
- Infrastrukturänderungen reviewen und separat committen
