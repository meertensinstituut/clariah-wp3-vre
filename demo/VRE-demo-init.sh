#/bin/bash
docker cp frog.cmdi vre_postgres_1:/tmp/frog.xml
docker cp ucto.cmdi vre_postgres_1:/tmp/ucto.xml
docker cp viewer.cmdi vre_postgres_1:/tmp/viewer.xml
docker cp folia.cmdi vre_postgres_1:/tmp/folia.xml

docker cp ucto.sql vre_postgres_1:/tmp/ucto.sql
docker exec -it vre_postgres_1 psql -U services services -f /tmp/ucto.sql
