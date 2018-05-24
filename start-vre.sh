#!/bin/bash

[[ $(docker ps | grep 'owncloud') ]] && CONFIGURE_OWNCLOUD=false || CONFIGURE_OWNCLOUD=true

# Combine compose files of different components
# (Add new compose file after blank, and before root)
docker-compose \
  -f ./docker-compose-blank.yml \
  -f ./integration/docker-compose.yml \
  -f ./owncloud/docker-compose.yml \
  -f ./recognizer/docker-compose.yml \
  -f ./registry/docker-compose.yml \
  -f ./switchboard/docker-compose.yml \
  -f ./indexer/docker-compose.yml \
  -f ./deployment-service/docker-compose.yml \
  -f ./docker-compose.yml \
  -p vre up -d --build # replace this line with 'config' to see generated docker-compose-file

# Configure owncloud only when container isn't started yet:
if [ "$CONFIGURE_OWNCLOUD" = true ]; then
  docker exec vre_owncloud_1 /var/www/html/apps/vre/docker-configure-owncloud.sh
  echo "wait 180 secs to make sure all services are running.."
  sleep 180
fi

echo "Run integration tests (add 'debug' to start in debug mode)"
docker exec vre_integration_1 /usr/local/integration/docker-run-integration-tests.sh $1
