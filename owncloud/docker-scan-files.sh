#!/bin/bash
while true ; do
  SCAN_TIME=`date '+%Y-%m-%d %H:%M:%S'`
  printf "Start scanning files at ${SCAN_TIME}\n" \
  >> /var/www/html/data/files-scan.log

  sudo -u www-data /usr/local/bin/php /var/www/html/occ files:scan --all \
  >> /var/www/html/data/files-scan.log &

  sleep 5;
done
