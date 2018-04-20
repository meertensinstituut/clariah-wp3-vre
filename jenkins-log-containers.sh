#!/bin/bash

echo 'save logging of vre containers in ./logs'

docker logs vre_integration_1  > logs/vre_integration_1.log
docker logs vre_recognizer_1 > logs/vre_recognizer_1.log
docker logs vre_deployment_1 > logs/vre_deployment_1.log
docker logs vre_dreamfactory_1 > logs/vre_dreamfactory_1.log
docker logs vre_owncloud_1 > logs/vre_owncloud_1.log
docker logs vre_trifecta_1 > logs/vre_trifecta_1.log
docker logs vre_kafka_1 > logs/vre_kafka_1.log
docker logs vre_fits_1 > logs/vre_fits_1.log
docker logs vre_solr_1 > logs/vre_solr_1.log
docker logs vre_zookeeper_1 > logs/vre_zookeeper_1.log
docker logs vre_postgres_1 > logs/vre_postgres_1.log
docker logs vre_switchboard_1 > logs/vre_switchboard_1.log
docker logs vre_indexer_1 > logs/vre_indexer_1.log