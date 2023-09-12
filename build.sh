#!/bin/bash

echo "Compiling the application with a docker build"

mvn clean package
docker build -t thomasg/archi_cloud_td1_application .