# Compiling native binaries
This documents explains how to compile the native libraries using.

## Using docker
The `bin/docker/buildLinux` and `bin/docker/buildWin32` directories both contain a Docker file capable of preparing docker images ready for compiling `libsass`. For installation instructions, please refer to the official [Docker documentation](https://docs.docker.com/).

To prepare the images, you should run the commands below (this might take a while). Note that the path to the git repository still has to be filled in.
- `docker build --tag="libsass/windows:1" --file "<path/to/sbt-sassify's/git>/bin/docker/buildWin32/Dockerfile"`
- `docker build --tag="libsass/linux:1" --file "<path/to/sbt-sassify's/git>/bin/docker/buildLinux/Dockerfile"`

Now that the images are ready, compiling the libraries is as easy as running the commands below. Note that the path to the git repository still has to be filled in.

- 32-bit Windows
`docker run --rm -v <path/to/sbt-sassify's/git>/bin/docker/buildWin32/x86/:/Compile -v <path/to/sbt-sassify's/git>:/sass libsass/windows:1`
- 64-bit Windows
`docker run --rm -v <path/to/sbt-sassify's/git>/bin/docker/buildWin32/x86-64/:/Compile -v <path/to/sbt-sassify's/git>:/sass libsass/windows:1`
- 64-bit Linux
`docker run --rm -v <path/to/sbt-sassify's/git>/bin/docker/buildLinux/x86-64/:/Compile -v <path/to/sbt-sassify's/git>:/sass libsass/linux:1`

After running the docker commands, the binaries have been updated.

## Manual compilation
- 32-bit Linux:
No 32-bit docker images exist. As such the 32-bit binary needs to be compiled manually on a 32-bit machine. To compile the library use the terminal to navigate to the `native-src` directory and run the `make shared` command. When this is done, copy the `libsass.so` binary from the `native-src/lib` directory to the `resources/linux-x86` directory.

- OS X:
No OS X docker images exist. As such this binary needs to be compiled manually on a OS X machine. To compile the library use the terminal to navigate to the `native-src` directory and run the `make shared` command. Then to link the `dylib`, run

`c++ -shared -Wall -O2 -Wl,-undefined,error -std=c++0x -stdlib=libc++ -ldl -fPIC -fPIC -o lib/libsass.dylib src/*.o -lm -lstdc++ -ldl`

When this is done, copy the `libsass.dylib` binary from the `native-src/lib` directory to the `resources/darwin` directory.
