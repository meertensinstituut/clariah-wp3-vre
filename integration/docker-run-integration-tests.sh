#!/bin/bash

echo 'start integration tests...'

if [ "$1" = "debug" ]; then
  # Change maven remote debug port to '*:8085':
  sed -i -e 's/8000/*:8085/g' /usr/share/maven/bin/mvnDebug
  mvnDebug -DfailIfNoTests=false -Dtest=FoliaEditorTest test
#  mvnDebug test
else
  mvn clean -DfailIfNoTests=false -Dtest=FoliaEditorTest test
#  mvn clean test
fi
