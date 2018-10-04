FROM postgres:10.1

MAINTAINER Matthijs Brouwer <matthijs.brouwer@meertens.knaw.nl>

ADD config/create_objects.sh   /docker-entrypoint-initdb.d/create_objects.sh
ADD config/create_services.sh  /docker-entrypoint-initdb.d/create_services.sh
