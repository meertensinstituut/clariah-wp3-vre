FROM knawhuc/clariah-wp3-vre-builder as builder

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

FROM openjdk:11-jre-slim-stretch
COPY --from=builder /usr/local/recognizer/target/recognizer-0.1-SNAPSHOT-jar-with-dependencies.jar /usr/local/recognizer/recognizer.jar

CMD java -jar /usr/local/recognizer/recognizer.jar
