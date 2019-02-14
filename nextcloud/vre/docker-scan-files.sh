#!/bin/bash
while true ; do
  sudo -u www-data /usr/local/bin/php /var/www/html/occ files:scan --all;
  sleep 2;
done
