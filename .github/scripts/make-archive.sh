#!/usr/bin/env bash
set -e

USE_DOCKER=false
DOCKER_IMAGE=""
COMMAND_PREFIX=()

if [ "$WINDOWS_CROSS" == "true" ]; then
  USE_DOCKER=true
  DOCKER_IMAGE="fedora-image"
elif [ "$(expr substr $(uname -s) 1 5 2>/dev/null)" == "Linux" ]; then
  USE_DOCKER=true
  DOCKER_IMAGE="alpine-image"
fi

if [ "$USE_DOCKER" == "true" ]; then
  COMMAND_PREFIX=(docker run -v "$(pwd):/workdir" -w /workdir "$DOCKER_IMAGE")
fi

# link loops?
"${COMMAND_PREFIX[@]}" rm -f \
  build/qemu-bundle/usr/local/lib/libslirp.dylib \
  build/qemu-bundle/usr/local/lib/x86_64-linux-gnu/libslirp.so.0 \
  build/qemu-bundle/usr/local/lib/x86_64-linux-gnu/libslirp.so \
  build/qemu-bundle/usr/local/lib/aarch64-linux-gnu/libslirp.so.0 \
  build/qemu-bundle/usr/local/lib/aarch64-linux-gnu/libslirp.so \
  build/qemu-bundle/usr/local/lib/libslirp.so.0 \
  build/qemu-bundle/usr/local/lib/libslirp.so

"${COMMAND_PREFIX[@]}" cp -RL build/qemu-bundle "build/$1"
"${COMMAND_PREFIX[@]}" mv build/build.ninja "build/$1/"

if [ "$USE_DOCKER" == "true" ]; then
  docker run -v "$(pwd):/workdir" -w "/workdir/build/$1" "$DOCKER_IMAGE" tar -zcvf "../../$1.tar.gz" .
  sudo chown -R "$(id -un):$(id -gn)" "$1.tar.gz"
else
  cd "build/$1"
  tar -zcvf "../../$1.tar.gz" *
  cd -
fi
