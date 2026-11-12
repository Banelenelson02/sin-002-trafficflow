# TrafficFlow live demo guide

Use this guide for a practice presentation or screen recording. It complements the main [README](README.md).

## Demo sequence

Show five independent Java services, REST calls, an ActiveMQ congestion event, the Watchdog detecting an outage and recovery, and the integration tests. Aim for about 5–7 minutes. Work in **PowerShell from the repository root**, the folder containing `common`, `ingestion-service` and the other service folders.

**Important:** TrafficFlow estimates travel time using `10 + congestion level × 5` minutes. It does not calculate a physical road path. The demo shows the implementation and tests; check the official assignment rubric separately.

### 0. Get ready

- Open IntelliJ and Docker Desktop. Keep this guide accessible on your phone or a second screen.
- Use **five terminal tabs** to keep Java services running and a **sixth tab** to send test requests. If services are already running, keep them running rather than starting duplicates on occupied ports.
- Copy **only the commands** below, not PowerShell prompt text.
- If executable JARs do not exist, build each service as described in [README: Build](README.md#build).
- Do a trial recording first. You can narrate live or record video first and add your voice later.

### 1. ActiveMQ broker

In a PowerShell tab at the repository root:

```powershell
docker compose -f common/docker-compose.yml up -d
docker compose -f common/docker-compose.yml ps
```

**Show:** The ActiveMQ container is up. The messaging port is `61616` and the optional broker web console uses `http://localhost:8161`.

**Say:** “ActiveMQ is our message broker. It carries congestion updates and Intersection Service heartbeats.”

### 2. Start the five services

Run one command in each **separate** terminal tab, from the repository root, in this order. Leave them running.

**Terminal 1 — Ingestion, port 7020**
```powershell
java -jar ingestion-service/target/ingestion-service.jar
```

**Terminal 2 — Intersection, port 7021**
```powershell
java -jar intersection-service/target/intersection-service.jar
```

**Terminal 3 — Congestion, port 7022**
```powershell
java -jar congestion-service/target/congestion-service.jar
```

**Terminal 4 — Routing, port 7023**
```powershell
java -jar routing-service/target/routing-service.jar
```

**Terminal 5 — Watchdog, port 7024**
```powershell
java -jar intersection-watchdog/target/intersection-watchdog.jar
```

**Show:** Javalin startup logs for ports 7020–7024, the Routing Service subscribing to `congestion-topic`, and Watchdog receiving heartbeats.

**Say:** “Each Java service has one responsibility. They use REST for direct requests and ActiveMQ for asynchronous messages.”

### 3. REST endpoints

In **Terminal 6**, run these one at a time:

```powershell
curl.exe http://localhost:7020/intersections
curl.exe http://localhost:7021/intersections/INT-1001
curl.exe http://localhost:7022/congestion
curl.exe "http://localhost:7023/route?from=INT-1001&to=INT-1005"
curl.exe -i http://localhost:7024/status
```

**Show:** Ingestion returns the cleaned dataset (17 records in the previous successful test), Intersection returns one record, Congestion returns a level, Routing returns a travel estimate, and Watchdog returns `UP` with HTTP 200 while heartbeats arrive.

**Say:** “These endpoints let us retrieve cleaned intersections, look up an individual intersection, get congestion, estimate travel time, and monitor heartbeat status.”

### 4. Congestion and ActiveMQ

First set congestion to zero and show a 10-minute route estimate:

```powershell
Invoke-RestMethod -Method Put -Uri "http://localhost:7022/congestion" -ContentType "application/json" -Body (@{ level = 0 } | ConvertTo-Json)
curl.exe "http://localhost:7023/route?from=INT-1001&to=INT-1005"
```

Then update congestion to level four and check again:

```powershell
Invoke-RestMethod -Method Put -Uri "http://localhost:7022/congestion" -ContentType "application/json" -Body (@{ level = 4 } | ConvertTo-Json)
Start-Sleep -Seconds 2
curl.exe "http://localhost:7023/route?from=INT-1001&to=INT-1005"
```

**Expected:** The estimate changes from **10 minutes (level 0)** to **30 minutes (level 4)**. If the subscriber has not processed the update yet, retry the route request.

**Say:** “Congestion publishes an event to ActiveMQ, Routing consumes that event, and the travel estimate changes from ten to thirty minutes.”

### 5. Simulate Intersection Service failure

- In **Terminal 2**, press **Ctrl+C** to stop **only** Intersection (port 7021).
- Leave Docker and the other four Java services running.
- In **Terminal 6**, run:

```powershell
Start-Sleep -Seconds 20
curl.exe -i http://localhost:7024/status
```

**Expected:** HTTP **503 Service Unavailable** and `"status":"DOWN"`.

**Say:** “The Intersection Service stopped sending heartbeats, so Watchdog detected the timeout and reported the service as down.”

### 6. Demonstrate recovery

Restart Intersection in Terminal 2:

```powershell
java -jar intersection-service/target/intersection-service.jar
```

Then check from Terminal 6:

```powershell
Start-Sleep -Seconds 5
curl.exe -i http://localhost:7024/status
```

**Expected:** HTTP **200 OK** and `"status":"UP"`. If still down, allow the next heartbeat and retry.

**Say:** “After restarting Intersection, new heartbeat messages arrive and Watchdog returns to UP.”

### 7. Broker-backed Watchdog tests

Keep ActiveMQ running. In Terminal 6:

```powershell
mvn -f intersection-watchdog/pom.xml test '-Dtrafficflow.integrationTests=true'
```

**Previously observed on 12 November 2026:** `BUILD SUCCESS`, **10 tests**, **0 failures**, **0 errors**, including a dead-letter queue test that retries an invalid heartbeat. Show the **actual** result of your new test run.

**Say:** “The tests cover heartbeat handling, status reporting, message configuration, and retry and dead-letter queue behaviour.”

If this test fails, capture the actual error and investigate before presenting it as passing. A broker-backed test can be affected by existing queue or DLQ messages.

### 8. Finish

**Say:** “TrafficFlow demonstrates direct REST communication, asynchronous messaging through ActiveMQ, and service monitoring with failure detection and recovery. Thank you.”

## Trial recording review

On **Windows 11**, Snipping Tool may have a **Record** feature; select the recording region and start a screen capture. On **Windows 10** or if Record is unavailable, use another screen recorder such as OBS Studio. You can record without speaking and add a voiceover afterward.

Before recording, enlarge the terminal font so the commands and JSON are readable. Avoid displaying credentials or unrelated personal information. Save the trial video, then review it for:

- [ ] ActiveMQ container shown running
- [ ] All five services shown on their ports
- [ ] REST responses visible
- [ ] Congestion level 0 → 4, travel estimate 10 → 30 minutes
- [ ] Watchdog UP → DOWN (503) → UP (200)
- [ ] Integration test summary shown accurately
- [ ] Clear narration and readable recording
- [ ] Official rubric and final submission requirements checked
