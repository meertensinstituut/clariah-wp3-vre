#!/bin/bash
cd /usr/local/integration/

(cd ./services && ./insert-services.sh)

echo 'start integration tests...'
if [ "$1" = "debug" ]; then
  # Change maven debug port to '*:8085':
  sed -i -e 's/8000/*:8085/g' /usr/share/maven/bin/mvnDebug
  mvnDebug test
else
  mvn clean test
fi
