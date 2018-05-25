Clariah VRE
===

Clariah VRE consists of the following components:

- **Integration**: to verify that all components play nicely together
- **Owncloud**: to host files
- **Recognizer**: to determine file types
- **Registry**: to persist objects and list services
- **Switchboard**: to prepare service requests

See vre-model.svg for an overview of the VRE.

Using:
- Java 9
- Php 7

Deployment
---

- To start all containers: `./start-vre.sh`. 
  - To run integration tests in remote debug-mode, add: `debug`.
  - NB. Atm saxon-utils of deployment-service is added by hand: see readme of component.
- Start **UI**: 
  - For local use: install browser addon that enables CORS
  - `cd ./ui && ./start-ui.sh`
- For details, see README of components.

Development
---

- Add new components: expand docker-compose command in `./start-vre.sh`.
    - Environment variables can be added to `./.env`.
    - References to component files and volumes in `./<component>/docker-compose.yml` should be overwritten in `./docker-compose.yml`
  
Manual demo
---

### Integration
- Run `./start-vre.sh`.
- During integration tests files will be uploaded and kafka topics will be created.
- After integration:
  - See kafka queues: go to `localhost:9000/#/observe` for monitoring tool Trifecta
  - See objects database: connect with client using `jdbc:postgresql://localhost:5432/objects` (credentials, see `DB_OBJECTS_USER` and `DB_OBJECTS_PASSWORD` in `.env`)

### Uploading files:

- Upload file using console:

```
curl -v 'http://localhost:8082/remote.php/webdav/testfile.txt' \
     -X PUT \
     -H 'Content-Type: text/plain; charset=UTF-8' \
     -u admin:admin \
     -d 'testfile sent with <3 from curl'
```

- Upload file using client:
  - Install owncloud client
  - Login to server `http://localhost:8082` (credentials, see e.g. `OWNCLOUD_ADMIN_NAME` and `OWNCLOUD_ADMIN_PASSWORD` in `.env`)
  - Add file.

## Ports

Containers expose the following ports:
```
3000->3000/tcp   vre_ui
5432->5432/tcp   vre_postgres_1
80->8080/tcp     vre_deployment_1
8080->8080/tcp   vre_fits_1
8081->80/tcp     vre_recognizer_1
8082->80/tcp     vre_owncloud_1
8085->8085/tcp   vre_integration_1
8089->80/tcp     vre_dreamfactory_1
9000->9000/tcp   vre_trifecta_1
9010->8080/tcp   vre_switchboard_1
33104->9092/tcp  vre_kafka_1
```