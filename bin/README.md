# Compiling native binaries
This documents explains how to compile the native libraries using.

## Using docker
The `bin/docker/buildLinux` and `bin/docker/buildWin32` directories both contain a Docker file capable of preparing docker images ready for compiling `libsass`. For installation instructions, please refer to the official [Docker documentation](https://docs.docker.com/).

To prepare the images, you should run the commands below (this might take a while). Note that the path to the git repository still has to be filled in.
- `docker build --tag="libsass/windows:1" "<path/to/sbt-sassify's/git>/bin/docker/buildWin32"`
- `docker build --tag="libsass/linux:1" "<path/to/sbt-sassify's/git>/bin/docker/buildLinux/x86-64"`
- `docker build --tag="libsass/linux32:1" "<path/to/sbt-sassify's/git>/bin/docker/buildLinux/x86"`
- `docker build --tag="libsass/darwin:1" "<path/to/sbt-sassify's/git>/bin/docker/buildDarwin"`

Now that the images are ready, compiling the libraries is as easy as running the commands below. Note that the path to the git repository still has to be filled in.

- 32-bit Windows
  `docker run --rm -v <path/to/sbt-sassify's/git>/bin/docker/buildWin32/x86/:/Compile -v <path/to/sbt-sassify's/git>:/sass libsass/windows:1`
- 64-bit Windows
  `docker run --rm -v <path/to/sbt-sassify's/git>/bin/docker/buildWin32/x86-64/:/Compile -v <path/to/sbt-sassify's/git>:/sass libsass/windows:1`
- 32-bit Linux
  `docker run --rm -v <path/to/sbt-sassify's/git>/bin/docker/buildLinux/x86/:/Compile -v <path/to/sbt-sassify's/git>:/sass libsass/linux32:1`
- 64-bit Linux
  `docker run --rm -v <path/to/sbt-sassify's/git>/bin/docker/buildLinux/x86-64/:/Compile -v <path/to/sbt-sassify's/git>:/sass libsass/linux:1`
- Darwin/OS X
  `docker run --rm -v <path/to/sbt-sassify's/git>/bin/docker/buildDarwin:/Compile -v <path/to/sbt-sassify's/git>:/sass libsass/darwin:1`

After running the docker commands, the binaries have been updated. For simplicity reasons, one could also run the `buildAll.sh` file. This would run all docker commands directly. Note that the script should be run from the project's root directory. Note that this script still assumes that the docker containers are available.