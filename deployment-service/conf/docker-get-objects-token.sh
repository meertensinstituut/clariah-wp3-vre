#!/bin/bash
export SERVICES_TOKEN=$( \
  curl -s -k -3 \
    -X POST "http://dreamfactory:80/api/v2/user/session" \
    -d '{ "email" : "'${USER_SERVICES_EMAIL}'", "password" : "'${USER_SERVICES_PASSWORD}'" }' \
    -H "Content-Type: application/json" \
  | jq -r .session_token \
)
echo "OK"
echo "SERVICES_TOKEN: ${SERVICES_TOKEN}"
tail -f /dev/null