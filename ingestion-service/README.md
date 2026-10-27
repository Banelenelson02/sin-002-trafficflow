
# Ingestion Service

## Overview

The Ingestion Service is the first stage of the TrafficFlow pipeline. It reads the deliberately messy `intersections-legacy.csv` file, cleans and normalizes the records, and exposes the cleaned intersection data through a REST API.

This is an independent Maven module with no parent POM.

Part of the [TrafficFlow](../README.md) project.

## Responsibilities

- Read `intersections-legacy.csv` from the application's classpath resources.
- Trim leading and trailing whitespace and normalize inconsistent values.
- Convert missing or placeholder values into `null`.
- Normalize intersection IDs, district names, signal types, and boolean flags.
- Skip rows with too few columns or invalid/blank intersection IDs.
- Remove duplicate records using normalized intersection IDs.
- Fill missing fields in a record using available values from duplicate records.
- Expose the cleaned data through REST endpoints.

## Known data issues

The source CSV is deliberately messy. The service handles issues such as:

- **Inconsistent casing** in intersection IDs, district names, and signal types.
- **Padding and whitespace**, including leading and trailing spaces.
- **Duplicate records** with IDs that differ only by casing.
- **Missing or placeholder values**, including blank fields, `N/A`, `TBD`, `unknown`, `-`, and `NaN`.
- **Inconsistent boolean representations**, including `Y`/`N`, `yes`/`no`, `1`/`0`, and `true`/`false`.
- **Rows with insufficient columns** or invalid intersection IDs.

The current implementation does not parse or validate date formats, numeric counts, or naming and spelling variants beyond the normalization described above.

## Worked example

Consider these sample rows from `intersections-legacy.csv`:

```csv
intersection_id,District ,signal_type,active_flag
INT-1001, Downtown ,4-way,Y
INT-1005,Downtown,Roundabout,true
int-1005,downtown ,ROUNDABOUT,TRUE
INT-1007,Eastside,,1
INT-1015,,4-way,Y
```

A possible cleaned representation is:

```json
[
  {
    "id": "INT-1001",
    "district": "Downtown",
    "signalType": "4-way",
    "active": true
  },
  {
    "id": "INT-1005",
    "district": "Downtown",
    "signalType": "roundabout",
    "active": true
  },
  {
    "id": "INT-1007",
    "district": "Eastside",
    "signalType": null,
    "active": true
  },
  {
    "id": "INT-1015",
    "district": null,
    "signalType": "4-way",
    "active": true
  }
]
```

What happens:

- `INT-1001`: whitespace is removed from the district, and `Y` becomes `true`.
- `INT-1005` and `int-1005`: the records share the same normalized ID, so they are combined into one record.
- `INT-1007`: the missing signal type remains `null` rather than being guessed.
- `INT-1015`: the missing district remains `null`.

This example illustrates the intended cleaning behavior. The exact output depends on the actual source data.

## API endpoints

The service listens on port `7020`.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Checks whether the service is running. Returns `OK`. |
| `GET` | `/intersections` | Returns the cleaned intersection records as JSON. |

### Example requests

Check the service health:

```bash
curl http://localhost:7020/health
```

Expected response:

```text
OK
```

Retrieve the cleaned intersection records:

```bash
curl http://localhost:7020/intersections
```

The response contains a JSON array of intersection records with the fields `id`, `district`, `signalType`, and `active`.

## Project structure

```text
ingestion-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/co/wethinkcode/trafficflow/
    │   │   ├── IngestionServiceApp.java
    │   │   ├── Intersection.java
    │   │   └── IntersectionCsvReader.java
    │   └── resources/
    │       └── intersections-legacy.csv
    └── test/
        └── java/
```

## Build

From the repository root, run:

```bash
mvn -f ingestion-service/pom.xml clean package
```

Alternatively, from inside the `ingestion-service` directory:

```bash
mvn clean package
```

## Run

From the `ingestion-service` directory, build and start the service:

```bash
mvn clean package
java -jar target/ingestion-service.jar
```

The service starts on port `7020`.

If you are running the service from the repository root, use the JAR path:

```bash
java -jar ingestion-service/target/ingestion-service.jar
```

## Testing

No automated tests are currently documented for this module. You can manually verify the service using the following commands:

```bash
curl -i http://localhost:7020/health
curl -i http://localhost:7020/intersections
```

The health endpoint should return HTTP `200` with `OK`. The intersections endpoint should return HTTP `200` with the cleaned records in JSON format.

Automated tests could be added later to cover whitespace normalization, placeholder handling, boolean conversion, duplicate merging, invalid rows, and missing CSV values.
