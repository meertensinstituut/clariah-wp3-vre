CLARIAH WP3 VRE under the hood
===

The CLARIAH WP3 VRE consists of the following components:

- **Owncloud**: to host files
- **Recognizer**: to determine file types
- **Registry**: to persist objects and list services
- **Switchboard**: to prepare service requests
- **UI**: to allow users to interact with the VRE through a user interface
- **Integration**: to verify that all components play nicely together

See [vre-model.svg](vre-model.svg) for an overview of the VRE.

Using:
- Java 9 or higher
- Php 7

Deployment
---

- See README of components.
- To start all containers, run: `./start-vre.sh`. 
  - To run integration tests in remote debug-mode, add: `debug`.
  - NB. Atm saxon-utils of deployment-service is added by hand: see readme of component.
- UI runs at `localhost:3000`

Development
---

- Add new components: expand docker-compose command in `./start-vre.sh`.
    - Environment variables can be added to `./.env`.
    - References to component files and volumes in `./<component>/docker-compose.yml` should be overwritten in `./docker-compose.yml`
  
Demo
---

See [./demo](./demo/README.md).

### Integration
- Run `./start-vre.sh`.
- During integration tests files will be uploaded and kafka topics will be created.
- After integration:
  - See kafka queues: go to `localhost:9000/#/observe` for monitoring tool Trifecta
  - See objects database: connect with client using `jdbc:postgresql://localhost:5432/objects` (credentials, see `DB_OBJECTS_USER` and `DB_OBJECTS_PASSWORD` in `.env`)

### Uploading files:

- Upload file using console:

```sh
curl -v 'http://localhost:8082/remote.php/webdav/testfile.txt' \
     -X PUT \
     -H 'Content-Type: text/plain; charset=UTF-8' \
     -u admin:admin \
     -d 'testfile sent with <3 from curl'
```

- Upload file using client:
  - Install nextcloud client
  - Login to server `http://localhost:8082` (credentials, see e.g. `NEXTCLOUD_ADMIN_NAME` and `NEXTCLOUD_ADMIN_PASSWORD` in `.env`)
  - Add file.

## Code style

The [HuygensING checkstyle](https://github.com/HuygensING/checkstyle) configuration files are used.

## Ports

Run: `docker ps` to see which ports are used
