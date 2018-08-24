#!/bin/sh

chmod g+w /usr/local/var/www-data/flat.db
/usr/local/bin/lamachine-start-webserver
tail -F /dev/null