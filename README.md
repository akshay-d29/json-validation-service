# Json Validation Service

## Supported APIs
 
```
POST    /schema/SCHEMAID        - Upload a JSON Schema with unique `SCHEMAID`
GET     /schema/SCHEMAID        - Download a JSON Schema with unique `SCHEMAID`

POST    /validate/SCHEMAID      - Validate a JSON document against the JSON Schema identified by `SCHEMAID`
```

## Run Http Server Locally

``` sbt run ```\
The HTTP server runs on host = **localhost** and port = **8080**

## Run Tests Locally
``` sbt test ```

## Tech Stack
```cats-effect, circe, http4s```

## Examples

**Upload the JSON Schema**: ```curl http://localhost:8080/schema/schemaId -X POST -d @config-schema.json```

**Get Schema**: ```curl http://localhost:8080/schema/schemaId```

**Validate Json against Schema**: ```curl http://localhost:8080/validate/schemaId -X POST -d @config.json```
