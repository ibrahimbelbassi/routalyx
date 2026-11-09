# Routalyx – Learning Journey

**Stand:** 9. November 2026
**Projekt:** Routalyx – Flottenmanagement und Analytics
**Aktueller Entwicklungsbranch:** `main`
**Aktueller Hauptbranch:** `main` – Merge-Commit `42d8a21`
**Letzter Meilenstein:** Kubernetes-Deployment in kind mit GHCR-Image, PostgreSQL in Docker Compose und erfolgreicher GitHub Actions CI

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

### Registry und Deployment – aktualisierter Stand

Seit dem Kubernetes-Meilenstein wurde das Docker-Image in GHCR bereitgestellt und in der lokalen kind-Umgebung verwendet.

- GHCR-Image: `ghcr.io/ibrahimbelbassi/routalyx:sha-8cc5b17`
- Kubernetes-Deployment in kind erfolgreich durchgeführt.
- Kubernetes-Pod wurde Ready.
- Actuator Health-Endpunkt und Swagger UI über Port-Forwarding geprüft.
- PostgreSQL bleibt in Docker Compose auf dem Fedora-Host.

**Umgesetzt und verifiziert:**
- Maven-Test-Job in GitHub Actions.
- Docker-Image-Build in GitHub Actions.
- Image-Veröffentlichung in GHCR im bisherigen Workflow.
- Lokales Kubernetes-Deployment in kind.

**Noch nicht umgesetzt:**
- Automatisiertes Kubernetes-Deployment aus GitHub Actions.
- Deployment in eine externe oder produktive Kubernetes-Umgebung.
- Automatisierter Rollback-Prozess.
- Vollständiger produktiver CD-Workflow.

### Begriffe

- **CI (Continuous Integration):** Änderungen werden automatisiert gebaut und geprüft.
- **Runner:** Die Umgebung, auf der ein GitHub-Actions-Job ausgeführt wird.
- **Job-Abhängigkeit:** `needs: test` legt fest, dass der Image-Build vom erfolgreichen Test-Job abhängt.
- **Container-Registry:** Ein Dienst, in dem Docker-Images gespeichert und für andere Umgebungen bereitgestellt werden können.
- **Deployment:** Die Bereitstellung einer Anwendung in einer Zielumgebung.

### Nächste Lernziele

1. Kubernetes Deployment, ReplicaSet, Pod und Service weiter vertiefen.
2. Änderungen an ConfigMap und Deployment nachvollziehen und kontrolliert ausrollen.
3. Deployment-Rollout, Rollout-Status und Rollback mit Kubernetes kennenlernen.
4. GitHub Actions um automatisierte Deployment-Verifikation erweitern.
5. Secrets und Berechtigungen für CI/CD sicher verwalten.
6. Kubernetes Observability mit Prometheus und Grafana vorbereiten.
7. PostgreSQL vorerst weiterhin in Docker Compose betreiben und eine spätere Kubernetes-Datenbankmigration separat planen.

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
- Flyway-Metadatenwarnung HTTP 401 untersuchen.
- Spring-Warnung zu `spring.jpa.open-in-view` bewusst bewerten.
- Mockito-Warnung zur dynamischen Agent-Ladung bei Bedarf untersuchen.

**Docker und Betrieb**
- Docker-Build-Kontext und `.dockerignore` bei Bedarf weiter überprüfen.
- Laufzeitkonfiguration des Containers bei Änderungen gezielt verifizieren.

**Kubernetes**
- Rollout-Verhalten bei neuen Image-Versionen untersuchen.
- Deployment-Rollback praktisch durchführen.
- Ressourcenlimits und Probes bei Last und Fehlerfällen untersuchen.
- Kubernetes-Konfiguration schrittweise professionalisieren.

**CI/CD**
- Automatisiertes Deployment aus GitHub Actions ergänzen.
- Deployment-Status und Health nach einem Rollout automatisch überprüfen.
- Rollback bei einem fehlgeschlagenen Deployment planen.

**Observability**
- Prometheus und Grafana in der lokalen Lernumgebung vorbereiten.
- Metrics, Logs und Traces voneinander unterscheiden und praktisch untersuchen.

### Aktuelle Abgrenzung

**Umgesetzt und verifiziert:**
- Maven-Verifikation in GitHub Actions.
- Docker-Image-Build in GitHub Actions.
- GHCR-Image für Routalyx.
- Lokales Kubernetes-Deployment in kind.
- Kubernetes Namespace, ConfigMap, Secret, Deployment und Service.
- Startup-, Liveness- und Readiness-Probes.
- PostgreSQL weiterhin in Docker Compose.

