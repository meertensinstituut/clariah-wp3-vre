#!/bin/bash

WD=`curl -X POST http://localhost:9010/switchboard/rest/exec/UCTO -H "Content-Type: application/json" -d '{ "params": [ { "name": "untokinput", "type": "file", "value": '$1', "params": [ { "language": "nld", "author": "F. Emmer" } ] } ] }'`
echo $WD

WD=`echo "$WD" | jq -r '.workDir'`
echo $WD

sleep 5

curl http://localhost:9010/switchboard/rest/exec/task/${WD}