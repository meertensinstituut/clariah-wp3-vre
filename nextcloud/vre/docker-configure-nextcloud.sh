#!/bin/bash
cd /var/www/html
echo "waiting for apps folder to be populated!"
while [ ! -d "/var/www/html/apps" ]
do
    echo -n "."
    sleep 5
done
echo "apps folder populated!"

cp -a /tmp/vre/. /var/www/html/apps/vre
echo "waiting for apps/vre folder to be populated!"
while [ ! -d "/var/www/html/apps/vre" ]
do
    echo -n "."
    sleep 5
done
echo "apps/vre folder populated!"

echo "downloading SAML app"
git clone -b v1.6.2 https://github.com/nextcloud/user_saml.git /var/www/html/apps/user_saml
while [ ! -d "/var/www/html/apps/user_saml" ]
do
    echo -n "."
    sleep 5
done
echo "apps/user_saml folder populated!"

# sometimes occ is not yet installed
# apache is run when occ is installed

echo -n "wait for apache (implies occ installed) ."
while ! pgrep apache2 2>&1 > /dev/null
do
    echo -n "."
    sleep 5
done
echo " done"

# clear existing admin user files:
rm -rf data/$OWNCLOUD_ADMIN_NAME/files/*
rm -rf data/$OWNCLOUD_ADMIN_NAME/files/.vre/*
rm -rf data/$OWNCLOUD_ADMIN_NAME/files_trashbin/*
rm -rf data/$OWNCLOUD_ADMIN_NAME/files_versions/*
rm -rf data/$OWNCLOUD_ADMIN_NAME/thumbnails/*

# empty log files:
> data/vrelog.txt
> data/files-scan.log

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
sudo -u www-data /usr/local/bin/php /var/www/html/occ config:system:set trusted_domains 2 --value "nextcloud"

# activate vre app:
sudo -u www-data /usr/local/bin/php /var/www/html/occ app:enable vre
# activate SAML
sudo -u www-data /usr/local/bin/php /var/www/html/occ app:enable user_saml

# add test user
export OC_PASS=$TEST_PASSWORD
su -s /bin/sh www-data -c \
  "php occ user:add --password-from-env --display-name=\"$TEST_USER\" --group=\"users\" $TEST_USER"

# check for new files:
nohup /var/www/html/apps/vre/docker-scan-files.sh </dev/null &>/dev/null &
# nohup bash -c "/var/www/html/apps/vre/docker-scan-files.sh </dev/null &>/dev/null" &

# do not delete or comment out the ps aux below, it is required to have the nohup command working. no idea why 
ps aux
echo "file scanner started!"
