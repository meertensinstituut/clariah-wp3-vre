FROM tomcat:9-jre11

RUN apt-get update && \
    apt-get install -y \
      curl \
      vim \
      maven \
    #   openjdk-10-jdk \
  && rm -rf /var/lib/apt/lists/*

ENV CATALINA_OPTS="-Xmx700m --add-exports java.base/jdk.internal.ref=ALL-UNNAMED"

ADD ./conf/conf.xml /conf/conf.xml
ADD logging.properties /usr/local/tomcat/conf/logging.properties
CMD ["catalina.sh", "run"]