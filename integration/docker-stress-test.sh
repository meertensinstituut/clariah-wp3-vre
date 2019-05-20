#!/usr/bin/env bash
echo 'start stress tests...'
mvn clean test -Dtest="*,!nl.knaw.meertens.clariah.vre.integration.**"