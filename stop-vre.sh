#!/bin/bash

VRE_CONTAINERS=$(docker ps -a | command grep vre | awk '{print $1}')

echo "stop containers"
docker stop $VRE_CONTAINERS
echo "rm containers"
docker rm $VRE_CONTAINERS

docker ps -a
