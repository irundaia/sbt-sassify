#!/bin/bash

cd /sass

rm -r resources/win32-x86-64
mkdir -p resources/win32-x86-64

# *** Build libsass

make -C native-src clean
cd native-src
git reset --hard # hard reset
git clean -xdf # hard clean
cd ..

# *** Prepare makefile to use static bindings
# static libgcc and libstdc++
sed -i 's/LDFLAGS  += -std=gnu++0x/LDFLAGS  += -std=gnu++0x -static-libgcc -static-libstdc++/' native-src/Makefile
# static windows bindings
sed -i 's/ -Wl,--subsystem,windows/ -static -Wl,--subsystem,windows/' native-src/Makefile

MAKE=mingw32 \
CC=x86_64-w64-mingw32-gcc \
CXX=x86_64-w64-mingw32-g++ \
WINDRES=x86_64-w64-mingw32-windres \
BUILD=static \
    make -C native-src -j8 lib/libsass.dll || exit 1
cp native-src/lib/libsass.dll resources/win32-x86-64/sass.dll || exit 1

git reset --hard # hard reset
git clean -xdf # hard clean