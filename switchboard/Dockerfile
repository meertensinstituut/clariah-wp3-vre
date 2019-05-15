FROM knawhuc/clariah-wp3-vre-builder as builder

ARG USER_TO_LOCK_WITH
ARG USER_TO_UNLOCK_WITH
ARG TEST_USER
ARG TEST_PASSWORD
ARG DEPLOYMENT_VOLUME
ENV KAFKA_HOST_NAME="test-val"
ENV SWITCHBOARD_TOPIC_NAME="test-val"
ENV NEXTCLOUD_TOPIC_NAME="test-val"
ENV NEXTCLOUD_VOLUME="test-val"
ENV SERVICES_DB_KEY="test-val"
ENV OBJECTS_DB_KEY="test-val"

RUN rm -rf /user/local/switchboard
COPY ./ /usr/local/switchboard
WORKDIR /usr/local/switchboard

# $USER is needed for tests
RUN echo "build switchboard..." && \
    export USER=$(whoami) && \
    mvn clean install

FROM tomcat:9-jre11-slim

ARG SWITCHBOARD_TOPIC_NAME
ARG NEXTCLOUD_TOPIC_NAME
ARG NEXTCLOUD_VOLUME
ARG DEPLOYMENT_VOLUME
ARG KAFKA_PORT
ARG TEST_USER
ARG TEST_PASSWORD
ARG USER_TO_LOCK_WITH
ARG USER_TO_UNLOCK_WITH
ARG APP_KEY_OBJECTS
ARG APP_KEY_SERVICES

COPY --from=builder /usr/local/switchboard/target/switchboard-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/switchboard.war

# Add tomcat filters to allow CORS:
COPY ./docker/web.xml /usr/local/tomcat/conf/web.xml

# Create system user for locking nextcloud files:
RUN useradd --create-home --shell /bin/bash locker

# Add vre test user as static json file:
COPY ./set-test-user.sh /usr/local/switchboard/set-test-user.sh
RUN echo "test user: $TEST_USER"
RUN /usr/local/switchboard/set-test-user.sh

CMD export CATALINA_OPTS="-Xmx700m --add-exports java.base/jdk.internal.ref=ALL-UNNAMED" && \
    /usr/local/tomcat/bin/catalina.sh run
