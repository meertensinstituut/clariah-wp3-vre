#!/bin/bash
VRE_CONTAINERS=$(docker ps -a | command grep vre | awk '{print $1}')
echo "stop"
docker stop $VRE_CONTAINERS
echo "rm"
docker rm $VRE_CONTAINERS

docker ps -a
