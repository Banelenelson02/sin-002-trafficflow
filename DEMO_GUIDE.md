# TrafficFlow live demo guide: one command at a time

This guide is for a **live presentation or practice recording on Windows 11**, using **PowerShell in IntelliJ IDEA**. It complements the main [README](README.md).

**How to use this guide:** Each numbered step has **ONE command only** (or one clearly marked keyboard action). Run it, explain what happened, check the output, and **only then** go to the next step. Do not copy several commands at once. You may keep this guide on your phone and explain naturally rather than read it word for word.

Aim for around 5–8 minutes, or longer if your assessment instructions allow it.

## Before starting

- Open IntelliJ in the **repository root**, the `sin-002-trafficflow` folder containing `common` and the five service folders.
- Open Docker Desktop.
- Have **five separate PowerShell terminals for Java services** and a **sixth terminal for commands and API requests**.
- If a service is already running, **do not start a second copy** on the same port. You can still demonstrate the running service.
- If you have already built the five JAR files, you do **not** need to download dependencies or rebuild just for the demo.
- Copy only the command inside each code block, not the `PS C:\...>` prompt.
- The estimated travel time uses a **simple formula**, `10 + (congestion level × 5)` minutes. It is not real road-network routing.

## Part 1: Introduce the project

**Say:** “Hello. Today I'll demonstrate TrafficFlow, a Java traffic management project made up of five independent services. They communicate using REST APIs and Apache ActiveMQ, and we will demonstrate congestion updates and service monitoring.”

**Show:** The project folders and architecture in the main README. There is no command for this introduction.

## Part 2: Check ActiveMQ

### Step 1. Verify the broker is running

**Terminal:** 6

```powershell
docker compose -f common/docker-compose.yml ps
```

**Show/expect:** An ActiveMQ container with a running/up status.

**Say:** “ActiveMQ is the message broker for asynchronous communication. It carries congestion events and heartbeat messages.”

### Step 2. Start ActiveMQ only if it is not running

**Terminal:** 6

**Skip this step if Step 1 already showed the broker running.**

```powershell
docker compose -f common/docker-compose.yml up -d
```

**Show/expect:** Docker reports the ActiveMQ container started.

**Say:** “Now the message broker is available on port 61616.”

## Part 3: Start each Java service

**Important:** Run each command in a **different terminal** and leave it running. Do not press Ctrl+C unless told to in the Watchdog demonstration.

### Step 3. Ingestion Service

**Terminal:** 1

```powershell
java -jar ingestion-service/target/ingestion-service.jar
```

**Show/expect:** Ingestion starts and listens on **port 7020**.

**Say:** “Ingestion reads the cleaned intersection dataset and exposes it through a REST API.”

### Step 4. Intersection Service

**Terminal:** 2

```powershell
java -jar intersection-service/target/intersection-service.jar
```

**Show/expect:** Intersection loads **17 intersection records** in the tested dataset, listens on **port 7021**, and starts sending heartbeats.

**Say:** “Intersection returns information about individual intersections and sends regular heartbeat messages.”

### Step 5. Congestion Service

**Terminal:** 3

```powershell
java -jar congestion-service/target/congestion-service.jar
```

**Show/expect:** Congestion starts on **port 7022**.

**Say:** “Congestion manages the current congestion level and publishes updates to ActiveMQ.”

### Step 6. Routing Service

**Terminal:** 4

```powershell
java -jar routing-service/target/routing-service.jar
```

**Show/expect:** Routing starts on **port 7023** and subscribes to the ActiveMQ `congestion-topic`.

**Say:** “Routing checks intersection information and produces a simplified estimated travel time using the latest congestion level.”

### Step 7. Intersection Watchdog

**Terminal:** 5

```powershell
java -jar intersection-watchdog/target/intersection-watchdog.jar
```

**Show/expect:** Watchdog starts on **port 7024**. Its terminal should begin showing received heartbeat messages.

**Say:** “The Watchdog listens for heartbeats and detects when the Intersection Service is unavailable.”

## Part 4: REST API demonstration

**Use Terminal 6 for EVERY command in this part. Run and explain one at a time.**

### Step 8. Show all cleaned intersections

```powershell
curl.exe http://localhost:7020/intersections
```

**Show/expect:** JSON containing **17 intersections** in the tested dataset.

**Say:** “This GET request returns the cleaned intersection records from the Ingestion Service.”

### Step 9. Look up one intersection

```powershell
curl.exe http://localhost:7021/intersections/INT-1001
```

**Show/expect:** A JSON record for **INT-1001**.

**Say:** “Here I request one intersection by its ID instead of the whole list.”

### Step 10. Check congestion

```powershell
curl.exe http://localhost:7022/congestion
```

**Show/expect:** JSON with a congestion level. The current value may differ from a previous trial.

**Say:** “This endpoint gives us the congestion level currently stored by the Congestion Service.”

### Step 11. Request a route estimate

```powershell
curl.exe "http://localhost:7023/route?from=INT-1001&to=INT-1005"
```

**Show/expect:** A JSON response with source, destination, current congestion and estimated time.

**Say:** “This endpoint gives an estimated travel time between two active intersections.”

### Step 12. Check Watchdog status

```powershell
curl.exe -i http://localhost:7024/status
```

**Show/expect:** **HTTP 200** and `"status":"UP"` if heartbeats are arriving.

**Say:** “The Watchdog is UP because it is receiving heartbeat messages.”

## Part 5: Demonstrate ActiveMQ congestion updates

**Use Terminal 6. Each step is a separate command.** This section deliberately starts with congestion level zero so the result is predictable.

### Step 13. Set congestion to zero

