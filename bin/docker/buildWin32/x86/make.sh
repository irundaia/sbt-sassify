#!/bin/bash

cd /sass

rm -r src/main/resources/win32-x86
mkdir -p src/main/resources/win32-x86

# *** Build libsass

make -C src/native clean
cd src/native
git reset --hard # hard reset
git clean -xdf # hard clean
cd ../..

# *** Prepare makefile to use static bindings
# static libgcc and libstdc++
sed -i 's/LDFLAGS  += -std=gnu++0x/LDFLAGS  += -std=gnu++0x -static-libgcc -static-libstdc++/' src/native/Makefile
# static windows bindings
sed -i 's/ -Wl,--subsystem,windows/ -static -Wl,--subsystem,windows/' src/native/Makefile

MAKE=mingw32 \
CC=i686-w64-mingw32-gcc \
CXX=i686-w64-mingw32-g++ \
WINDRES=i686-w64-mingw32-windres \
BUILD=static \
    make -C src/native -j8 lib/libsass.dll || exit 1
cp src/native/lib/libsass.dll src/main/resources/win32-x86/sass.dll || exit 1

cd src/native
git reset --hard # hard reset
git clean -xdf # hard clean