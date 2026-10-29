# Routalyx – Learning Journey

**Stand:** 29. Oktober 2026
**Projekt:** Routalyx – Flottenmanagement und Analytics
**Aktueller Entwicklungsbranch:** `feat/vehicle-list-pagination`
**Aktueller Hauptbranch:** `main` – zuletzt bestätigter Commit `a1b7500`
**Letzter Meilenstein:** Vehicle API 2.0 – Pagination und Statusfilter

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

**Status:** Das Multi-Stage-Dockerfile wurde lokal erfolgreich gebaut. Das Image `routalyx:ci` wurde anschließend mit `docker image ls routalyx` nachgewiesen. Der Build verwendet zwei Stufen: Maven für das Packaging und ein schlankeres Java-Runtime-Image für die Anwendung.

**Abgrenzung:** Der erfolgreiche Image-Build bestätigt noch nicht automatisch, dass jede Laufzeitkonfiguration korrekt ist. Ein Registry-Push und ein Deployment sind noch nicht umgesetzt.

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

Diese Ergebnisse beziehen sich auf den dokumentierten Lauf.

### 6.1 Fehlerbehandlung und Cleanup verifizieren

Neben dem regulären Testlauf wurde die Fehlerbehandlung von `scripts/test.sh` kontrolliert geprüft.

**Erfolgsfall**
- 18 Tests wurden ausgeführt.
- Es gab 0 Fehler, 0 fehlgeschlagene Tests und 0 übersprungene Tests.
- Maven meldete `BUILD SUCCESS`.
- Die Testdatenbank `postgres-test` wurde anschließend gestoppt.

**Kontrollierter Fehlerfall**
- Ein temporärer Docker-Compose-Override ließ den Testcontainer absichtlich mit Exitcode `42` abbrechen.
- Das Skript gab diesen Exitcode weiter und führte seinen Cleanup-Handler aus.
- Die Testdatenbank `postgres-test` wurde gestoppt.

Damit sind die reguläre Ausführung und die grundlegende Fehlerbehandlung des Skripts lokal verifiziert. Der Fehlerfall war eine kontrollierte Shell-Simulation und kein fehlgeschlagener Maven-Test.

**Lernpunkte**
- Ein Shell-Skript muss Fehlercodes zuverlässig weitergeben.
- Ein `trap` mit `EXIT` kann Cleanup auch nach einem Fehler ausführen.
- Testinfrastruktur sollte unabhängig von der Entwicklungsdatenbank behandelt werden.
- Ein erfolgreicher Testlauf und ein kontrollierter Fehlerfall prüfen unterschiedliche Eigenschaften.

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

## 10. CI mit GitHub Actions

Die Datei `.github/workflows/ci.yml` definiert die automatisierte CI-Pipeline.

### Test-Job

Der Job `test` läuft auf einem GitHub-gehosteten Ubuntu-Runner. Er richtet Java 21 ein, verwendet Maven-Caching und startet PostgreSQL 17 Alpine als Datenbank-Service für die Tests.

Der Verifikationsbefehl lautet:

```bash
./mvnw --batch-mode --no-transfer-progress verify
```

Die Datenbankverbindung wird über Umgebungsvariablen bereitgestellt. Dadurch sind die Tests nicht von einer lokal installierten PostgreSQL-Instanz abhängig.

### Docker-Image-Build

Der Job `build-image` baut das Docker-Image auf einem GitHub-Runner:

```bash
docker build --tag routalyx:ci .
```

Die Abhängigkeit

```yaml
needs: test
```

stellt sicher, dass der Image-Build erst beginnt, wenn der Test-Job erfolgreich abgeschlossen wurde.

### Warum zwei Jobs?

Die Trennung macht die Pipeline übersichtlicher. Der Test-Job prüft die Anwendung und ihre Integration mit der Datenbank. Der zweite Job prüft, ob sich aus dem Repository ein Docker-Image bauen lässt.

