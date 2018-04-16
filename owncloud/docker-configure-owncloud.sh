#!/bin/bash
cd /var/www/html

# sometimes occ is not yet installed
# apache is run when occ is installed

echo -n "wait for apache (implies occ installed) ."
while ! pgrep apache2 2>&1 > /dev/null
do
    echo -n "."
    sleep 5
done
echo " done"

# clear all files from admin:
rm -rf data/$OWNCLOUD_ADMIN_NAME/files_versions/*
rm -rf data/$OWNCLOUD_ADMIN_NAME/files/*

echo "creating owncloud admin and enabling vre app..."

# install owncloud:
sudo -u www-data /usr/local/bin/php /var/www/html/occ maintenance:install \
 --database=$OWNCLOUD_DB_TYPE \
 --database-name=$OWNCLOUD_DB_NAME \
 --database-host=$OWNCLOUD_DB_HOST \
 --database-user=$OWNCLOUD_DB_USER \
 --database-pass=$OWNCLOUD_DB_PASSWORD \
 --admin-user=$OWNCLOUD_ADMIN_NAME \
 --admin-pass=$OWNCLOUD_ADMIN_PASSWORD \
 --data-dir=$OWNCLOUD_DATA_DIR

# do not add default files:
sudo -u www-data /usr/local/bin/php /var/www/html/occ config:system:set skeletondirectory

# add docker link 'owncloud' to trusted domains:
sudo -u www-data /usr/local/bin/php /var/www/html/occ config:system:set trusted_domains 1 --value "owncloud"

# activate vre app:
sudo -u www-data /usr/local/bin/php /var/www/html/occ app:enable vre

# check for new files:
nohup /var/www/html/apps/vre/docker-scan-files.sh </dev/null &>/dev/null &