#!/bin/bash

echo 'start integration tests...'
mvn clean test -Dtest="*,!nl.knaw.meertens.clariah.vre.stress.**"