GitHub Actions verwendet hierfür Runner mit eigener Laufzeitumgebung. Ein auf dem lokalen Fedora-System gebautes Image wird nicht automatisch auf den GitHub-Runner übertragen.

### Git-Workflow und Verifikation

- PR #4 integrierte den Test-Workflow in `main`.
- PR #5 ergänzte den Docker-Image-Build und wurde ebenfalls erfolgreich gemergt.
- Die GitHub-Actions-Läufe für beide Pull Requests waren laut bestätigtem Ergebnis erfolgreich.
- Der lokale Docker-Build von `routalyx:ci` war erfolgreich.

**Noch nicht umgesetzt:** Das Image wird nicht in eine Registry gepusht. Es gibt noch kein automatisiertes Deployment oder Rollback.

### Begriffe

- **CI (Continuous Integration):** Änderungen werden automatisiert gebaut und geprüft.
- **Runner:** Die Umgebung, auf der ein GitHub-Actions-Job ausgeführt wird.
- **Job-Abhängigkeit:** `needs: test` legt fest, dass der Image-Build vom erfolgreichen Test-Job abhängt.
- **Container-Registry:** Ein Dienst, in dem Docker-Images gespeichert und für andere Umgebungen bereitgestellt werden können.
- **Deployment:** Die Bereitstellung einer Anwendung in einer Zielumgebung.

### Nächste Lernziele

1. Fehlerfall von `scripts/test.sh` gezielt prüfen.
2. Flyway-Metadatenwarnung HTTP 401 untersuchen.
3. Spring-Warnung zu `spring.jpa.open-in-view` bewusst bewerten.
4. Einen Registry-Push planen und erst nach Prüfung der Zugangsdaten und Berechtigungen implementieren.
5. Darauf aufbauend einen kontrollierten Deployment-Prozess mit Verifikation und Rollback entwickeln.

## 11. Lernstand

### Umgesetzt und verifiziert

**Anwendung und Datenbank**
- Getrennte Docker-Compose-Dienste für Entwicklungs- und Testdatenbank
- Konfigurierbare Datenbankverbindungen über Umgebungsvariablen
- Flyway für Datenbankmigrationen
- API-Validierungstests für ungültige Eingaben

**Docker und Tests**
- Docker-basierter Maven-Testlauf mit `scripts/test.sh`
- Erfolgreicher Testlauf mit 18 Tests, 0 Fehlern und 0 Fehlschlägen
- Multi-Stage-Dockerfile für Build und Laufzeit
- Lokaler Docker-Image-Build mit dem Tag `routalyx:ci` erfolgreich
- Spring-Boot-Anwendung im Container gestartet und zuvor über Actuator- und Fahrzeug-API-Endpunkte mit HTTP 200 geprüft
- `.dockerignore` zum Ausschluss unnötiger Dateien und lokaler Konfigurationen

