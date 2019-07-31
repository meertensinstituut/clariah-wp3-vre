#!/bin/bash
docker cp recipe.cmdi vre_postgres_1:/tmp/recipe.cmdi
docker cp service.sql vre_postgres_1:/tmp/service.sql
docker exec -it vre_postgres_1 bash -c "cd /tmp && psql -U services services -f /tmp/service.sql"
