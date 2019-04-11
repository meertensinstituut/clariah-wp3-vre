# Deployment service

## Deployment
Run: `./start.sh`

## Tests
Run: `docker exec vre_deployment_1 mvn clean test`

## Status codes

```
url: {protocol}://{host}:{port}/deployment-service/a

PUT {url}/exec/{service}/{workdir}   200    created
PUT {url}/exec/{service}/{workdir}   403    already running
GET {url}/exec/{service}/{workdir}   200    finished
GET {url}/exec/{service}/{workdir}   202    continue
```
