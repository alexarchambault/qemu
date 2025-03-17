FROM fedora:41

RUN dnf update --refresh -y && \
  dnf install -y --setopt=tsflags=nodocs \
    mingw64-binutils \
    mingw64-gcc \
    mingw64-gettext-static \
    mingw64-glib2-static \
    mingw64-libffi-static \
    mingw64-pcre2-static \
    mingw64-win-iconv-static \
    mingw64-winpthreads-static \
    mingw64-zlib-static \
    meson \
    git \
    gcc \
    diffutils \
    patch && \
  dnf clean all && \
  rm -rf /var/cache/dnf /var/lib/dnf

COPY x86_64-w64-mingw32.txt /

# freedesktop is currently down
# https://gitlab.freedesktop.org/slirp/libslirp/-/archive/v4.8.0/libslirp-v4.8.0.tar.gz
COPY libslirp-v4.8.0.tar.gz /
COPY libslirp.patch /

RUN tar -zxf /libslirp-v4.8.0.tar.gz && \
  cd libslirp-v4.8.0 && \
  patch -p1 < /libslirp.patch && \
  meson setup build --prefix=/usr/x86_64-w64-mingw32/sys-root/mingw/ --cross-file /x86_64-w64-mingw32.txt -Ddefault_library=both && \
  meson compile -C build && \
  meson install --no-rebuild -C build && \
  rm -rf libslirp-v4.8.0 && \
  cd /usr/x86_64-w64-mingw32/sys-root/mingw/include/slirp && \
  mv libslirp.h libslirp.h.bak && \
  cat libslirp.h.bak | sed 's/__declspec(dllexport)//g' | sed 's/__declspec(dllimport)//g' > libslirp.h && \
  rm -f libslirp.h.bak
