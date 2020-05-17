#! /usr/bin/env bash

docker build -t icestreamer .

docker images

docker run -t icestreamer

