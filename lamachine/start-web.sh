#!/bin/sh

chmod g+w /usr/local/var/www-data/flat.db
## enable the next line to debug ucto in lamachine
# docker cp ./ucto.py vre_lamachine_1:/usr/local/src/clamservices/clamservices/config/ucto.py
/usr/local/bin/lamachine-start-webserver
tail -F /dev/null