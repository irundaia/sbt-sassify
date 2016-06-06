#!/bin/bash

cd /sass

rm -r src/main/resources/linux-x86
mkdir -p src/main/resources/linux-x86

# *** Build libsass
make -C src/native clean
cd src/native
git reset --hard # hard reset
git clean -xdf # hard clean
cd ../..

BUILD="shared" \
  make -C src/native -j8 || exit 1

# *** Copy to target location
cp src/native/lib/libsass.so src/main/resources/linux-x86/libsass.so || exit 1

# *** Cleanup
cd /sass/src/native
git clean -xdf