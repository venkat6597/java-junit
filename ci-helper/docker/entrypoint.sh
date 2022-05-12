#!/bin/sh
java -javaagent:/newrelic/newrelic.jar -Dnewrelic.environment=$NEWRELIC_PROFILE -jar app.jar