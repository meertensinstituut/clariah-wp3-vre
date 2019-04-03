#!/bin/bash

if [ ! -f nos.txt ]; then
	echo "ERR: nos.txt doesn't exist!"
	echo "     Copy & paste a news item from http://www.nos.nl as plain text into nos.txt"
	exit
fi

curl -v 'http://localhost:8082/remote.php/webdav/nos.txt' \
     -X PUT \
     -H 'Content-Type: text/plain; charset=UTF-8' \
     -u test:achtkarakters \
     --data-binary '@nos.txt'

sleep 5

docker exec -it vre_postgres_1 psql -U objects objects -c "SELECT id FROM object WHERE filepath='test/files/nos.txt';"
