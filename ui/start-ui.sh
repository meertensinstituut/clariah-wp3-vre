#!/bin/bash

# use environment variables from root:
cp ../.env ./.env

# variables that start with 'REACT_APP' are passed to react app:
REACT_APP_KEY_GET_OBJECTS="${APP_KEY_GET_OBJECTS}"

echo "start UI..."
npm start
open http://localhost:3000/
