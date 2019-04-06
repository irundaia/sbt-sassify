FROM ubuntu:14.04
MAINTAINER "Han van Venrooij" <clerris@gmail.com>

# Install required tools
RUN set -x \
    && apt-get update \
    && apt-get install -y git build-essential

# Run configuration
WORKDIR /sass
CMD ["/Compile/make.sh"]
