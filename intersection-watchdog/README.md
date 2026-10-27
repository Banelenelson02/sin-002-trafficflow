
# Intersection Watchdog

## Overview

The Intersection Watchdog monitors heartbeat messages from the Intersection Service. If the service stops sending heartbeats, the Watchdog reports that the service is down.

It provides a health endpoint and a status endpoint so that the service's monitoring state can be checked through HTTP.

Part of the [TrafficFlow](../README.md) project. This is an independent Maven module with no parent POM.

## Responsibilities

- Subscribe to the ActiveMQ queue `intersection-heartbeat-queue`.
- Track the most recently received heartbeat from the Intersection Service.
- Detect heartbeats that ActiveMQ has dead-lettered after a processing failure.
- Report the service as down if no heartbeat has been received or the latest heartbeat is too old.
- Expose HTTP endpoints for health and monitoring status.

## API endpoints

The service listens on port `7024`.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Checks whether the Watchdog service is running. Returns `OK`. |
| `GET` | `/status` | Reports whether recent Intersection Service heartbeats are being received. |

### Check the Watchdog health

```bash
curl -i http://localhost:7024/health
```

The endpoint should return HTTP `200` with `OK` when the Watchdog is running.

### Check the Intersection Service status

```bash
curl -i http://localhost:7024/status
```

When recent heartbeats are being received, the endpoint returns HTTP `200` with a JSON response similar to:

```json
{
  "status": "UP",
  "message": "Intersection Service heartbeat is being received"
}
```

If no heartbeat has been received, or the most recent heartbeat is older than the allowed threshold, the endpoint returns HTTP `503` with a response similar to:

```json
{
  "status": "DOWN",
  "message": "No recent heartbeat from Intersection Service"
}
```

## ActiveMQ integration

The Watchdog consumes heartbeat messages from the ActiveMQ queue `intersection-heartbeat-queue`.

- **Broker URL:** `tcp://localhost:61616`
- **Queue:** `intersection-heartbeat-queue`
- **Publisher:** Intersection Service
- **Consumer:** Intersection Watchdog
- **Heartbeat interval:** approximately five seconds
- **Timeout threshold:** more than 15 seconds without a heartbeat

The broker configuration is defined in the service's `mq/MqConfig.java` class. ActiveMQ is configured through the Docker Compose setup in [`../common/`](../common).

The Intersection Service publishes heartbeat messages periodically. The Watchdog uses the arrival of these messages to determine whether the service appears to be alive.

The heartbeat consumer uses a transacted JMS session. It commits only valid
`TextMessage` heartbeats and explicitly rolls back malformed marked heartbeats.
The ActiveMQ client redelivery policy permits one redelivery, after which ActiveMQ
sends a poison acknowledgement and the broker routes the persistent message to
its built-in `ActiveMQ.DLQ` queue. The Watchdog's DLQ consumer has the same
`trafficflowMessageType = 'heartbeat'` selector, so unrelated dead-lettered
messages do not affect its status.

Dead-letter alerts are recovery-aware rather than permanently latched: a valid
subsequent heartbeat clears the DLQ alarm and restores `UP`. The dead-letter
event remains in broker administration tools and the Watchdog logs.

## How monitoring works

1. The Intersection Service publishes a heartbeat message to the queue approximately every five seconds.
2. The Watchdog consumes the message and records the time of the latest heartbeat.
3. The `/status` endpoint checks whether a heartbeat has been received recently.
4. If no heartbeat has been received, or more than 15 seconds have elapsed since the latest heartbeat, the Watchdog reports `DOWN`.
5. If ActiveMQ dead-letters a marked heartbeat, it reports `DOWN` with HTTP `503`.
6. A later valid heartbeat clears that alert; otherwise, it reports `UP`.

The Watchdog's status is based on heartbeat reception. It does not independently prove that every function of the Intersection Service is working correctly.

## Project structure

```text
intersection-watchdog/
├── pom.xml
└── src/
    └── main/
        └── java/co/wethinkcode/trafficflow/
            ├── HeartbeatListener.java
            ├── IntersectionWatchdogApp.java
            ├── WatchdogStatus.java
            └── mq/
                └── MqConfig.java
```

## Build

From the repository root:

```bash
mvn -f intersection-watchdog/pom.xml clean package
```

Alternatively, from inside the `intersection-watchdog` directory:

```bash
mvn clean package
```

## Run

From inside the `intersection-watchdog` directory:

```bash
mvn clean package
java -jar target/intersection-watchdog.jar
```

From the repository root:

```bash
java -jar intersection-watchdog/target/intersection-watchdog.jar
```

The service listens on port `7024`.

Ensure ActiveMQ is running and the Intersection Service is publishing heartbeats when testing the monitoring behavior.

## Testing

Unit tests cover heartbeat validation, missing-heartbeat status, DLQ status, and
recovery after a valid heartbeat:

```bash
mvn -f intersection-watchdog/pom.xml test
```

The real-broker integration test is intentionally skipped unless explicitly
enabled. It sends a malformed marked heartbeat and verifies two delivery
attempts (initial delivery plus one retry) and DLQ detection. Start a clean
ActiveMQ broker first, then run:

```bash
mvn -f intersection-watchdog/pom.xml test -Dtrafficflow.integrationTests=true
```

Check the Watchdog's health and status:

```bash
curl -i http://localhost:7024/health
curl -i http://localhost:7024/status
```

For an integration test, start ActiveMQ and the Intersection Service, then check `/status`. It should report `UP` when recent heartbeats are being received.

To test the timeout behavior, stop the heartbeat publisher or stop the Intersection Service, wait longer than 15 seconds, and request `/status` again. The Watchdog should report `DOWN` with HTTP `503`.

Restart the Intersection Service and verify that the Watchdog begins receiving heartbeats again.

## Current limitations

- The Watchdog monitors heartbeat messages, not every aspect of the Intersection Service's functionality.
- The status endpoint reports monitoring state through HTTP; a separate notification or alert delivery mechanism is not documented.
- The DLQ integration test requires a locally running ActiveMQ broker and is not part of the default unit-test run.
