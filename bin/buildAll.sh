#!/usr/bin/env bash

docker run --rm -v $(pwd)/bin/docker/buildWin32/x86/:/Compile -v $(pwd):/sass libsass/windows:1
docker run --rm -v $(pwd)/bin/docker/buildWin32/x86-64/:/Compile -v $(pwd):/sass libsass/windows:1
docker run --rm -v $(pwd)/bin/docker/buildLinux/x86/:/Compile -v $(pwd):/sass libsass/linux32:1
docker run --rm -v $(pwd)/bin/docker/buildLinux/x86-64/:/Compile -v $(pwd):/sass libsass/linux:1
docker run --rm -v $(pwd)/bin/docker/buildDarwin:/Compile -v $(pwd):/sass libsass/darwin:1