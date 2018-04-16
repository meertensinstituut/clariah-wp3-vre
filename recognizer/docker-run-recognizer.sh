#!/bin/bash

mvn install && \
sleep 20  && \
java -jar --add-modules java.xml.bind ./target/recognizer-0.1-SNAPSHOT.jar consume
