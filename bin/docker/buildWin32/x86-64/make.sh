#!/bin/bash

cd /sass

rm -r src/main/resources/win32-x86-64
mkdir -p src/main/resources/win32-x86-64

# *** Build libsass

make -C src/native clean
cd src/native
git reset --hard # hard reset
git clean -xdf # hard clean
cd ../..

MAKE=mingw32 \
CC=x86_64-w64-mingw32-gcc \
CXX=x86_64-w64-mingw32-g++ \
WINDRES=x86_64-w64-mingw32-windres \
LDFLAGS=" -Wall -static-libgcc -static-libstdc++ " \
CXXFLAGS=" -static -Wall " \
BUILD=static \
    make -C src/native -j8 lib/libsass.dll || exit 1
cp src/native/lib/libsass.dll src/main/resources/win32-x86-64/sass.dll || exit 1

cd src/native
git reset --hard # hard reset
git clean -xdf # hard clean