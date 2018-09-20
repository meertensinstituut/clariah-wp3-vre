#!/bin/bash

echo "populate service registry with TEST and VIEWER service..."
PGPASSWORD=${DB_SERVICES_PASSWORD}
psql -h postgres -U ${DB_SERVICES_USER} ${DB_SERVICES_DATABASE} -f ./insert-services.sql
