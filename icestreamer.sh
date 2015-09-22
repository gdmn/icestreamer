#! /usr/bin/env bash

pushd "`dirname $0`" > /dev/null
SCRIPTLOCATION="`pwd`"
popd > /dev/null

java -Xmx200M -jar "${SCRIPTLOCATION}/target/icestreamer-1.0-SNAPSHOT.jar" --port 6680 $*