**Noch nicht umgesetzt:**
- Automatisiertes Kubernetes-Deployment aus GitHub Actions.
- Deployment in eine externe oder produktive Kubernetes-Umgebung.
- Automatisierter Rollback.
- Produktive Observability- und Alerting-Konfiguration.

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

## 13. OpenAPI-Dokumentation – 30. Oktober 2026

### Ziel

Die REST API soll über eine maschinenlesbare OpenAPI-Spezifikation
und eine interaktive Swagger UI nachvollziehbar dokumentiert werden.

### Umsetzung

- Springdoc-Abhängigkeit `springdoc-openapi-starter-webmvc-ui`
  in Version `3.0.3` hinzugefügt.
- `OpenApiConfig` definiert Titel, Beschreibung und Version der API.
- `VehicleController` dokumentiert die fünf Fahrzeug-Endpunkte
  mit `@Operation`, `@Parameter`, `@Tag` und `@ApiResponses`.
- OpenAPI-Spezifikation über `/v3/api-docs` geprüft.
- Swagger UI über `/swagger-ui/index.html` aufgerufen.

### Verifikation

- Docker-Image erfolgreich neu gebaut.
- Spring Boot 4.0.8 mit Java 21 gestartet.
- PostgreSQL 17.11 verbunden; Flyway meldete Schema-Version 1 als aktuell.
- `./scripts/test.sh`: 23 Tests, 0 Fehler, 0 Fehlschläge,
  0 übersprungene Tests; Maven meldete `BUILD SUCCESS`.
- Die fünf Vehicle-Endpunkte wurden in der generierten
  OpenAPI-Spezifikation mit ihren dokumentierten Antworten geprüft.
- PR #9 wurde in `main` integriert.
- Merge-Commit: `3fadfcb`.
- Feature-Commit: `313f5a4`.

### Lernpunkte

- OpenAPI beschreibt den Vertrag einer REST API maschinenlesbar.
- Springdoc erzeugt die Spezifikation aus Spring-Controllern
  und deren Annotationen.
- Swagger UI stellt die Spezifikation interaktiv dar.
- Ein erfolgreicher Java-Testlauf allein bestätigt nicht,
  dass die tatsächlich laufende Docker-Anwendung den neuesten
  Quellcode verwendet. Deshalb wurden Image-Neubau und Laufzeit
  separat geprüft.

### Abgrenzung

Die OpenAPI-Dokumentation wurde lokal und über den gemergten
Git-Stand verifiziert. Ein produktives Deployment wurde dadurch
nicht nachgewiesen.

## 14. Standardisierte API-Fehlerantworten mit RFC 9457

### Ziel

Die Vehicle API soll Fehler in einem einheitlichen, maschinenlesbaren Format zurückgeben.

### Technischer Hintergrund

RFC 9457 definiert ein standardisiertes Format für HTTP-Fehlerantworten mit dem Medientyp `application/problem+json`.

Spring stellt dafür `ProblemDetail` bereit.

### Umsetzung

Die zentrale Fehlerbehandlung befindet sich in `ApiExceptionHandler.java`.

- `@RestControllerAdvice` bündelt die Fehlerbehandlung.
- `ProblemDetail` bildet die standardisierte Fehlerantwort.
- `detail` beschreibt den Fehler.
- `instance` enthält den Request-Pfad.
- `errors` ergänzt feldbezogene Validierungsinformationen.

Behandelte Fehlerfälle:

- `MethodArgumentNotValidException`: HTTP 400.
- `DataIntegrityViolationException`: HTTP 409.
- `ResponseStatusException`: Übernahme des Status und der Beschreibung.
- `MethodArgumentTypeMismatchException`: HTTP 400.

