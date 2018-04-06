#!/bin/bash
if [ -z ${DB_SERVICES_USER} ] || [ -z ${DB_SERVICES_DATABASE} ] || [ -z ${DB_SERVICES_PASSWORD} ]; then

  echo "Couldn't create services"

else

  set -e
  
  POSTGRES="psql --username ${POSTGRES_USER}"
  
  echo "Creating database role: ${DB_SERVICES_USER}"
  
  $POSTGRES <<-EOSQL
  CREATE USER ${DB_SERVICES_USER} WITH CREATEDB PASSWORD '${DB_SERVICES_PASSWORD}';
EOSQL
  
  echo "Creating database: ${DB_SERVICES_DATABASE}"
  
  $POSTGRES <<EOSQL
  CREATE DATABASE ${DB_SERVICES_DATABASE} OWNER ${DB_SERVICES_USER};
EOSQL
  
fi;
