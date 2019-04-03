#!/bin/bash

# First run ./start-vre.sh from root

docker exec -it vre_integration_1 ./docker-initialize.sh
docker exec -it vre_integration_1 ./docker-run-integration-tests.sh $1