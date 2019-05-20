#!/bin/bash
set -e

echo "merge component compose files into ./docker-compose.yml"
source ./merge-containers.sh

echo "pull new images"
docker-compose pull

echo "pull builder image for vre java projects"
docker pull knawhuc/clariah-wp3-vre-builder

echo "build vre java projects"
docker-compose -p vre build --no-cache \
  switchboard \
  deployment \
  recognizer \
  tagger \
  integration

echo "start containers"
docker-compose -p vre up -d

echo "configure nextcloud"
docker cp nextcloud/vre vre_nextcloud_1:/tmp/vre
cp .env nextcloud/.env
docker exec vre_nextcloud_1 /tmp/vre/docker-configure-nextcloud.sh

echo "initialize vre"
docker exec vre_integration_1 ./docker-initialize.sh

echo "run integration tests"
docker exec vre_integration_1 ./docker-test.sh
