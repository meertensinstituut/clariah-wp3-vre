# Create basic docker image with empty maven repo:
#FROM maven:3.5.4-jdk-11-slim
#RUN mkdir -p /root/.m2/repository
#WORKDIR /usr/local/tagger

FROM knawhuc/clariah-wp3-vre-recognizer AS old

FROM maven:3.5.4-jdk-11-slim
COPY --from=old /root/.m2/repository /root/.m2/repository

RUN rm -rf /user/local/recognizer
COPY ./ /usr/local/recognizer
WORKDIR /usr/local/recognizer

ARG KAFKA_BROKER
ARG FITS_FILES_ROOT
ARG KAFKA_PORT
ARG NEXTCLOUD_TOPIC_NAME
ARG NEXTCLOUD_GROUP_NAME
ARG RECOGNIZER_TOPIC_NAME
ARG RECOGNIZER_GROUP_NAME
ARG USER_OBJECTS_EMAIL
ARG USER_OBJECTS_PASSWORD
ARG APP_KEY_OBJECTS

RUN mvn clean install

CMD java -jar ./target/recognizer-0.1-SNAPSHOT.jar
