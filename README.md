
# TrafficFlow

TrafficFlow is a Java-based urban traffic management project that models road intersections, congestion levels, and estimated travel times. It uses multiple REST services and ActiveMQ messaging to demonstrate communication between independent services.

## Project Status

| Stage | Description | Status |
|---|---|---|
| Stage 1 | Clean legacy intersection CSV data | Implemented |
| Stage 2 | Create REST services and synchronous communication | Implemented |
| Stage 3 | Use ActiveMQ to send congestion updates asynchronously | Implemented |
| Stage 4 | Monitor Intersection Service using heartbeats | Partially implemented |

Stage 4 currently detects missing heartbeats. Dead-letter queue monitoring has not yet been implemented.

## Architecture

```text
Ingestion Service (:7020)
        |
        | REST: GET /intersections
        v
Intersection Service (:7021)
        |
        | REST: GET /intersections/{id}
        v
Routing Service (:7023)
        ^
        | ActiveMQ topic: congestion-topic
        |
Congestion Service (:7022)


Intersection Service
        |
        | ActiveMQ queue:
        | intersection-heartbeat-queue
        v
Intersection Watchdog (:7024)
```

The Routing Service retrieves intersection details through REST and receives congestion updates through ActiveMQ. It calculates a simplified estimated travel time based on the current congestion level.

## Services and Ports

| Service | Directory | Port | Responsibility |
|---|---|---:|---|
| Ingestion Service | `ingestion-service/` | 7020 | Reads and exposes cleaned intersection data. |
| Intersection Service | `intersection-service/` | 7021 | Loads intersection data and provides individual intersection lookups. Publishes heartbeats. |
| Congestion Service | `congestion-service/` | 7022 | Stores congestion levels and publishes updates. |
| Routing Service | `routing-service/` | 7023 | Looks up intersections and estimates travel time. |
| Intersection Watchdog | `intersection-watchdog/` | 7024 | Monitors Intersection Service heartbeats. |
| ActiveMQ Broker | `common/` | 61616 | Enables asynchronous communication between services. |

The ActiveMQ web console is available on port `8161`.

Each service is an independent Maven project with its own `pom.xml`. There is no root parent or aggregator POM.

## Technology Stack

- Java 17+
- Maven 3.8+
- Javalin
- Jackson
- OpenCSV
- ActiveMQ Classic
- Docker Compose

## Integration Contracts

### REST Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `http://localhost:7020/health` | Checks Ingestion Service health. |
| GET | `http://localhost:7020/intersections` | Returns cleaned intersection records. |
| GET | `http://localhost:7021/health` | Checks Intersection Service health. |
| GET | `http://localhost:7021/intersections/{id}` | Retrieves an intersection by ID. |
| GET | `http://localhost:7022/health` | Checks Congestion Service health. |
| GET | `http://localhost:7022/congestion` | Returns the current congestion level. |
| PUT | `http://localhost:7022/congestion` | Updates the congestion level. |
| GET | `http://localhost:7023/health` | Checks Routing Service health. |
| GET | `http://localhost:7023/route?from=INT-1001&to=INT-1002` | Estimates travel time between two intersections. |
| GET | `http://localhost:7024/health` | Checks Watchdog health. |
| GET | `http://localhost:7024/status` | Reports whether recent heartbeats are being received. |

### ActiveMQ Topic: `congestion-topic`

- **Producer:** Congestion Service
- **Consumer:** Routing Service
- **Payload:** `{"level": 0}` through `{"level": 8}`

The Congestion Service publishes a message when the congestion level changes. The Routing Service subscribes to the topic instead of polling the Congestion Service for every route request.

### ActiveMQ Queue: `intersection-heartbeat-queue`

- **Producer:** Intersection Service
- **Consumer:** Intersection Watchdog
- **Frequency:** Approximately every five seconds

The Watchdog reports `DOWN` if no heartbeat has been received for more than 15 seconds.

The broker URL used by the services is `tcp://localhost:61616`.

## Project Structure

