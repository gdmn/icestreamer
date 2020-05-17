#! /usr/bin/env bash

docker build -t icestreamer .

docker images

#docker-slim build icestreamer
#docker run -p 6680:6680 -t icestreamer.slim

#docker run -p 6680:6680 -t icestreamer
docker run -p 6680:6680 -v /home/dmn/Muzyka:/music:ro -t icestreamer
