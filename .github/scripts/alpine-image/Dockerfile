FROM alpine:3.21.0

# initially based on https://github.com/ziglang/qemu-static/blob/3a689be4a02379c6d219b08334109b69957067e6/Dockerfile

RUN apk update && \
  apk upgrade && \
  apk add \
    make \
    samurai \
    perl \
    python3 \
    gcc \
    libc-dev \
    pkgconf \
    linux-headers \
    glib-dev glib-static \
    zlib-dev zlib-static \
    util-linux-static \
    flex \
    bison \
    ninja \
    gmp-dev \
    bash \
    xz \
    git \
    patch \
    curl \
    meson \
    build-base && \
  rm -rf /var/cache/apk/*

# freedesktop is currently down
# https://gitlab.freedesktop.org/slirp/libslirp/-/archive/v4.8.0/libslirp-v4.8.0.tar.gz
COPY libslirp-v4.8.0.tar.gz /
COPY libslirp.patch /

RUN tar -zxf /libslirp-v4.8.0.tar.gz && \
  cd libslirp-v4.8.0 && \
  patch -p1 < /libslirp.patch && \
  meson setup build --prefix=/usr -Ddefault_library=both && \
  meson compile -C build && \
  meson install --no-rebuild -C build && \
  cd .. && \
  rm -rf libslirp-v4.8.0
