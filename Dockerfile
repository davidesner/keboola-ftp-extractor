# instead of maven:3.5.0-jdk-7-alpine, contained old java version
FROM maven:3.5.2-jdk-7-slim  

MAINTAINER David Esner <esnerda@gmail.com>

COPY . /code/
# set switch that enables correct JVM memory allocation in containers
ENV JAVA_OPTS='-Xmx512m -Xms512m  -Djdk.tls.client.protocols="TLSv1,TLSv1.1,TLSv1.2" -Djdk.tls.useExtendedMasterSecret=false'
ENV MAVEN_OPTS='-Xmx512m -Xms512m -Djdk.tls.client.protocols="TLSv1,TLSv1.1,TLSv1.2" -Djdk.tls.useExtendedMasterSecret=false'

WORKDIR /code/
RUN mvn compile

ENTRYPOINT mvn -q exec:java -Dexec.args=/data  