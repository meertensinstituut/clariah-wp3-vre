FROM postgres:10.1

MAINTAINER Matthijs Brouwer <matthijs.brouwer@meertens.knaw.nl>

#ENV DB_OBJECTS_USER objects
#ENV DB_OBJECTS_PASSWORD pwobjects
#ENV DB_OBJECTS_NAME objects
#ENV DB_SERVICES_USER services
#ENV DB_SERVICES_PASSWORD pwservices
#ENV DB_SERVICES_NAME services

ADD config/create_objects.sh   /docker-entrypoint-initdb.d/create_objects.sh
ADD config/create_services.sh  /docker-entrypoint-initdb.d/create_services.sh
