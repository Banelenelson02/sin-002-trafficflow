# TrafficFlow demo checklist (Windows 11 / PowerShell)

This is a repeatable local presentation checklist, not evidence that every service has passed an end-to-end test.

## Prerequisites

- Java 17 or later and Maven installed: `java -version`, `mvn -version`
- Git installed: `git --version`
- Docker Desktop installed **and running**: `docker compose version`
- IntelliJ IDEA can open the cloned repository. This repo has **five independent Maven projects**, not one parent Maven build.

## Build all five modules

Run these commands from the repository root in IntelliJ's PowerShell terminal:

```powershell
mvn -f ingestion-service/pom.xml clean package
mvn -f intersection-service/pom.xml clean package
mvn -f congestion-service/pom.xml clean package
mvn -f routing-service/pom.xml clean package
mvn -f intersection-watchdog/pom.xml clean package
```

Stop if a build fails; record its first meaningful error. Do not claim a successful build before running it.

## Start services

First start ActiveMQ from the repository root:

```powershell
docker compose -f common/docker-compose.yml up -d
docker compose -f common/docker-compose.yml ps
```

Open **five separate PowerShell terminal tabs** in the repository root, one command per terminal, in this order:

```powershell
java -jar ingestion-service/target/ingestion-service.jar
```
```powershell
java -jar intersection-service/target/intersection-service.jar
```
```powershell
java -jar congestion-service/target/congestion-service.jar
```
```powershell
java -jar routing-service/target/routing-service.jar
```
```powershell
java -jar intersection-watchdog/target/intersection-watchdog.jar
```

The Intersection Service loads ingestion data at startup; therefore start ingestion first. Keep the terminals open.

## Live demonstration

In a sixth PowerShell terminal, check endpoints:

```powershell
curl.exe http://localhost:7020/intersections
curl.exe http://localhost:7021/intersections/INT-1001
curl.exe http://localhost:7022/congestion
curl.exe "http://localhost:7023/route?from=INT-1001&to=INT-1005"
curl.exe http://localhost:7024/status
```

Confirm the IDs actually exist and are active; otherwise choose IDs returned by ingestion. Then demonstrate a congestion update:

```powershell
curl.exe -i -X PUT "http://localhost:7022/congestion" -H "Content-Type: application/json" -d '{\"level\":4}'
curl.exe "http://localhost:7023/route?from=INT-1001&to=INT-1005"
```

With level 4 and working event delivery, the simplified estimate is 30 minutes (`10 + 4 * 5`). An immediate request can race with asynchronous topic delivery; retry after the Routing Service receives the update.

Now try invalid input, which should return HTTP 400:

```powershell
curl.exe -i -X PUT "http://localhost:7022/congestion" -H "Content-Type: application/json" -d '{\"level\":\"heavy\"}'
```

Finally, stop the Intersection Service, wait **more than 15 seconds**, and call the watchdog status again. It should report `DOWN` (HTTP 503). Restart Intersection Service and verify heartbeats return. The DLQ scenario is covered by an opt-in real-broker test, not by this simple stop/start demo.

## What to say when presenting

- **Stage 1:** CSV data is cleaned and exposed through ingestion REST API.
- **Stage 2:** Intersection and Routing services communicate with HTTP requests.
- **Stage 3:** Congestion Service publishes congestion changes to an ActiveMQ topic; Routing Service subscribes asynchronously.
- **Stage 4:** Intersection Service sends heartbeat queue messages; Watchdog detects missed and malformed heartbeats, with a dead-letter queue handling repeated failures.
- **Scope:** Travel times are a simple formula for demonstration, not a real street-route calculation or prediction.

## Remaining acceptance checks

- Run all five module Maven tests locally and save the results.
- Run the broker-backed Watchdog integration test: `mvn -f intersection-watchdog/pom.xml test -Dtrafficflow.integrationTests=true`.
- Verify topic updates, heartbeat timeout/recovery, and malformed-message DLQ behaviour on the actual machine.
- Check any additional grading instructions outside the repository README against the implemented behaviour.

For project details and contracts, see the root [README](../README.md).
