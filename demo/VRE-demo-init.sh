#/bin/bash

docker cp ucto.cmdi vre_postgres_1:/tmp/ucto.xml
docker cp ucto.sql vre_postgres_1:/tmp/ucto.sql
docker exec -it vre_postgres_1 psql -U services services -f /tmp/ucto.sql
