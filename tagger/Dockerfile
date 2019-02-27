# Create basic docker image with empty maven repo:
#FROM maven:3.5.4-jdk-11-slim
#RUN mkdir -p /root/.m2/repository
#WORKDIR /usr/local/tagger

FROM knawhuc/clariah-wp3-vre-tagger AS old

FROM maven:3.5.4-jdk-11-slim
COPY --from=old /root/.m2/repository /root/.m2/repository

RUN ls -R /root/.m2/repository
RUN mvn help:evaluate -Dexpression=settings.localRepository | grep -v '\[INFO\]'

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
CMD java -jar ./target/tagger-1.0-SNAPSHOT.jar
