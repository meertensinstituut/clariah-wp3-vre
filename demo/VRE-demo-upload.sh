#!/bin/bash
curl -v 'http://localhost:8082/remote.php/webdav/nos.txt' \
     -X PUT \
     -H 'Content-Type: text/plain; charset=UTF-8' \
     -u test:achtkarakters \
     --data-binary '@nos.txt'

sleep 5

docker exec -it vre_postgres_1 psql -U objects objects -c "SELECT id FROM object WHERE filepath='admin/files/nos.txt';"
