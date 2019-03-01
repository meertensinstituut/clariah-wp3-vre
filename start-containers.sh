#!/bin/bash
set -e

BUILD_OR_NOT=${1:--no-build} # or --build

# Combine compose files of different components
# (add new compose file after blank, and before root)
docker-compose \
  -f ./docker-compose-blank.yml \
  -f ./integration/docker-compose.yml \
  -f ./nextcloud/docker-compose.yml \
  -f ./recognizer/docker-compose.yml \
  -f ./registry/docker-compose.yml \
  -f ./switchboard/docker-compose.yml \
  -f ./indexer/docker-compose.yml \
  -f ./deployment-service/docker-compose.yml \
  -f ./tagger/docker-compose.yml \
  -f ./docker-compose.yml \
  -p vre up -d $BUILD_OR_NOT # replace with 'config' to see generated docker-compose-file