**Git und CI/CD**
- Feature-Branches und Pull Requests für Änderungen verwendet
- GitHub-Actions-Workflow für automatisierte Maven-Verifikation integriert (PR #4)
- Separater `build-image`-Job in GitHub Actions integriert (PR #5)
- `needs: test` stellt sicher, dass der Image-Build erst nach erfolgreichem Test-Job ausgeführt wird
- Beide Pull Requests erfolgreich gemergt; die CI-Läufe waren laut bestätigtem Ergebnis erfolgreich
- Lokales `main` mit `origin/main` synchronisiert; Stand `f4aa37d`

### Noch offen

**Tests und Fehlerdiagnose**
- Fehlerfall von `scripts/test.sh` gezielt prüfen
- Flyway-Metadatenwarnung HTTP 401 untersuchen
- Spring-Warnung zu `spring.jpa.open-in-view` bewusst bewerten

**Docker und Betrieb**
- Docker-Build-Kontext und `.dockerignore` bei Bedarf weiter überprüfen
- Laufzeitkonfiguration des Containers bei Änderungen erneut gezielt verifizieren

**Nächste DevOps-Meilensteine**
- Docker-Image in eine Registry veröffentlichen
- Registry-Zugangsdaten und Berechtigungen sicher konfigurieren
- Deployment-Prozess mit Verifikation und Rollback aufbauen
- Danach Kubernetes-Deployment für Routalyx vorbereiten, passend zum tatsächlichen Projektstand

### Aktuelle Abgrenzung

**Bereits umgesetzt:** automatisierte Maven-Verifikation und Docker-Image-Build in GitHub Actions.

**Noch nicht umgesetzt:** Registry-Push, automatisiertes Deployment und Rollback.

### Nächster Schritt

Die Dokumentationsänderungen abschließend prüfen und über einen separaten Commit und Pull Request in `main` integrieren. Danach den nächsten technischen Meilenstein anhand des tatsächlichen Projektstands festlegen.

## 12. Vehicle API 2.0 – Pagination und Statusfilter

**Stand: 29. Oktober 2026**

### Ziel

Die Fahrzeugliste soll auch bei wachsenden Datenmengen kontrolliert
abgerufen werden können. Die API unterstützt dafür Pagination und
einen optionalen Filter nach Fahrzeugstatus.

### Umsetzung

- `VehicleController` nimmt `page`, `size` und `status` als
  HTTP-Request-Parameter entgegen.
- `VehicleService` validiert die Seitennummer und Seitengröße.
  Zulässig sind Seitennummern ab 0 und Seitengrößen von 1 bis 100.
- `VehicleRepository` verwendet Spring Data JPA für paginierte
  Datenbankabfragen und die Filterung nach Status.
- Die Sortierung erfolgt nach Kennzeichen und anschließend nach ID.
- `VehiclePageResponse` liefert die Fahrzeugliste unter `content`
  sowie Seitennummer, Seitengröße, Gesamtanzahl, Seitenanzahl und
  Kennzeichen für die erste und letzte Seite.
- `ApiExceptionHandler` behandelt ungültige typisierte
  Request-Parameter mit einer HTTP-400-Antwort.

### API-Vertrag

Beispiel für eine Anfrage:

`GET /api/v1/vehicles?page=0&size=20&status=AVAILABLE`

Die Antwort enthält unter anderem `content`, `page`, `size`,
`totalElements`, `totalPages`, `first` und `last`.

### Verifikation

Am 29. Oktober 2026 wurde `./scripts/test.sh` ausgeführt.

- `git diff --check`: keine Fehler gemeldet.
- Maven-Testlauf: 23 Tests, 0 Fehler, 0 Fehlschläge,
  0 übersprungene Tests.
- Maven-Ergebnis: `BUILD SUCCESS`.
- Die Testdatenbank `postgres-test` wurde anschließend gestoppt.

Die Ergebnisse gelten für diesen konkreten lokalen Testlauf.
Der Feature-Branch ist damit noch nicht automatisch gemergt oder deployed.

### Lernpunkte

- Spring Data `Page` und `Pageable` ermöglichen Datenbankabfragen
  mit Seitennavigation und Gesamtmetadaten.
- Eine stabile Sortierung ist wichtig, damit Datensätze bei
  aufeinanderfolgenden Seiten möglichst konsistent verteilt werden.
- Ein HTTP-Response-Record definiert den JSON-Vertrag der API.
- Integrationstests prüfen das Verhalten über die HTTP-Schnittstelle.
- Ungültige Query-Parameter müssen kontrolliert und verständlich
  beantwortet werden.

### Offene Punkte

- Den vollständigen Git-Diff einschließlich neuer Dateien prüfen.
- Die Dokumentation und den aktuellen Branch-Stand synchronisieren.
- Die Warnungen zu Flyway-Metadaten, Mockito und
  `spring.jpa.open-in-view` separat bewerten.
- Nach abschließender Prüfung einen Commit erstellen.
