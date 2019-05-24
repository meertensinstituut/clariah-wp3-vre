#!/usr/bin/env bash
curl \
  -X 'POST' 'http://localhost:8090/ksql' \
  -H 'Content-Type: application/vnd.ksql.v1+json; charset=utf-8' \
  --data-binary '@query.json' \
  | jq

