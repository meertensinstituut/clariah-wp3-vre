#!/bin/bash
echo "build..."
mvn clean install && \
java -jar ./target/tagger-1.0-SNAPSHOT.jar
