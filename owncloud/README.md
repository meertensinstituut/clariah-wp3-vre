# Owncloud

Host files and track file activity.

## Development
- Add to external php libraries: https://github.com/owncloud/core.git
- Logs can be found in `./data/`

## Deployment
- Run: `composer install`.
- Start components: `./start-owncloud.sh`
- Install and configure local client (settings see: `docker-compose.yml`)

### Test kafka
Run:
```
$ docker exec vre_owncloud_1 php apps/vre/lib/Kafka/ExampleConsumer.php
```
Run in another console:
```
$ docker exec vre_owncloud_1 php apps/vre/lib/Kafka/ExampleProducer.php
```

### Test owncloud hooks
- Add a file to local client.
- Check that `/tmp/vrelog.txt` contains logged file activity.
- Check console running ExampleConsumer.
