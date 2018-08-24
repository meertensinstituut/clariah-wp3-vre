#!/bin/bash

cd /tmp/deployment
mvn clean install
cp deployment-api/target/deployment-api.war /usr/local/tomcat/webapps/deployment-service.war

/usr/local/tomcat/bin/catalina.sh run

# keep docker running when redeploying:
tail -f /dev/null
