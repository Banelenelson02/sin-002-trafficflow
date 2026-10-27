
# Congestion Service

## Overview

The Congestion Service tracks the city-wide congestion level, represented by an integer from `0` to `8`. It exposes a REST API to retrieve and update the current level.

When the congestion level changes, the service publishes a JSON message to the ActiveMQ topic `congestion-topic`. The Routing Service uses these updates when calculating estimated travel times.

Part of the [TrafficFlow](../README.md) project. This is an independent Maven module with no parent POM.

## Responsibilities

- Maintain the current city-wide congestion level.
- Expose an endpoint to check service health.
- Return the current congestion level through a REST API.
- Accept updates to the congestion level.
- Validate that congestion levels are integers between `0` and `8`.
- Publish congestion updates to ActiveMQ when the level changes.

## API endpoints

The service listens on port `7022`.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Checks whether the service is running. Returns `OK`. |
| `GET` | `/congestion` | Retrieves the current congestion level. |
| `PUT` | `/congestion` | Updates the congestion level. Valid values are integers from `0` to `8`. |

### Example requests

Check service health:

```bash
curl -i http://localhost:7022/health
```

Retrieve the current congestion level:

```bash
curl -i http://localhost:7022/congestion
```

Update the congestion level to `5`:

```bash
curl -i -X PUT http://localhost:7022/congestion \
  -H "Content-Type: application/json" \
  -d '{"level": 5}'
```

The request body uses JSON with a `level` field. The value must be an integer between `0` and `8`.

## ActiveMQ integration

When the congestion level changes, the service publishes a JSON message to the ActiveMQ topic `congestion-topic`.

- **Broker URL:** `tcp://localhost:61616`
- **Topic:** `congestion-topic`
- **Message format:** JSON
- **Example message:** `{"level":5}`
- **Consumer:** Routing Service

The broker configuration is defined in the service's `mq/MqConfig.java` class. ActiveMQ is configured through the Docker Compose setup in [`../common/`](../common).

The service publishes updates when the level changes rather than publishing an update for every request that leaves the level unchanged.

## Project structure

```text
congestion-service/
├── pom.xml
└── src/
    └── main/
        └── java/co/wethinkcode/trafficflow/
            ├── CongestionServiceApp.java
            └── mq/
                └── MqConfig.java
```

## Build

From the repository root:

```bash
mvn -f congestion-service/pom.xml clean package
```

Alternatively, from inside the `congestion-service` directory:

```bash
mvn clean package
```

## Run

From inside the `congestion-service` directory:

```bash
mvn clean package
java -jar target/congestion-service.jar
```

From the repository root:

```bash
java -jar congestion-service/target/congestion-service.jar
```

The service listens on port `7022`.

Ensure the ActiveMQ broker is running if you want to test message publishing.

## Testing

The module includes `CongestionTest.java` and `CongestionControllerTest.java`. You can also manually test the API using:

```bash
curl -i http://localhost:7022/health
curl -i http://localhost:7022/congestion
```

You can also update the congestion level:

```bash
curl -i -X PUT http://localhost:7022/congestion \
  -H "Content-Type: application/json" \
  -d '{"level": 5}'
```

Try an out-of-range value to verify validation:

```bash
curl -i -X PUT http://localhost:7022/congestion \
  -H "Content-Type: application/json" \
  -d '{"level": 9}'
```

An invalid congestion level should be rejected.

To verify the integration, change the congestion level and check that a message is published to `congestion-topic` and received by the Routing Service.

## Current limitations

- The congestion level is a city-wide value rather than a measurement for each individual road.
- Estimated travel times are simplified calculations, not predictions based on live traffic data.
- Automated test classes: `CongestionTest.java` and `CongestionControllerTest.java`.
