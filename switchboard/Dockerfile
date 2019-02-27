# Create basic docker image with empty maven repo:
#FROM maven:3.5.4-jdk-11-slim
#RUN mkdir -p /root/.m2/repository
#WORKDIR /usr/local/switchboard

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

FROM knawhuc/clariah-wp3-vre-switchboard AS old

FROM knawhuc/clariah-wp3-vre-switchboard-builder
COPY --from=old /root/.m2/repository /root/.m2/repository

RUN ls -R /root/.m2/repository
RUN mvn help:evaluate -Dexpression=settings.localRepository | grep -v '\[INFO\]'

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

RUN echo "deploy switchboard.war..." && \
    cp /usr/local/switchboard/target/switchboard-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/switchboard.war

# tail to keep docker running when redeploying jar:
CMD export CATALINA_OPTS="-Xmx700m --add-exports java.base/jdk.internal.ref=ALL-UNNAMED" && \
    /usr/local/tomcat/bin/catalina.sh run && \
    tail -f /dev/null
