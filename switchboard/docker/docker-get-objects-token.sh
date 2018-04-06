#!/bin/bash
export OBJECTS_TOKEN=$( \
  curl -s -k -3 \
    -X POST "dreamfactory:80/api/v2/user/session" \
    -d '{ "email" : "'${USER_OBJECTS_EMAIL}'", "password" : "'${USER_OBJECTS_PASSWORD}'" }' \
    -H "Content-Type: application/json" \
  | jq -r .session_token \
)
echo "OBJECTS_TOKEN: ${OBJECTS_TOKEN}"