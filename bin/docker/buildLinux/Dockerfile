FROM centos:5
MAINTAINER "Han van Venrooij" <irundaia@gmail.com>

RUN set -x \
    && yum install -y wget

# Install build tools
RUN set -x \
    && cd /etc/yum.repos.d \
    && wget http://people.centos.org/tru/devtools-1.1/devtools-1.1.repo -O devtools-1.1.repo \
    && yum install -y devtoolset-1.1 devtoolset-1.1-gcc devtoolset-1.1-gcc-c++ glibc glibc-devel \
    && scl enable devtoolset-1.1 bash

# Set PATH, because "scl enable" does not have any effects to "docker build"
ENV PATH $PATH:/opt/centos/devtoolset-1.1/root/usr/bin

# Install git
RUN set -x \
    && cd / \
    && wget http://packages.sw.be/rpmforge-release/rpmforge-release-0.5.2-2.el5.rf.i386.rpm \
    && rpm --import http://apt.sw.be/RPM-GPG-KEY.dag.txt \
    && rpm -K rpmforge-release-0.5.2-2.el5.rf.i386.rpm \
    && rpm -i rpmforge-release-0.5.2-2.el5.rf.i386.rpm \
    && yum install -y git \
    && rm rpmforge-release-0.5.2-2.el5.rf.i386.rpm

# Run configuration
WORKDIR /sass
CMD ["/Compile/make.sh"]