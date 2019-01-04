#!/bin/bash
docker cp tika.cmdi vre_postgres_1:/tmp/tika.cmdi
docker cp tika.sql vre_postgres_1:/tmp/tika.sql
docker exec -it vre_postgres_1 bash -c "cd /tmp && psql -U services services -f /tmp/tika.sql"
