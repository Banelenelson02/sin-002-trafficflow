# TrafficFlow live demo guide: one command at a time

This guide is for a **live presentation or practice recording on Windows 11**, using **PowerShell in IntelliJ IDEA**. It complements the main [README](README.md).

**Usage:** Run one command at a time. Each Java service uses its own terminal. Do not combine commands.

## Before starting

- Open IntelliJ in the **repository root**, the `sin-002-trafficflow` folder containing `common` and the five service folders.
- Open Docker Desktop.
- Have **five separate PowerShell terminals for Java services** and a **sixth terminal for commands and API requests**.
- If a service is already running, **do not start a second copy** on the same port. You can still demonstrate the running service.
- If you have already built the five JAR files, you do **not** need to download dependencies or rebuild just for the demo.
- Copy only the command inside each code block, not the `PS C:\...>` prompt.
- The estimated travel time uses a **simple formula**, `10 + (congestion level × 5)` minutes. It is not real road-network routing.

## Part 2: Check ActiveMQ

### Step 1. Verify the broker is running

**Terminal:** 6

```powershell
docker compose -f common/docker-compose.yml ps
```

### Step 2. Start ActiveMQ only if it is not running

**Terminal:** 6

**Skip this step if Step 1 already showed the broker running.**

```powershell
docker compose -f common/docker-compose.yml up -d
```

## Part 3: Start each Java service

**Important:** Run each command in a **different terminal** and leave it running. Do not press Ctrl+C unless told to in the Watchdog demonstration.

### Step 3. Ingestion Service

**Terminal:** 1

```powershell
java -jar ingestion-service/target/ingestion-service.jar
```

### Step 4. Intersection Service

**Terminal:** 2

```powershell
java -jar intersection-service/target/intersection-service.jar
```

### Step 5. Congestion Service

**Terminal:** 3

```powershell
java -jar congestion-service/target/congestion-service.jar
```

### Step 6. Routing Service

**Terminal:** 4

```powershell
java -jar routing-service/target/routing-service.jar
```

### Step 7. Intersection Watchdog

**Terminal:** 5

```powershell
java -jar intersection-watchdog/target/intersection-watchdog.jar
```

## Part 4: REST API demonstration

**Use Terminal 6 for EVERY command in this part. Run and explain one at a time.**

### Step 8. Show all cleaned intersections

```powershell
curl.exe http://localhost:7020/intersections
```

### Step 9. Look up one intersection

```powershell
curl.exe http://localhost:7021/intersections/INT-1001
```

### Step 10. Check congestion

```powershell
curl.exe http://localhost:7022/congestion
```

### Step 11. Request a route estimate

```powershell
curl.exe "http://localhost:7023/route?from=INT-1001&to=INT-1005"
```

### Step 12. Check Watchdog status

```powershell
curl.exe -i http://localhost:7024/status
```

## Part 5: Demonstrate ActiveMQ congestion updates

**Use Terminal 6. Each step is a separate command.** This section deliberately starts with congestion level zero so the result is predictable.

### Step 13. Set congestion to zero

```powershell
Invoke-RestMethod -Method Put -Uri "http://localhost:7022/congestion" -ContentType "application/json" -Body (@{ level = 0 } | ConvertTo-Json)
```

### Step 14. Show the baseline route estimate

```powershell
curl.exe "http://localhost:7023/route?from=INT-1001&to=INT-1005"
```

### Step 15. Set congestion to four

```powershell
Invoke-RestMethod -Method Put -Uri "http://localhost:7022/congestion" -ContentType "application/json" -Body (@{ level = 4 } | ConvertTo-Json)
```

### Step 16. Allow the message to arrive

```powershell
Start-Sleep -Seconds 2
```

### Step 17. Show the updated route estimate

```powershell
curl.exe "http://localhost:7023/route?from=INT-1001&to=INT-1005"
```

## Part 6: Demonstrate Watchdog failure detection

### Step 18. Stop only Intersection Service

**Terminal:** 2

**Keyboard action:** Press **Ctrl+C** in the terminal running Intersection.

### Step 19. Wait for Watchdog timeout

**Terminal:** 6

```powershell
Start-Sleep -Seconds 20
```

### Step 20. Check status during the outage

**Terminal:** 6

```powershell
curl.exe -i http://localhost:7024/status
```

## Part 7: Demonstrate recovery

### Step 21. Restart Intersection Service

**Terminal:** 2, from the repository root

```powershell
java -jar intersection-service/target/intersection-service.jar
```

### Step 22. Wait for the next heartbeat

**Terminal:** 6

```powershell
Start-Sleep -Seconds 5
```

### Step 23. Confirm recovery

**Terminal:** 6

```powershell
curl.exe -i http://localhost:7024/status
```

## Part 8: Automated Watchdog integration test

### Step 24. Run the integration tests

**Terminal:** 6. Keep Docker and ActiveMQ running.

```powershell
mvn -f intersection-watchdog/pom.xml test '-Dtrafficflow.integrationTests=true'
```

## Trial screen recording

Use Windows 11 Snipping Tool or another screen recorder to capture the demonstration.

Check the final recording for:

- [ ] All five service ports visible
- [ ] REST outputs shown clearly
- [ ] Congestion level 0 produces 10 minutes
- [ ] Congestion level 4 produces 30 minutes
- [ ] Watchdog goes UP → DOWN (HTTP 503) → UP (HTTP 200)
- [ ] Integration test result is readable
- [ ] No passwords, private information or irrelevant windows are visible

