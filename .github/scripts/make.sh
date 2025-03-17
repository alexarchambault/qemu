#!/usr/bin/env bash
set -e

USE_DOCKER=false
DOCKER_IMAGE=""

if [ "$WINDOWS_CROSS" == "true" ]; then
  USE_DOCKER=true
  DOCKER_IMAGE="fedora-image"
elif [ "$(expr substr $(uname -s) 1 5 2>/dev/null)" == "Linux" ]; then
  USE_DOCKER=true
  DOCKER_IMAGE="alpine-image"
fi

COMMAND=(make -j2)

if [ "$USE_DOCKER" == "true" ]; then
  COMMAND=(docker run -v "$(pwd):/workdir" -w /workdir "$DOCKER_IMAGE" "${COMMAND[@]}")
fi

exec "${COMMAND[@]}"
