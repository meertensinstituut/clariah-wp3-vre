#!/bin/bash

mvn clean install && \
sleep 20  && \
java -jar ./target/recognizer-0.1-SNAPSHOT.jar
