#!/bin/bash

# create file:
mkdir -p /usr/local/tomcat/conf/Catalina/localhost && \
  touch /usr/local/tomcat/conf/Catalina/localhost/static.xml

# add static files dir:
echo '<Context path="/static" docBase="/usr/local/switchboard/static_files/"></Context>' \
  > /usr/local/tomcat/conf/Catalina/localhost/static.xml

# add test user:
mkdir -p ./static_files && \
  touch ./static_files/test-user.json
echo "{\"user\": \"$TEST_USER\"}" > ./static_files/test-user.json