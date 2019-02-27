#!/bin/bash

echo "add test user..."
./set-test-user.sh

echo "remove work dirs in tmp deployment volume..."
rm -rf "${DEPLOYMENT_VOLUME}"/*

# $USER is needed for tests
echo "build switchboard..." && \
export USER=$(whoami) && \
mvn clean install && \

echo "deploy switchboard.war..." && \
cp /usr/local/switchboard/target/switchboard-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/switchboard.war && \

echo "start tomcat..." && \
export CATALINA_OPTS="-Xmx700m --add-exports java.base/jdk.internal.ref=ALL-UNNAMED" && \
/usr/local/tomcat/bin/catalina.sh run

# keep docker running when redeploying jar:
tail -f /dev/null