### Beispiel

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/api/v1/vehicles",
  "errors": {
    "licensePlate": "must not be blank",
    "status": "must not be null"
  }
}
```

### Tests und Verifikation

Nach der Bereinigung doppelter Assertions wurde `./scripts/test.sh` vollständig ausgeführt:

- 24 Tests
- 0 Fehlschläge
- 0 Fehler
- 0 übersprungene Tests
- Maven-Ergebnis: `BUILD SUCCESS`

### Git-Stand

- `ffabc0b`: `feat(api): standardize error responses`
- `64069c0`: `test(api): remove duplicate error assertions`

Die Implementierung ist committed. Der Feature-Branch wurde noch nicht gepusht. Ein Pull Request wurde noch nicht erstellt.

### Erkenntnisse

Eine zentrale Fehlerbehandlung verhindert, dass unterschiedliche Controller ihre Fehlerantworten jeweils selbst formatieren müssen.

Das standardisierte Format erleichtert die Verarbeitung durch Frontend-Anwendungen und andere API-Clients.

## 15. Entkopplung der Application-Schicht von API-Modellen

### Ziel

Die Application-Schicht soll nicht von HTTP-Request- oder Response-Modellen
der API-Schicht abhängen. Dadurch werden Verantwortlichkeiten klarer
getrennt und die Geschäftslogik unabhängiger von der REST-Schnittstelle.

### Umsetzung

- `VehicleCommand` repräsentiert Eingaben für die Application-Schicht.
- `VehicleResult` repräsentiert das Ergebnis eines einzelnen Fahrzeugvorgangs.
- `VehiclePageResult` repräsentiert paginierte Ergebnisse.
- `VehicleService` verwendet Application-Modelle statt API-DTOs.
- `VehicleController` übernimmt die Übersetzung zwischen API-Modellen
  und Application-Modellen.
- Application-Exceptions ersetzen HTTP-spezifische Fehlerbehandlung
  innerhalb des Services:
  - `VehicleNotFoundException`
  - `DuplicateLicensePlateException`
  - `InvalidVehicleQueryException`
- `ApiExceptionHandler` übersetzt Application-Exceptions in HTTP-Fehler.

### Architekturgrenze

Die API-Schicht ist von der Application-Schicht entkoppelt.

Die Application-Schicht verwendet weiterhin Spring Data JPA und
Persistence-Typen. Eine vollständige Entkopplung von der Datenbanktechnik
ist damit noch nicht erreicht.

### Verifikation

- PR #12 wurde in `main` integriert.
- Merge-Commit: `9438482`.
- Feature-Commit: `6802fce`.
- `./scripts/test.sh`: 24 Tests, 0 Fehler, 0 Fehlschläge.
- Maven meldete `BUILD SUCCESS`.
- `main` wurde mit `origin/main` synchronisiert.
- Arbeitsverzeichnis war sauber.

### Lernpunkte

- API-DTOs gehören zur Schnittstellenschicht.
- Application-Modelle repräsentieren Anwendungsfälle unabhängig vom HTTP-Vertrag.
- Der Controller ist für die Übersetzung zwischen API und Application zuständig.
- Eine Schichtentkopplung ist schrittweise möglich.
- Dependency Inversion gegenüber Persistence ist eine mögliche nächste
  Architekturverbesserung.

### Abgrenzung

Der Merge und die Tests wurden bestätigt.
Ein produktives Deployment wurde nicht nachgewiesen.

## 16. Repository-Port und Adapter – 2. November 2026

### Ausgangssituation

Vor diesem Refactoring verwendete `VehicleService` direkt `VehicleRepository`. Dadurch war die Application-Schicht an Spring Data JPA gekoppelt.

Das erschwerte es, die Geschäftslogik unabhängig von der konkreten Persistenztechnologie zu testen oder später eine andere Implementierung einzusetzen.

### Ziel

Die Application-Schicht soll nur noch wissen, welche Speicheroperationen sie benötigt. Wie diese Operationen technisch umgesetzt werden, entscheidet die Persistence-Schicht.

### Architektur

```text
VehicleService
      |
      v
VehicleStore (Application-Port)
      ^
      |
VehicleRepositoryAdapter (Persistence)
      |
      v
