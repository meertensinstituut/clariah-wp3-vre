#!/bin/bash
set -e

source ./start-containers.sh --no-build

docker cp nextcloud/vre vre_nextcloud_1:/tmp/vre
cp .env nextcloud/.env
docker exec vre_nextcloud_1 /tmp/vre/docker-configure-nextcloud.sh

echo "Initialize components"
docker exec vre_integration_1 ./docker-initialize.sh

echo "Succesfully started the VRE" && \
echo "Start UI: cd ./ui && ./start-ui.sh"
