#!/bin/bash
while true ; do
  SCAN_TIME=`date '+%Y-%m-%d %H:%M:%S'`
  printf "Start scanning files at ${SCAN_TIME}\n" \
    >> /var/www/html/data/files-scan.log

  SCAN_RESULT="$(sudo -u www-data /usr/local/bin/php /var/www/html/occ files:scan --all)"
  if [[ $SCAN_RESULT = *"LockedException"* ]]; then
    printf "found a LockedException; try again..\n" \
      >> /var/www/html/data/files-scan.log
    sudo -u www-data /usr/local/bin/php /var/www/html/occ files:scan --all \
      >> /var/www/html/data/files-scan.log
  else
    printf "no LockedException\n" >> /var/www/html/data/files-scan.log
  fi
  END_SCAN_TIME=`date '+%Y-%m-%d %H:%M:%S'`
  printf "Finished scanning files at ${END_SCAN_TIME}\n" \
    >> /var/www/html/data/files-scan.log

  sleep 5;
done
