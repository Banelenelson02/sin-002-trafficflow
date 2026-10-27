
# Intersection Service

## Overview

The Intersection Service retrieves cleaned intersection records from the Ingestion Service and makes individual intersections available through a REST API. It caches the records in memory, using normalized intersection IDs as keys.

The service also publishes periodic heartbeat messages to ActiveMQ so the Intersection Watchdog can monitor whether the service is alive.

Part of the [TrafficFlow](../README.md) project. This is an independent Maven module with no parent POM.

## Responsibilities

- Fetch cleaned intersection records from the Ingestion Service at startup.
- Cache intersection records in memory, indexed by uppercase intersection ID.
- Provide an endpoint to check service health.
- Retrieve an individual intersection by its ID.
- Publish heartbeat messages to ActiveMQ every five seconds.
- Support monitoring by the Intersection Watchdog through the heartbeat queue.

## API endpoints

The service listens on port `7021`.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Checks whether the service is running. Returns `OK`. |
| `GET` | `/intersections/{id}` | Retrieves a cached intersection by its ID. Returns `404` if the intersection does not exist. |

### Example requests

Check service health:

```bash
curl -i http://localhost:7021/health
```

Retrieve an intersection:

```bash
curl -i http://localhost:7021/intersections/INT-1001
```

Replace `INT-1001` with an ID that exists in your cleaned intersection data.

## ActiveMQ integration

The Intersection Service publishes heartbeat messages to the ActiveMQ queue `intersection-heartbeat-queue`.

- **Broker URL:** `tcp://localhost:61616`
- **Queue:** `intersection-heartbeat-queue`
- **Publishing interval:** approximately every five seconds
- **Consumer:** Intersection Watchdog

The broker configuration is defined in the service's `mq/MqConfig.java` class. The ActiveMQ broker is configured through the Docker Compose setup in [`../common/`](../common).

The Watchdog uses these heartbeat messages to determine whether the Intersection Service is responding. Each heartbeat is marked with `trafficflowMessageType=heartbeat`, allowing the Watchdog to identify only TrafficFlow heartbeat messages in ActiveMQ's `ActiveMQ.DLQ` queue.

## Dependencies

At startup, the service requests intersection data from the Ingestion Service:

```text
GET http://localhost:7020/intersections
```

This means the Ingestion Service should be running and accessible when the Intersection Service starts.

The records are cached in memory. Changes to the source CSV are not automatically reflected in this cache while the service is running.

## Project structure

```text
intersection-service/
├── pom.xml
└── src/
    └── main/
        ├── java/co/wethinkcode/trafficflow/
        │   ├── IntersectionServiceApp.java
        │   ├── Intersection.java
        │   └── mq/
        │       └── MqConfig.java
        └── resources/
```

## Build

From the repository root:

```bash
mvn -f intersection-service/pom.xml clean package
```

Alternatively, from inside the `intersection-service` directory:

```bash
mvn clean package
```

## Run

From inside the `intersection-service` directory:

```bash
mvn clean package
java -jar target/intersection-service.jar
```

From the repository root:

```bash
java -jar intersection-service/target/intersection-service.jar
```

The service listens on port `7021`.

Ensure the ActiveMQ broker and Ingestion Service are running before starting it.

## Testing

No automated tests are currently documented for this module. You can manually check the service using:

```bash
curl -i http://localhost:7021/health
curl -i http://localhost:7021/intersections/INT-1001
```

The health endpoint should return HTTP `200` with `OK`. The individual intersection endpoint should return the matching intersection as JSON if the ID exists.

To test heartbeat monitoring, start the ActiveMQ broker and Intersection Watchdog, then check the Watchdog's status endpoint:

```bash
curl -i http://localhost:7024/status
```

## Current limitations

- Intersection data is loaded at startup and cached in memory.
- The service depends on the Ingestion Service being available when it loads its data.
- The heartbeat is a simple liveness signal, not a check that every other service is healthy.
- No automated tests are currently documented.
