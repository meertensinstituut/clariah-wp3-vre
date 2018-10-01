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
rm -rf data/$NEXTCLOUD_ADMIN_NAME/files/*
rm -rf data/$NEXTCLOUD_ADMIN_NAME/files/.vre/*
rm -rf data/$NEXTCLOUD_ADMIN_NAME/files_trashbin/*
rm -rf data/$NEXTCLOUD_ADMIN_NAME/files_versions/*
rm -rf data/$NEXTCLOUD_ADMIN_NAME/thumbnails/*

# clear existing test user files:
rm -rf data/$TEST_USER/files/*
rm -rf data/$TEST_USER/files/.vre/*
rm -rf data/$TEST_USER/files_trashbin/*
rm -rf data/$TEST_USER/files_versions/*
rm -rf data/$TEST_USER/thumbnails/*

# empty log files:
> data/vrelog.txt
> data/files-scan.log

echo "creating nextcloud admin and enabling vre app..."

# install nextcloud:
sudo -u www-data /usr/local/bin/php /var/www/html/occ maintenance:install \
 --database=$NEXTCLOUD_DB_TYPE \
 --database-name=$NEXTCLOUD_DB_NAME \
 --database-host=$NEXTCLOUD_DB_HOST \
 --database-user=$NEXTCLOUD_DB_USER \
 --database-pass=$NEXTCLOUD_DB_PASSWORD \
 --admin-user=$NEXTCLOUD_ADMIN_NAME \
 --admin-pass=$NEXTCLOUD_ADMIN_PASSWORD \
 --data-dir=$NEXTCLOUD_DATA_DIR

# do not add default files:
sudo -u www-data /usr/local/bin/php /var/www/html/occ config:system:set skeletondirectory

# add docker link 'nextcloud' to trusted domains:
sudo -u www-data /usr/local/bin/php /var/www/html/occ config:system:set trusted_domains 1 --value "nextcloud"
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
