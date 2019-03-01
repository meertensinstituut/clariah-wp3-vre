CLARIAH WP3 VRE under the hood
===

The CLARIAH WP3 VRE consists of the following components:

- **UI**: to allow users to interact with the VRE through a user interface
- **Nextcloud**: to host files
- **Recognizer**: to determine file types
- **Tagger**: to tag (new) files in the objects registry
- **Registry**: to list services and save metadata about files
- **Switchboard**: to prepare service deployment
- **Deployment-service**: to deploy a (remote) service
- **Integration**: to verify that all components play nicely together

See component readme's for more information.

See [vre-model.svg](vre-model.svg) for an overview of the VRE.

Using:
- Java 11
- Php 7
- [Kafka](https://kafka.apache.org/)
- [Nextcloud](https://nextcloud.com/)
- [Dreamfactory](https://www.dreamfactory.com/)

## Local deployment

- To start all containers, run: `./start-vre.sh`. 
  - To run integration tests in remote debug-mode, add: `debug`.
  - NB. Atm saxon-utils of deployment-service is added by hand: see readme of component.
- To start UI, run: `cd ./ui && ./start-ui.sh`. 
- UI runs at `localhost:3000`

## Integration tests
- Run `./test-vre.sh` which:
  - Builds all components
  - Runs tests in `./integration`
    
## Development

- Add new component: 
  - Environment variables can be added to `./.env`.
  - Expand docker-compose command in `./start-vre.sh`.
  - References to component files and volumes in `./<component>/docker-compose.yml` should be overwritten in `./docker-compose.yml`
  - Integration tests can be added to java app in `./integation`

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
