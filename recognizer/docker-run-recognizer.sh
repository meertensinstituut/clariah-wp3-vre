#!/bin/bash
mvn install && \
sleep 20  && \ # TODO: check registry has been started
source ./docker-get-objects-token.sh  && \
java -jar --add-modules java.xml.bind ./target/recognizer-0.1-SNAPSHOT.jar consume
