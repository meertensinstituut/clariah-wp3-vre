#!/bin/bash
# get vre environment variables:
cp ../.env ./.env

docker-compose -p vre up --build