```text
sin-002-trafficflow/
├── common/
│   ├── docker-compose.yml
│   └── README.md
├── ingestion-service/
│   ├── pom.xml
│   └── src/
├── intersection-service/
│   ├── pom.xml
│   └── src/
├── congestion-service/
│   ├── pom.xml
│   └── src/
├── routing-service/
│   ├── pom.xml
│   └── src/
├── intersection-watchdog/
│   ├── pom.xml
│   └── src/
└── README.md
```

Most Java classes use the `co.wethinkcode.trafficflow` package. Some messaging and client classes use subpackages such as `mq` and `client`.

## Build

Build each service from its own directory.

```bash
cd ingestion-service && mvn clean package
cd ../intersection-service && mvn clean package
cd ../congestion-service && mvn clean package
cd ../routing-service && mvn clean package
cd ../intersection-watchdog && mvn clean package
```

Alternatively, build all five Maven projects from the repository root:

```bash
find . -name pom.xml -execdir mvn -q package \;
```

Each service produces an executable JAR inside its `target/` directory.

## Running the System

Start ActiveMQ before starting the services.

### 1. Start ActiveMQ

From the repository root:

```bash
cd common
docker compose up -d
docker compose ps
cd ..
```

The broker listens on `tcp://localhost:61616`.

### 2. Start Ingestion Service

In a terminal:

```bash
cd ingestion-service
java -jar target/ingestion-service.jar
```

### 3. Start Intersection Service

Open another terminal:

```bash
cd intersection-service
java -jar target/intersection-service.jar
```

This service loads intersection records from the Ingestion Service during startup.

### 4. Start Congestion Service

Open another terminal:

```bash
cd congestion-service
java -jar target/congestion-service.jar
```

### 5. Start Routing Service

Open another terminal:

```bash
cd routing-service
java -jar target/routing-service.jar
```

### 6. Start Intersection Watchdog

Open another terminal:

```bash
cd intersection-watchdog
java -jar target/intersection-watchdog.jar
```

Keep each service running in its own terminal.

## Testing the Endpoints

### Health Checks

```bash
curl http://localhost:7020/health
curl http://localhost:7021/health
curl http://localhost:7022/health
curl http://localhost:7023/health
curl http://localhost:7024/health
```

Each health endpoint should return:

```text
OK
```

### Retrieve Intersections

```bash
curl http://localhost:7020/intersections
```

### Retrieve an Individual Intersection

```bash
curl http://localhost:7021/intersections/INT-1001
```

Replace `INT-1001` with an ID that exists in the intersection data.

### Update Congestion

```bash
curl -i -X PUT http://localhost:7022/congestion \
  -H "Content-Type: application/json" \
  -d '{"level":4}'
```

Valid congestion levels range from `0` to `8`.

When the level changes, the Congestion Service publishes an update to ActiveMQ. The Routing Service receives the update through `congestion-topic`.

To retrieve the current level:

```bash
curl http://localhost:7022/congestion
```

### Request a Route Estimate

```bash
curl -i "http://localhost:7023/route?from=INT-1001&to=INT-1002"
```

Use two existing, active intersection IDs.

The current estimated travel time formula is:

```text
estimated travel time = 10 + (congestion level × 5) minutes
```

For example, congestion level `4` produces an estimated travel time of `30` minutes.

This is a simplified demonstration formula, not a real-time traffic prediction or a full road-network routing algorithm.

### Check Watchdog Status

```bash
curl -i http://localhost:7024/status
```

When heartbeats are arriving, the Watchdog reports `UP`.

If the Intersection Service stops sending heartbeats and the timeout passes, the Watchdog reports `DOWN` with HTTP status `503`.

## Current Limitations

- Travel time is calculated using a simplified formula.
- The Watchdog monitors missed heartbeats but does not yet monitor dead-letter queues.
- Automated test coverage has not yet been added.
- Service-specific documentation still needs to be aligned with the latest implementation.

## Domain Concepts

TrafficFlow models road intersections, districts, and signal types. The current routing endpoint focuses on intersection IDs, active status, congestion levels, and estimated travel time.


WTC-5QF4JVHX
