#!/bin/bash
set -e

source ./start-containers.sh

docker cp nextcloud/vre vre_nextcloud_1:/tmp/vre
cp .env nextcloud/.env
docker exec vre_nextcloud_1 /tmp/vre/docker-configure-nextcloud.sh

echo "Run integration tests"
docker exec vre_integration_1 /usr/local/integration/docker-run-integration-tests.sh
