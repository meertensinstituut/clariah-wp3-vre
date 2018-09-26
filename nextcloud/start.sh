#!/bin/bash
# use environment variables from root:
cp ../.env ./.env

docker-compose -p vre up --build -d

docker cp vre vre_nextcloud_1:/tmp/vre
docker exec -t -i vre_nextcloud_1 /tmp/vre/docker-configure-nextcloud.sh
