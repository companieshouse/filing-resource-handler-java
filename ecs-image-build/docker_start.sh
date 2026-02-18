#!/bin/bash

PORT=8081

exec java -jar -XX:MaxRAMPercentage=80 -Dserver.port="${PORT}" "filing-resource-handler-java.jar"