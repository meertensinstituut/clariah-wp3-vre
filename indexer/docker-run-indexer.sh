#!/bin/bash
mvn install
sleep 20 # TODO: check registry has been started
java -jar --add-modules java.xml.bind ./target/indexer-0.1-SNAPSHOT.jar consume