VehicleRepository / Spring Data JPA
```

### 1. Application-Port: `VehicleStore`

`VehicleStore` ist ein Interface in der Application-Schicht.

Es beschreibt die benötigten Operationen:

- Fahrzeug anhand der ID suchen
- Fahrzeug anhand des Kennzeichens suchen
- Kennzeichen auf Existenz prüfen
- Fahrzeug erstellen
- Fahrzeug aktualisieren
- Fahrzeug löschen
- Fahrzeuge paginiert abrufen

Das Interface enthält keine Spring-Data- oder JPA-spezifischen Typen.

### 2. Persistence-Adapter: `VehicleRepositoryAdapter`

Der Adapter implementiert `VehicleStore`.

Seine Aufgaben:

- Spring Data Repository verwenden
- JPA-Entities erstellen und aktualisieren
- Entities in Application-Ergebnisse umwandeln
- Spring-Data-Pagination in `VehiclePageResult` übertragen

Damit bleibt die technische Persistenzlogik außerhalb der Application-Schicht.

### 3. Dependency Inversion

Die Application-Schicht hängt von einer Abstraktion ab:

```text
Application -> VehicleStore
Persistence -> VehicleStore
```

Der Adapter implementiert das Interface. Spring übernimmt zur Laufzeit die Dependency Injection.

Das ist ein Beispiel für Dependency Inversion und einen Port-and-Adapter-Ansatz.

### 4. Umgang mit Optional

Die Update-Operation gibt ein `Optional<VehicleResult>` zurück.

Damit kann der Adapter ausdrücken, dass das Fahrzeug während des Updates möglicherweise nicht mehr existiert. Die Application-Schicht entscheidet, daraus eine `VehicleNotFoundException` zu machen.

### 5. Tests

Für `VehicleService` wurden Unit-Tests mit JUnit und Mockito ergänzt.

Der Service kann damit unabhängig von PostgreSQL und Spring Data getestet werden. Ein Mock von `VehicleStore` simuliert die benötigten Speicheroperationen.

**Verifikation:** Der vollständige Testlauf ergab 34 bestandene Tests und `BUILD SUCCESS`.

### Gelernt

- Unterschied zwischen Repository-Port und Persistence-Adapter
- Dependency Inversion
- Entkopplung der Application-Schicht von Spring Data JPA
- Verwendung von Mockito für isolierte Unit-Tests
- Trennung von Application-Modellen und JPA-Entities
- Spring Dependency Injection über Interface-Implementierungen

### Status

Lokal umgesetzt und getestet. Noch nicht committed, gepusht oder gemergt. Kein Deployment erfolgt.

### Nächster Schritt

Staged Diff prüfen und anschließend einen nachvollziehbaren Commit vorbereiten.


## 17. Kubernetes-Deployment mit kind – 9. November 2026

### Ziel

Routalyx erstmals in Kubernetes betreiben und die bisherige Container- und CI-Erfahrung auf ein lokales Kubernetes-Deployment übertragen.

### Ausgangslage

- Fedora als Entwicklungsumgebung.
- kind-Cluster `dev`.
- Docker Compose für PostgreSQL.
- Spring Boot 4.0.8 mit Java 21.
- GHCR als Container-Registry.
- GitHub Actions für Tests und Docker-Image-Build.

### Architektur

```text
Fedora Host
│
├── Docker Compose
│   └── PostgreSQL 17
│
└── kind Kubernetes Cluster
    └── Namespace: routalyx
        ├── ConfigMap: routalyx-config
        ├── Secret: routalyx-db
        ├── Deployment: routalyx
        │   └── Routalyx Pod
        └── Service: routalyx (ClusterIP)
```

Die Anwendung läuft in Kubernetes, die PostgreSQL-Datenbank bleibt auf dem Fedora-Host in Docker Compose.

### Kubernetes-Ressourcen

#### Namespace

Der Namespace `routalyx` gruppiert die Kubernetes-Ressourcen der Anwendung.

Namespaces helfen dabei, Ressourcen logisch zu trennen und zu verwalten.

#### ConfigMap

Die ConfigMap `routalyx-config` enthält nicht-sensitive Konfiguration:

- JDBC-Datenbank-URL
- Datenbankbenutzername

Konfiguration wird damit von der Deployment-Definition getrennt.

#### Secret

Das Kubernetes Secret `routalyx-db` enthält Datenbankzugangsdaten.

Das Secret wurde außerhalb der versionierten YAML-Dateien erzeugt. Zugangsdaten werden nicht in Git eingecheckt.

**Sicherheitshinweis:** Kubernetes Secrets sind nicht automatisch verschlüsselt, nur weil sie vom Typ Secret sind. Für produktive Umgebungen sind zusätzliche Zugriffskontrollen und eine geeignete Secret-Verwaltung notwendig.

#### Deployment

Das Deployment `routalyx` verwaltet einen Application-Pod.

Es definiert unter anderem:

- Container-Image
- Replica-Anzahl
- Umgebungsvariablen
- Container-Port
- CPU- und Memory-Requests
- CPU- und Memory-Limits
- Health-Probes

Kubernetes sorgt dafür, dass der gewünschte Zustand – eine laufende Replica – eingehalten wird.

#### Service

Der Service `routalyx` verwendet den Typ `ClusterIP`.

Er stellt einen stabilen internen Netzwerkzugang zu den Pods bereit. Der Service verwendet den Label-Selector `app: routalyx`.

Die Verbindung zum Service wurde lokal über Port-Forwarding hergestellt:

```bash
kubectl port-forward -n routalyx svc/routalyx 8080:8080
```

Port-Forwarding ist eine temporäre Verbindung für Entwicklung und Diagnose und kein produktiver Ingress.

### Health-Probes

#### Startup-Probe

Spring Boot benötigt Zeit für den Start und die Initialisierung.

Die Startup-Probe gibt der Anwendung Zeit, hochzufahren, bevor Kubernetes die regulären Liveness-Prüfungen verwendet.

#### Liveness-Probe

Die Liveness-Probe prüft, ob die Anwendung grundsätzlich läuft.

Sie verwendet:

`/actuator/health/liveness`

Eine fehlgeschlagene Liveness-Prüfung kann dazu führen, dass Kubernetes den Container neu startet.

#### Readiness-Probe

Die Readiness-Probe prüft, ob die Anwendung bereit ist, Requests anzunehmen.

Sie verwendet:

`/actuator/health/readiness`

Die Readiness-Gruppe berücksichtigt zusätzlich die Datenbankverbindung. Ein nicht bereiter Pod soll keine regulären Requests erhalten.

### Netzwerk: Kubernetes zu PostgreSQL

Die PostgreSQL-Datenbank läuft nicht innerhalb des kind-Clusters.

Die Spring-Boot-Anwendung verwendet:

`jdbc:postgresql://172.18.0.1:5432/routalyx`

