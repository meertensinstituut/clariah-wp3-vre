#!/bin/bash
cd /usr/local/integration/

echo "populate service registry with test service with user [${DB_SERVICES_USER}] and database [${DB_SERVICES_DATABASE}]..."
PGPASSWORD=${DB_SERVICES_PASSWORD}
psql -h postgres -U ${DB_SERVICES_USER} ${DB_SERVICES_DATABASE} -f test-service.sql

echo 'start integration tests...'
if [ "$1" = "debug" ]; then
  # Change maven debug port to '*:8085':
  sed -i -e 's/8000/*:8085/g' /usr/share/maven/bin/mvnDebug
  mvnDebug test
else
  mvn clean test
fi
