FROM daald/ubuntu32:vivid
MAINTAINER "Han van Venrooij" <clerris@gmail.com>

RUN set -x \
    && apt-get -y install git build-essential

# Run configuration
WORKDIR /sass
CMD ["/Compile/make.sh"]