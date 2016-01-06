#!/bin/bash

cd /sass

rm -r resources/linux-x86-64
mkdir -p resources/linux-x86-64

# *** Build libsass
make -C native-src clean
cd native-src
git reset --hard # hard reset
git clean -xdf # hard clean
cd ..
BUILD="shared" make -C native-src -j8 || exit 1

# *** Copy to target location
cp native-src/lib/libsass.so resources/linux-x86-64/libsass.so || exit 1

# *** Cleanup
cd /sass/native-src/
git clean -xdf