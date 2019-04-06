# Compiling native binaries
This documents explains how to compile the native libraries using.

## Using docker
The `bin/docker/buildLinux` and `bin/docker/buildWin32` directories both contain a Docker file capable of preparing docker images ready for compiling `libsass`. For installation instructions, please refer to the official [Docker documentation](https://docs.docker.com/).

To prepare the images, you should run the commands below (this might take a while). Note that the path to the git repository still has to be filled in.
- `docker build --tag="libsass/windows:1" "<path/to/sbt-sassify's/git>/bin/docker/buildWin32"`
- `docker build --tag="libsass/linux:1" "<path/to/sbt-sassify's/git>/bin/docker/buildLinux/x86-64"`
- `docker build --tag="libsass/darwin:1" "<path/to/sbt-sassify's/git>/bin/docker/buildDarwin"`

Now that the images are ready, compiling the libraries is as easy as running `./bin/buildAll.sh`.
