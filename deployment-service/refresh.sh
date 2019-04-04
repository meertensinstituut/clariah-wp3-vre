#!/bin/bash

docker cp . vre_deployment_1:/usr/local/deployment
docker exec -it vre_deployment_1 sh -c "mvn clean install && cp deployment-api/target/deployment-api.war /usr/local/tomcat/webapps/deployment-service.war "