Damit verbindet sich der Pod über das kind-/Docker-Netzwerk mit dem PostgreSQL-Dienst auf dem Fedora-Host.

Diese Konfiguration ist für die aktuelle lokale Lernumgebung geeignet, aber nicht als allgemeine Produktionsarchitektur gedacht.

### Ressourcen

Das Deployment verwendet Requests und Limits:

| Ressource | Request | Limit |
|---|---:|---:|
| CPU | 100m | 500m |
| Memory | 256Mi | 512Mi |

Requests dienen Kubernetes zur Planung der Pod-Platzierung.

Limits begrenzen den Ressourcenverbrauch des Containers.

### Verifikation

- Deployment erfolgreich in kind angewendet.
- Pod Ready (1/1).
- Service und EndpointSlice überprüft.
- Application-Logs kontrolliert.
- PostgreSQL-Verbindung hergestellt.
- Flyway validierte die Migration.
- Schema-Version 1 war aktuell.
- Actuator Health über Port-Forwarding erfolgreich geprüft.
- Swagger UI über Port-Forwarding erreichbar.
- `./scripts/test.sh`: 34 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungene Tests.
- Maven meldete `BUILD SUCCESS`.
- GitHub Actions Test-Job erfolgreich.
- GitHub Actions Docker-Image-Build erfolgreich.

Diese Ergebnisse beziehen sich auf die tatsächlich bestätigten Test- und Deployment-Läufe.

### Git-Workflow

- Feature-Branch: `feat/deploy-routalyx-kind`
- Feature-Commit: `7c6a8a1`
- PR #15 erfolgreich gemergt.
- Merge-Commit: `42d8a21`
- Lokaler `main` mit `origin/main` synchronisiert.
- Working Tree nach dem Merge sauber bestätigt.

### Lernpunkte

- Kubernetes Deployment verwaltet Pods über ReplicaSets.
- Pods sind die kleinste deploybare Einheit in Kubernetes.
- Services stellen stabile Netzwerkzugänge bereit.
- ConfigMaps und Secrets trennen Konfiguration von Deployment-Code.
- Health-Probes unterscheiden Start, Leben und Bereitschaft.
- Requests und Limits beeinflussen Scheduling und Ressourcenverbrauch.
- kind ermöglicht Kubernetes-Lernen lokal.
- Kubernetes und Docker Compose können in einer Lernumgebung gemeinsam verwendet werden.

### Status und Abgrenzung

Das lokale kind-Deployment ist umgesetzt und verifiziert.

Es ist kein produktives Deployment.

PostgreSQL verbleibt in Docker Compose.

Ein automatisiertes Kubernetes-Deployment aus GitHub Actions und ein Rollback-Prozess sind noch nicht umgesetzt.

### Nächster Schritt

Kubernetes-Rollouts und Rollbacks zunächst manuell lernen. Anschließend kann ein kontrollierter Deployment-Job in GitHub Actions ergänzt werden.
