FROM ubuntu:14.04
MAINTAINER "Han van Venrooij" <clerris@gmail.com>

# Install required tools
RUN set -x \
    && apt-get update \
    && apt-get install -y software-properties-common \
    && apt-get install -y git mingw-w64 make

# Run configuration
WORKDIR /sass
CMD ["/Compile/make.sh"]
