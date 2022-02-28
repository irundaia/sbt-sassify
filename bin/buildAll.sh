#!/usr/bin/env bash

docker run --rm -v $(pwd)/bin/docker/buildWin32/x86/:/Compile -v $(pwd):/sass libsass/windows:1
docker run --rm -v $(pwd)/bin/docker/buildWin32/x86-64/:/Compile -v $(pwd):/sass libsass/windows:1

./bin/docker/buildLinux/x86/dockcross make -C src/native/ -j8 shared
cp src/native/lib/libsass.so src/main/resources/linux-x86/libsass.so
./bin/docker/buildLinux/x86/dockcross make -C src/native/ clean

docker run --rm -v $(pwd)/bin/docker/buildLinux/x86-64/:/Compile -v $(pwd):/sass libsass/linux:1
docker run --rm -v $(pwd)/bin/docker/buildDarwin-x86-64:/Compile -v $(pwd):/sass libsass/darwin-x86-64:1
