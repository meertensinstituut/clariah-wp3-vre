#!/bin/bash
# use environment variables from root:
cp ../.env ./.env

docker-compose -p vre up -d
docker exec -t -i vre_owncloud_1 /var/www/html/apps/vre/docker-configure-owncloud.sh
