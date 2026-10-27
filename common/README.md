
# common — Asynchronous Decoupling (MQ)

## Overview

Part of the [TrafficFlow](../README.md) project. This directory contains the Docker Compose configuration for the ActiveMQ broker shared by the services below. It is not a service itself and does not have its own application port.

The broker supports two asynchronous communication workflows:

1. Congestion updates between the Congestion Service and Routing Service.
2. Heartbeat messages between the Intersection Service and Intersection Watchdog.

### Topic: `congestion-topic`

The Routing Service receives congestion updates through an ActiveMQ topic instead of polling the Congestion Service for every route request.

- **Producer:** `congestion-service` (`../congestion-service`)
- **Consumer:** `routing-service` (`../routing-service`)
- **Message format:** JSON

Example payload:

```json
{
  "level": 4
}
```

Congestion levels range from `0` to `8`. When the level changes, the Congestion Service publishes an update. The Routing Service subscribes to the topic and updates its current congestion level.

Each independent Maven service maintains its own messaging configuration, including the broker URL and topic name.

### Queue: `intersection-heartbeat-queue`

The Intersection Watchdog monitors heartbeats from the Intersection Service to detect when recent heartbeat messages stop arriving.

- **Producer:** `intersection-service` (`../intersection-service`)
- **Consumer:** `intersection-watchdog` (`../intersection-watchdog`)
- **Message format:** Plain text

Example payload:

```text
Intersection service is alive
```

The Intersection Service publishes a heartbeat approximately every five seconds. The Watchdog records the time of the latest received heartbeat and reports the service as `DOWN` if no heartbeat has arrived for more than 15 seconds.

**Note:** The Watchdog currently detects missed heartbeats. Dead-letter queue monitoring has not been implemented.

## Project Structure

```text
common/
├── docker-compose.yml
└── README.md
```

This directory contains the broker configuration and documentation. The actual message publishing and consuming code belongs to the participating services.

Each service is an independent Maven project, so the relevant ActiveMQ dependencies and messaging configuration are maintained within the individual service modules.

## Build

There is nothing to build directly in this directory. It is used to start the ActiveMQ broker required by the messaging services.

## Run

Start the broker from the `common/` directory:

```bash
docker compose up -d
```

Check the container status:

```bash
docker compose ps
```

Follow the broker logs:

```bash
docker compose logs -f activemq
```

The broker uses the following local development endpoints:

- **Broker URL:** `tcp://localhost:61616`
- **Web console:** [http://localhost:8161](http://localhost:8161)

Start ActiveMQ before launching the services that publish or consume messages.

## Test

### Test congestion messaging

1. Start ActiveMQ.
2. Start the Congestion Service on port `7022`.
3. Start the Routing Service on port `7023`.
4. Change the congestion level:

```bash
curl -i -X PUT http://localhost:7022/congestion \
  -H "Content-Type: application/json" \
  -d '{"level":4}'
```

When the congestion level changes, the Congestion Service publishes a message to `congestion-topic`. Check the Routing Service logs for the received congestion update.

### Test heartbeat monitoring

1. Start ActiveMQ.
2. Start the Ingestion Service on port `7020`.
3. Start the Intersection Service on port `7021`.
4. Start the Intersection Watchdog on port `7024`.
5. Check the watchdog status:

```bash
curl -i http://localhost:7024/status
```

When heartbeats are being received, the response reports `UP`.

To test the timeout, stop the Intersection Service and wait more than 15 seconds. Check `/status` again. The Watchdog should report `DOWN` with HTTP status `503`.

## Current Limitations

- Dead-letter queue monitoring and related alerting have not been implemented.
- The broker URL is configured for local development.
- Messaging configuration is maintained in each independent service module rather than in a shared Java module.
