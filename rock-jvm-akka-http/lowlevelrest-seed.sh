#!/bin/sh
curl --header "Content-type: application/json" \
         --request POST \
         --data '{"make":"Fender","model":"Stratocaster","quantity":0}' \
         http://localhost:9000/api/guitar

curl --header "Content-type: application/json" \
         --request POST \
         --data '{"make":"Gibson","model":"Les Paul","quantity":19}' \
         http://localhost:9000/api/guitar

curl --header "Content-type: application/json" \
         --request POST \
         --data '{"make":"Martin","model":"LX1","quantity":10}' \
         http://localhost:9000/api/guitar