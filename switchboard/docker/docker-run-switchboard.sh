#!/bin/bash
echo "build switchboard..." && \
mvn clean install && \
echo "deploy switchboard.war..." && \
cp /usr/local/switchboard/target/switchboard-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/switchboard.war && \
echo "get registry token..." && \
source ./docker/docker-get-objects-token.sh \
echo "start tomcat..." && \
export CATALINA_OPTS="-Xmx700m --add-modules java.xml.bind,java.xml.ws --add-exports java.base/jdk.internal.ref=ALL-UNNAMED" && \
/usr/local/tomcat/bin/catalina.sh run
