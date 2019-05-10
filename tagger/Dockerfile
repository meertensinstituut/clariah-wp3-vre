FROM knawhuc/clariah-wp3-vre-builder as builder

RUN rm -rf /user/local/tagger
COPY ./ /usr/local/tagger
WORKDIR /usr/local/tagger

ARG KAFKA_PORT
ARG RECOGNIZER_TOPIC_NAME
ARG RECOGNIZER_GROUP_NAME
ARG TAGGER_TOPIC_NAME
ARG TAGGER_GROUP_NAME
ARG APP_KEY_OBJECTS
ARG TEST_USER
ARG TEST_PASSWORD

RUN mvn clean install

FROM openjdk:11-jre-slim-stretch
COPY --from=builder /usr/local/tagger/target/tagger-0.1-SNAPSHOT-jar-with-dependencies.jar /usr/local/tagger/tagger.jar

CMD java -jar /usr/local/tagger/tagger.jar
