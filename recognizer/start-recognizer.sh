#!/bin/bash
# use environment variables from root:
cp ../.env ./.env

mkdir -p /tmp/recognizer
echo "test file" > /tmp/recognizer/test.txt
docker-compose -p vre up
