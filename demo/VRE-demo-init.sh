#/bin/bash
# Part of speech tagging, named entity recognition, and more:
docker cp frog.cmdi vre_postgres_1:/tmp/frog.xml

# Viewer of folia files:
docker cp folia.cmdi vre_postgres_1:/tmp/folia.xml

# Editor of folia files, WIP:
docker cp foliaeditor.cmdi vre_postgres_1:/tmp/foliaeditor.xml

# Editor of deduplicate files, WIP:
docker cp demo.cmdi vre_postgres_1:/tmp/demo.xml

docker cp services.sql vre_postgres_1:/tmp/services.sql
docker exec -it vre_postgres_1 psql -U services services -f /tmp/services.sql
