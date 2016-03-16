FROM keboola/base
MAINTAINER David Esner <esnerda@gmail.com>

ENV APP_VERSION 1.1.0

RUN yum -y update && \
	yum -y install \
		epel-release \
		git \
		tar \
		&& \
	yum clean all


RUN yum -y install wget
RUN wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
RUN yum -y install apache-maven

WORKDIR /home

ENV JAVA_HOME /usr/lib/jvm/jre-1.7.0
RUN git clone https://github.com/davidesner/keboola-ftp-extractor.git ./  
RUN mvn compile

ENTRYPOINT mvn exec:java -Dexec.args=/data  