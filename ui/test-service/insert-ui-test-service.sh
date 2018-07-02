#/bin/bash
docker cp ui-test-service.cmdi vre_postgres_1:/tmp/ui-test-service.xml
docker cp ui-test-service.sql vre_postgres_1:/tmp/ui-test-service.sql
docker exec -it vre_postgres_1 psql -U services services -f /tmp/ui-test-service.sql
