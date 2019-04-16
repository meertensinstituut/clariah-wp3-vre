#!/bin/bash
set -e

source ./merge-containers.sh

docker-compose pull

# force building java images
docker-compose -p vre build --no-cache \
  switchboard \
  deployment \
  recognizer \
  tagger \
  integration

docker-compose -p vre up -d

docker cp nextcloud/vre vre_nextcloud_1:/tmp/vre
cp .env nextcloud/.env
docker exec vre_nextcloud_1 /tmp/vre/docker-configure-nextcloud.sh

echo "Initialize components"
docker exec vre_integration_1 ./docker-initialize.sh

echo "Run integration tests"
docker exec vre_integration_1 ./docker-run-integration-tests.sh
