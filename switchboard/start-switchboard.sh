#!/bin/bash
# get vre environment variables:
cp ../.env ./.env

docker-compose pull && docker-compose -p vre up -d