```powershell
Invoke-RestMethod -Method Put -Uri "http://localhost:7022/congestion" -ContentType "application/json" -Body (@{ level = 0 } | ConvertTo-Json)
```

**Show/expect:** Response indicating **level 0**.

**Say:** “First, I'll set congestion to zero to establish a baseline.”

### Step 14. Show the baseline route estimate

```powershell
curl.exe "http://localhost:7023/route?from=INT-1001&to=INT-1005"
```

**Show/expect:** `"congestionLevel":0` and `"estimatedTravelTimeMinutes":10`.

**Say:** “With no congestion, the estimated time is ten minutes.”

### Step 15. Set congestion to four

```powershell
Invoke-RestMethod -Method Put -Uri "http://localhost:7022/congestion" -ContentType "application/json" -Body (@{ level = 4 } | ConvertTo-Json)
```

**Show/expect:** Response indicating **level 4**.

**Say:** “Now I am updating the level to four. Congestion publishes this change to ActiveMQ, and Routing receives the event.”

### Step 16. Allow the message to arrive

```powershell
Start-Sleep -Seconds 2
```

**Show/expect:** PowerShell waits two seconds and returns to the prompt.

**Say:** “This short pause gives the asynchronous message time to be processed. Start-Sleep does not start any service.”

### Step 17. Show the updated route estimate

```powershell
curl.exe "http://localhost:7023/route?from=INT-1001&to=INT-1005"
```

**Show/expect:** `"congestionLevel":4` and `"estimatedTravelTimeMinutes":30`. If the update has not arrived, repeat **this one command** after a short wait.

**Say:** “The estimate changed from ten to thirty minutes after Routing consumed the congestion update.”

## Part 6: Demonstrate Watchdog failure detection

### Step 18. Stop only Intersection Service

**Terminal:** 2

**Keyboard action:** Press **Ctrl+C** in the terminal running Intersection.

**Show/expect:** The Intersection Service stops. Keep the other four services and ActiveMQ running.

**Say:** “I'll simulate an outage by stopping the Intersection Service.”

### Step 19. Wait for Watchdog timeout

**Terminal:** 6

```powershell
Start-Sleep -Seconds 20
```

**Show/expect:** PowerShell pauses for twenty seconds. The Watchdog considers heartbeat absence beyond fifteen seconds a failure.

**Say:** “The Watchdog allows time for heartbeats. When they stop arriving, it can mark the service as unavailable.”

### Step 20. Check status during the outage

**Terminal:** 6

```powershell
curl.exe -i http://localhost:7024/status
```

**Show/expect:** **HTTP 503 Service Unavailable** and `"status":"DOWN"`.

**Say:** “The Watchdog detected the missing heartbeats, so it now reports DOWN.”

## Part 7: Demonstrate recovery

### Step 21. Restart Intersection Service

**Terminal:** 2, from the repository root

```powershell
java -jar intersection-service/target/intersection-service.jar
```

**Show/expect:** Intersection starts on port **7021** and resumes heartbeat messages.

**Say:** “I'm restarting the Intersection Service so it can send heartbeats again.”

### Step 22. Wait for the next heartbeat

**Terminal:** 6

```powershell
Start-Sleep -Seconds 5
```

**Show/expect:** PowerShell waits five seconds.

**Say:** “This gives the restarted service time to send another heartbeat.”

### Step 23. Confirm recovery

**Terminal:** 6

```powershell
curl.exe -i http://localhost:7024/status
```

**Show/expect:** **HTTP 200 OK** and `"status":"UP"`. If the next heartbeat has not arrived yet, repeat this command after a moment.

**Say:** “After receiving a valid heartbeat, the Watchdog reports UP again.”

## Part 8: Automated Watchdog integration test

### Step 24. Run the integration tests

**Terminal:** 6. Keep Docker and ActiveMQ running.

```powershell
mvn -f intersection-watchdog/pom.xml test '-Dtrafficflow.integrationTests=true'
```

**Show/expect:** In the verified test on 12 November 2026: **BUILD SUCCESS**, **10 tests**, **0 failures**, **0 errors**. The `HeartbeatDlqIntegrationTest` also demonstrated retry and dead-letter queue handling for an invalid heartbeat. Show your **actual output**, not a previously assumed result.

**Say:** “Our automated tests verify heartbeat handling, Watchdog status and ActiveMQ dead-letter queue processing. The integration test retries an invalid heartbeat and checks that it reaches the dead-letter queue.”

**If the test fails:** Stop the recording or mention that you will investigate; do not claim a successful test. Other active consumers or prior queue messages may affect broker-backed tests.

## Part 9: Conclusion

**Say:** “TrafficFlow demonstrates five independent Java REST services, asynchronous congestion messaging using ActiveMQ, and heartbeat-based failure detection and recovery. The travel-time estimate is intentionally simplified. Thank you.”

No command required.

## Trial screen recording

Windows 11 Snipping Tool may offer a **Record** mode. Choose a readable capture region that shows the IntelliJ/PowerShell output. If your installation does not support video capture, use another recorder already available on your PC. You may **record silently and add a voiceover later**, or narrate live.

Check the final recording for:

- [ ] All five service ports visible
- [ ] REST outputs shown clearly
- [ ] Congestion level 0 produces 10 minutes
- [ ] Congestion level 4 produces 30 minutes
- [ ] Watchdog goes UP → DOWN (HTTP 503) → UP (HTTP 200)
- [ ] Integration test result is readable
- [ ] No passwords, private information or irrelevant windows are visible
- [ ] Recording matches the repository's official assessment instructions

**Presentation tip:** You do not need to show every terminal continuously. Keep each service alive in its own tab; switch to it when discussing that service, then return to Terminal 6. Every command in this guide is intentionally separate so you have time to explain it.
