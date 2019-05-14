FROM knawhuc/clariah-wp3-vre-builder as builder

ARG SWITCHBOARD_TOPIC_NAME
ARG NEXTCLOUD_TOPIC_NAME
ARG KAFKA_PORT
ARG NEXTCLOUD_VOLUME
ARG DEPLOYMENT_VOLUME
ARG TEST_USER
ARG TEST_PASSWORD
ARG USER_TO_LOCK_WITH
ARG USER_TO_UNLOCK_WITH
ARG APP_KEY_OBJECTS
ARG APP_KEY_SERVICES

RUN rm -rf /user/local/switchboard
COPY ./ /usr/local/switchboard
WORKDIR /usr/local/switchboard

RUN echo "add test user..." && \
    chmod +x /usr/local/switchboard/set-test-user.sh && \
    /usr/local/switchboard/set-test-user.sh

RUN echo "remove work dirs in tmp deployment volume [${DEPLOYMENT_VOLUME}]..." && \
    rm -rf "${DEPLOYMENT_VOLUME}/*"

# $USER is needed for tests
RUN echo "build switchboard..." && \
    export USER=$(whoami) && \
    mvn clean install

FROM tomcat:9-jre11-slim

COPY --from=builder /usr/local/switchboard/target/switchboard-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/switchboard.war

# Add tomcat filters to allow CORS:
COPY ./docker/web.xml /usr/local/tomcat/conf/web.xml

# Create user for locking nextcloud files:
RUN useradd --create-home --shell /bin/bash locker


# tail to keep docker running when redeploying jar:
CMD export CATALINA_OPTS="-Xmx700m --add-exports java.base/jdk.internal.ref=ALL-UNNAMED" && \
    /usr/local/tomcat/bin/catalina.sh run && \
    tail -f /dev/null
