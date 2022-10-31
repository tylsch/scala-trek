#!/bin/sh
curl --header "Content-type: application/json" \
         --request POST \
         --data '{"make":"Fender","model":"Stratocaster"}' \
         http://localhost:9000/api/guitar

curl --header "Content-type: application/json" \
         --request POST \
         --data '{"make":"Taylor","model":"914"}' \
         http://localhost:9000/api/guitar