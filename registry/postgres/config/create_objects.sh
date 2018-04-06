#!/bin/bash
if [ -z ${DB_OBJECTS_USER} ] || [ -z ${DB_OBJECTS_DATABASE} ] || [ -z ${DB_OBJECTS_PASSWORD} ]; then
  
  echo "Couldn't create objects"

else

  set -e
  
  POSTGRES="psql --username ${POSTGRES_USER}"
  
  echo "Creating database role: ${DB_OBJECTS_USER}"
  
  $POSTGRES <<-EOSQL
  CREATE USER ${DB_OBJECTS_USER} WITH CREATEDB PASSWORD '${DB_OBJECTS_PASSWORD}';
EOSQL
  
  echo "Creating database: ${DB_OBJECTS_DATABASE}"
  
  $POSTGRES <<EOSQL
  CREATE DATABASE ${DB_OBJECTS_DATABASE} OWNER ${DB_OBJECTS_USER};
EOSQL

fi;
