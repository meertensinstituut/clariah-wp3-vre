#!/bin/bash
cd /opt/dreamfactory

echo "===INITIALISING DREAMFACTORY WITH ADMIN ${ADMIN_EMAIL}==="

php artisan df:env --db_connection=sqlite --df_install=Docker   
php artisan key:generate
php artisan df:setup --admin_email ${ADMIN_EMAIL} --admin_password ${ADMIN_PASSWORD} --admin_first_name ${ADMIN_FIRSTNAME} --admin_last_name ${ADMIN_LASTNAME}
chown -R www-data:www-data /opt/dreamfactory

echo "===START APACHE==="

service apache2 start
sleep 2

echo "===CREATE PACKAGE==="

cd /dreamfactory
cmd_createEncryptedPasswordServices="echo password_hash(\"${USER_SERVICES_PASSWORD}\", PASSWORD_BCRYPT, ['cost' => 10]);"
cmd_createEncryptedPasswordObjects="echo password_hash(\"${USER_OBJECTS_PASSWORD}\", PASSWORD_BCRYPT, ['cost' => 10]);"
USER_SERVICES_PASSWORD=$(php -r "$cmd_createEncryptedPasswordServices")
USER_OBJECTS_PASSWORD=$(php -r "$cmd_createEncryptedPasswordObjects")
PACKAGE_TIME=$(date +"%Y-%m-%d %T")
find . -type f -name "*.json" | xargs sed -i  "s#PACKAGE_TIME#${PACKAGE_TIME}#g"

find . -type f -name "*.json" | xargs sed -i  "s#APP_KEY_SERVICES#${APP_KEY_SERVICES}#g"
find . -type f -name "*.json" | xargs sed -i  "s#USER_SERVICES_EMAIL#${USER_SERVICES_EMAIL}#g"
find . -type f -name "*.json" | xargs sed -i  "s#USER_SERVICES_PASSWORD#${USER_SERVICES_PASSWORD}#g"
find . -type f -name "*.json" | xargs sed -i  "s#DB_SERVICES_HOST#${DB_SERVICES_HOST}#g"
find . -type f -name "*.json" | xargs sed -i  "s#DB_SERVICES_PORT#${DB_SERVICES_PORT}#g"
find . -type f -name "*.json" | xargs sed -i  "s#DB_SERVICES_DATABASE#${DB_SERVICES_DATABASE}#g"
find . -type f -name "*.json" | xargs sed -i  "s#DB_SERVICES_USER#${DB_SERVICES_USER}#g"
find . -type f -name "*.json" | xargs sed -i  "s#DB_SERVICES_PASSWORD#${DB_SERVICES_PASSWORD}#g"

find . -type f -name "*.json" | xargs sed -i  "s#APP_KEY_GET_OBJECTS#${APP_KEY_GET_OBJECTS}#g"
find . -type f -name "*.json" | xargs sed -i  "s#APP_KEY_OBJECTS#${APP_KEY_OBJECTS}#g"
find . -type f -name "*.json" | xargs sed -i  "s#USER_OBJECTS_EMAIL#${USER_OBJECTS_EMAIL}#g"
find . -type f -name "*.json" | xargs sed -i  "s#USER_OBJECTS_PASSWORD#${USER_OBJECTS_PASSWORD}#g"
find . -type f -name "*.json" | xargs sed -i  "s#DB_OBJECTS_HOST#${DB_OBJECTS_HOST}#g"
find . -type f -name "*.json" | xargs sed -i  "s#DB_OBJECTS_PORT#${DB_OBJECTS_PORT}#g"
find . -type f -name "*.json" | xargs sed -i  "s#DB_OBJECTS_DATABASE#${DB_OBJECTS_DATABASE}#g"
find . -type f -name "*.json" | xargs sed -i  "s#DB_OBJECTS_USER#${DB_OBJECTS_USER}#g"
find . -type f -name "*.json" | xargs sed -i  "s#DB_OBJECTS_PASSWORD#${DB_OBJECTS_PASSWORD}#g"

zip package.zip package.json system/*

echo "===INSTALL PACKAGE==="

cmd_getkey="curl -s -k -3 -X POST \"http://localhost/api/v2/system/admin/session\"  -d '{ \"email\" : \"${ADMIN_EMAIL}\", \"password\" : \"${ADMIN_PASSWORD}\" }'  -H \"Content-Type: application/json\" | jq -r .session_token"
tmp_key=$(eval "${cmd_getkey}")
curl -X POST -F 'files=@package.zip;type=application/zip' -H "X-DreamFactory-Session-Token: ${tmp_key}" http://localhost/api/v2/system/package
echo
echo "===DREAMFACTORY READY==="
