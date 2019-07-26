#!/usr/bin/env bash
curl -X POST -H "Content-Type: application/json" -d "{ "product":"latte", "name":"clement" }" http://localhost:9500
#http POST :8080/http "{ "product":"latte", "name":"clement" }"
#http POST :8080/http "{ "product":"espresso", "name":"neo" }"
#http POST :8080/http "{ "product":"mocho", "name":"flore" }"
