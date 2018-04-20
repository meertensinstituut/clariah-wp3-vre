#!/bin/bash

# kill and prune VRE containers:
docker kill vre_integration_1
docker kill vre_recognizer_1
docker kill vre_deployment_1
docker kill vre_dreamfactory_1
docker kill vre_owncloud_1
docker kill vre_trifecta_1
docker kill vre_kafka_1
docker kill vre_fits_1
docker kill vre_solr_1
docker kill vre_zookeeper_1
docker kill vre_postgres_1
docker kill vre_switchboard_1
docker kill vre_indexer_1

docker container prune -f

docker ps -a 
