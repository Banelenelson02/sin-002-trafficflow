
# Routing Service

## Overview

The Routing Service provides simplified estimated travel times between two intersections. It retrieves intersection details from the Intersection Service and uses the current congestion level to calculate an estimated journey time.

The service subscribes to the ActiveMQ topic `congestion-topic` to receive congestion updates published by the Congestion Service.

Part of the [TrafficFlow](../README.md) project. This is an independent Maven module with no parent POM.

## Responsibilities

- Expose a REST API for requesting estimated travel times.
- Retrieve intersection details from the Intersection Service.
- Check whether the requested intersections exist and are active.
- Receive congestion updates through ActiveMQ.
- Calculate estimated travel time using the current congestion level.
- Return route information as JSON.

## API endpoints

The service listens on port `7023`.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Checks whether the service is running. Returns `OK`. |
| `GET` | `/route?from=ID&to=ID` | Estimates travel time between two intersections. |

### Example request

```bash
curl -i "http://localhost:7023/route?from=INT-1001&to=INT-1005"
```

Replace the example IDs with intersection IDs present in your data.

A successful response includes the origin and destination IDs, the congestion level used, and the estimated travel time in minutes.

Example response shape:

```json
{
  "from": "INT-1001",
  "to": "INT-1005",
  "congestionLevel": 3,
  "estimatedTravelTimeMinutes": 25
}
```

The values above illustrate the response format; actual results depend on the service's current state and the requested intersections.

## Travel time calculation

The service uses this simplified formula:

```text
estimatedTravelTimeMinutes = 10 + (congestionLevel * 5)
```

For example, if the congestion level is `3`:

```text
10 + (3 * 5) = 25 minutes
```

This is a demonstration formula. The service does not calculate actual road distances, select roads from a road network, or use live traffic measurements.

## Request validation and errors

The route endpoint expects both `from` and `to` query parameters.

The service handles common request and dependency errors:

- **HTTP 400** — a required query parameter is missing, or a requested intersection is inactive.
- **HTTP 404** — a requested intersection cannot be found.
- **HTTP 502** — the Intersection Service cannot be reached.

Check that the Intersection Service is running and that the requested intersection IDs exist in the cleaned dataset.

## ActiveMQ integration

The Routing Service subscribes to the ActiveMQ topic `congestion-topic`.

- **Broker URL:** `tcp://localhost:61616`
- **Topic:** `congestion-topic`
- **Publisher:** Congestion Service
- **Subscriber:** Routing Service
- **Message format:** JSON, for example `{"level":3}`

When the Congestion Service publishes a new congestion level, the Routing Service receives the update and uses the current level for its travel-time calculations.

The broker configuration is defined in the service's `mq/MqConfig.java` class. ActiveMQ is configured through the Docker Compose setup in [`../common/`](../common).

## Service dependencies

The Routing Service depends on:

- **Intersection Service**, running on port `7021`, to retrieve intersection details.
- **ActiveMQ**, running on port `61616`, to receive congestion updates.

The Intersection Service must have loaded its data from the Ingestion Service before route requests can be fulfilled successfully.

## Project structure

```text
routing-service/
├── pom.xml
└── src/
    └── main/
        └── java/co/wethinkcode/trafficflow/
            ├── RoutingServiceApp.java
            └── mq/
                └── MqConfig.java
```

## Build

From the repository root:

```bash
mvn -f routing-service/pom.xml clean package
```

Alternatively, from inside the `routing-service` directory:

```bash
mvn clean package
```

## Run

From inside the `routing-service` directory:

```bash
mvn clean package
java -jar target/routing-service.jar
```

From the repository root:

```bash
java -jar routing-service/target/routing-service.jar
```

The service listens on port `7023`.

Ensure ActiveMQ and the Intersection Service are running when testing route requests.

## Testing

The module includes `HttpClientsTest.java` and `MqConfigTest.java`. You can also manually check the running service using:

```bash
curl -i http://localhost:7023/health
curl -i "http://localhost:7023/route?from=INT-1001&to=INT-1005"
```

For integration testing, update the congestion level through the Congestion Service and submit another route request to check the resulting estimate.

## Current limitations

- Travel time is calculated using a fixed formula rather than actual road distances.
- The service does not model road networks, alternative routes, or turn-by-turn directions.
- Estimates depend on the congestion level received by the service.
- Automated test classes: `HttpClientsTest.java` and `MqConfigTest.java`.
