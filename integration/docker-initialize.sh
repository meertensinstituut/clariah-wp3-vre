#!/bin/bash
set -e

(cd ./services && ./insert-services.sh)

mvn clean install -Dmaven.test.skip
java -jar ./target/integration-1.0-SNAPSHOT.jar
