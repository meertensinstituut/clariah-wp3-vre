FROM maven:3.5.4-jdk-11-slim

RUN echo 'add mvnDebug to path...'
ENV PATH="/usr/share/maven/bin:${PATH}"
RUN apt-get update && \
    apt-get install -y postgresql-client

