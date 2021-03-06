#/bin/bash
# Part of speech tagging, named entity recognition, and more:
docker cp frog.cmdi vre_postgres_1:/tmp/frog.xml

# Viewer of folia files:
docker cp folia.cmdi vre_postgres_1:/tmp/folia.xml

# Editor of deduplicate files, WIP:
docker cp demo.cmdi vre_postgres_1:/tmp/demo.xml

docker cp insert-services.sql vre_postgres_1:/tmp/insert-services.sql
docker exec -it vre_postgres_1 psql -U services services -f /tmp/insert-services.sql
