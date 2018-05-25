#!/bin/bash

# use environment variables from root:
cp ../.env ./.env

# Variables should start with 'REACT_APP' for them to be passed to react app:
REACT_APP_KEY_GET_OBJECTS="${APP_KEY_GET_OBJECTS}"

echo "start UI..."
npm start
open http://localhost:3000/
