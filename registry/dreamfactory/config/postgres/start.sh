#!/bin/bash

cd /postgres

echo "===INITIALISING DATABASE ${DB_OBJECTS_DATABASE} ON ${DB_OBJECTS_HOST}:${DB_OBJECTS_PORT}==="

until psql -U ${DB_OBJECTS_USER} -h ${DB_OBJECTS_HOST} -p ${DB_OBJECTS_PORT} ${DB_OBJECTS_DATABASE} < /postgres/object.sql; do 
  >&2 echo "${DB_OBJECTS_HOST}:${DB_OBJECTS_PORT} is unavailable - sleeping"
  sleep 1
done

echo "===INITIALISING DATABASE ${DB_SERVICES_DATABASE} ON ${DB_SERVICES_HOST}:${DB_SERVICES_PORT}==="

until psql -U ${DB_SERVICES_USER} -h ${DB_SERVICES_HOST} -p ${DB_SERVICES_PORT} ${DB_SERVICES_DATABASE} < /postgres/service.sql; do 
  >&2 echo "${DB_SERVICES_HOST}:${DB_SERVICES_PORT} is unavailable - sleeping"
  sleep 1
done   

echo "===DATABASES READY==="
