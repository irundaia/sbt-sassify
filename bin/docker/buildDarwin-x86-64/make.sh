#!/usr/bin/env bash
cd /sass

rm -r src/main/resources/darwin
rm -r src/main/resources/darwin-x86-64
mkdir -p src/main/resources/darwin
mkdir -p src/main/resources/darwin-x86-64

# *** Build libsass
make -C src/native clean
cd src/native
git reset --hard # hard reset
git clean -xdf # hard clean
cd ../..

CFLAGS="-Wall -arch x86_64 -stdlib=libc++" \
CXXFLAGS="-Wall -arch x86_64 -stdlib=libc++" \
LDFLAGS="-stdlib=libc++" \
CC=/opt/osxcross/target/bin/x86_64-apple-darwin12-clang \
CXX=/opt/osxcross/target/bin/x86_64-apple-darwin12-clang++-libc++ \
BUILD=shared \
    make -C src/native -j8 || exit 1

# *** Copy to target location. Note that the makefile from libsass will output a .so file. Renaming it to .dylib is good enough.
cp src/native/lib/libsass.so src/main/resources/darwin/libsass.dylib || exit 1
cp src/native/lib/libsass.so src/main/resources/darwin-x86-64/libsass.dylib || exit 1

# *** Cleanup
cd /sass/src/native
git clean -xdf