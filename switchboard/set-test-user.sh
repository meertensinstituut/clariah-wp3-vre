#!/bin/bash

# create file:
mkdir -p /usr/local/tomcat/conf/Catalina/localhost && \
  touch /usr/local/tomcat/conf/Catalina/localhost/static.xml

echo "do static files exit?"
ls -al /usr/local/switchboard
ls -al /usr/local/switchboard/static_files

# add static files dir:
echo '<Context path="/static" docBase="/usr/local/switchboard/static_files/"></Context>' \
  > /usr/local/tomcat/conf/Catalina/localhost/static.xml

# add test user:
mkdir -p ./static_files && \
  touch ./static_files/test-user.json
echo "{\"user\": \"$TEST_USER\"}" > ./static_files/test-user.json