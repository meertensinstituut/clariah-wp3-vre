#!/bin/bash

# Combine compose files of different components
# (add new compose file after blank, and before root)
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
  -p vre up -d --build # replace with 'config' to see generated docker-compose-file

docker exec vre_owncloud_1 /var/www/html/apps/vre/docker-configure-owncloud.sh
echo "wait 180 secs to make sure all services are running..."
sleep 180

echo "Run integration tests"
docker exec vre_integration_1 /usr/local/integration/docker-run-integration-tests.sh

echo "Succesfully build VRE"
echo "Start UI: cd ./ui && ./start-ui.sh"