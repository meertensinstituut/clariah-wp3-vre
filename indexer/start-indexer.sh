#!/bin/bash
# use environment variables from root:
cp ../.env ./.env

docker-compose -p vre up
