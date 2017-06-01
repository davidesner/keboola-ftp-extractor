FROM maven:3-jdk-7
MAINTAINER David Esner <esnerda@gmail.com>

ENV APP_VERSION 1.2.4
 WORKDIR /home
RUN git clone https://github.com/davidesner/keboola-ftp-extractor.git ./
RUN mvn -q install

ENTRYPOINT java -Xmx512m -Xms512m -jar target/esnerda.keboola.ex.ftp-1.2.4-jar-with-dependencies.jar /data  