#!/usr/bin/env bash
mkdir ./parent-pom/
cp ../parent-pom/pom.xml ./parent-pom/

mv Dockerfile Dockerfile.original
mv Dockerfile.localParentPom Dockerfile

docker-compose build recognizer

mv Dockerfile Dockerfile.localParentPom
mv Dockerfile.original Dockerfile

rm -rf ./parent-pom/
