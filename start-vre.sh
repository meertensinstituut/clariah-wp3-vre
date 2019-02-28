#!/bin/bash
set -e

source ./start-containers.sh

docker cp nextcloud/vre vre_nextcloud_1:/tmp/vre
cp .env nextcloud/.env
docker exec vre_nextcloud_1 /tmp/vre/docker-configure-nextcloud.sh

echo "Succesfully started the VRE" && \
echo "Start UI: cd ./ui && ./start-ui.sh"
