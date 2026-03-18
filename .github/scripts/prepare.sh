#!/usr/bin/env bash
set -e

if [ "$WINDOWS_CROSS" == "true" ]; then
  cd .github/scripts/fedora-image
  docker build -t fedora-image .
  cd -
elif [ "$(expr substr $(uname -s) 1 5 2>/dev/null)" == "Linux" ]; then
  cd .github/scripts/alpine-image
  docker build -t alpine-image .
  cd -
elif [ "$(uname)" == "Darwin" ]; then
  brew install ninja libslirp
  pip3 install --break-system-packages distlib
elif [[ `uname | grep -E 'CYG*|MSYS*|MING*|UCRT*|ClANG*|GIT*'` ]]; then
  pip install meson==1.5.0 pycotap==1.3.1
  curl -fLO https://repo.msys2.org/mingw/mingw64/mingw-w64-x86_64-libslirp-4.8.0-2-any.pkg.tar.zst
  pacman --noconfirm -U mingw-w64-x86_64-libslirp-4.8.0-2-any.pkg.tar.zst
  echo cd "$MINGW_PREFIX/include"
  cd "$MINGW_PREFIX/include/slirp"
  ls
  if ! test -e libslirp.h; then
    find /usr -type f -name libslirp.h
    find /ming* -type f -name libslirp.h
  fi
  mv libslirp.h libslirp.h.bak
  cat libslirp.h.bak | sed 's/__declspec(dllexport)//g' | sed 's/__declspec(dllimport)//g' > libslirp.h
  rm -f libslirp.h.bak
  cd -
else
  echo "Unrecognized OS: $(uname)" 1>&2
  exit 1
fi
