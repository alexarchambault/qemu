#!/usr/bin/env bash
set -e

OS_ARGS=()
USE_DOCKER=false
DOCKER_IMAGE=""
MOSTLY_STATIC=false
SCALA_CLI=(.github/scripts/scala-cli)
BINARIES=()

SHARED_ARGS=(
  --enable-slirp
  --enable-tools
  --target-list=x86_64-softmmu,aarch64-softmmu
  --disable-bsd-user
  --disable-zstd
  --disable-libusb
  --disable-bsd-user
  --disable-curl
  --disable-libssh
  --disable-png
  --disable-vnc-jpeg
  --disable-gnutls
  --disable-pixman
  --disable-bzip2
  --disable-auth-pam
  --disable-vnc-sasl
  --disable-cocoa
  --disable-curses
  --disable-gcrypt
  --disable-plugins
  --disable-tpm
  --disable-debug-info
)

WINDOWS_OS_ARGS=(
  "--enable-whpx"
  "--extra-cflags=-fno-leading-underscore"
  "--extra-cflags=-DGLIB_STATIC_COMPILATION"
  "--extra-cflags=-DGOBJECT_STATIC_COMPILATION"
  "--extra-cflags=-DGIO_STATIC_COMPILATION"
  "--extra-cflags=-DGMODULE_STATIC_COMPILATION"
  "--extra-cflags=-DGI_STATIC_COMPILATION"
)

WINDOWS_BINARIES=(
  qemu-system-aarch64.exe
  qemu-system-x86_64.exe
  qemu-img.exe
  qemu-io.exe
  qemu-nbd.exe
  qemu-edid.exe
  storage-daemon/qemu-storage-daemon.exe
)

if [ "$WINDOWS_CROSS" == "true" ]; then
  OS_ARGS+=("${WINDOWS_OS_ARGS[@]}" "--cross-prefix=x86_64-w64-mingw32-")
  MOSTLY_STATIC=true
  BINARIES=("--cross" "${WINDOWS_BINARIES[@]}")
  USE_DOCKER=true
  DOCKER_IMAGE="fedora-image"
elif [ "$(expr substr $(uname -s) 1 5 2>/dev/null)" == "Linux" ]; then
  OS_ARGS+=("--enable-kvm" "--enable-virtfs" "--static" "--enable-linux-user")
  USE_DOCKER=true
  DOCKER_IMAGE="alpine-image"
elif [ "$(uname)" == "Darwin" ]; then
  OS_ARGS+=("--enable-hvf" "--enable-virtfs" "--disable-coreaudio")
  MOSTLY_STATIC=true
  BINARIES=(
    qemu-system-aarch64-unsigned
    qemu-system-x86_64-unsigned
    qemu-img
    qemu-io
    qemu-nbd
    qemu-edid
    storage-daemon/qemu-storage-daemon
  )
elif [[ `uname | grep -E 'CYG*|MSYS*|MING*|UCRT*|CLANG*|GIT*'` ]]; then
  OS_ARGS+=("${WINDOWS_OS_ARGS[@]}" "--prefix=C:/qemu")
  MOSTLY_STATIC=true
  BINARIES=("${WINDOWS_BINARIES[@]}")

  curl -fLO https://github.com/VirtusLab/scala-cli/releases/download/v1.7.0/scala-cli-x86_64-pc-win32.zip
  unzip scala-cli-x86_64-pc-win32.zip
  SCALA_CLI=("$(pwd)/scala-cli.exe")
else
  echo "Unrecognized OS: $(uname)" 1>&2
  exit 1
fi

COMMAND=(./configure "${SHARED_ARGS[@]}" "${OS_ARGS[@]}")

COMMAND_PREFIX=()

if [ "$USE_DOCKER" == "true" ]; then
  COMMAND_PREFIX=(docker run -v "$(pwd):/workdir" -w /workdir "$DOCKER_IMAGE")
fi

"${COMMAND_PREFIX[@]}" "${COMMAND[@]}"

if [ "$MOSTLY_STATIC" == "true" ]; then
  "${COMMAND_PREFIX[@]}" "${SCALA_CLI[@]}" --server=false .github/scripts/ProcessBuildNinja.scala -- "${BINARIES[@]}"
fi
