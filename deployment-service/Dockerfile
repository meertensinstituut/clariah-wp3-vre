FROM tomcat:latest

RUN apt-get update && \
    apt-get install -y \
      curl \
      vim \
      maven \
      openjdk-8-jdk \
  && rm -rf /var/lib/apt/lists/*

CMD ["catalina.sh", "run"]
ADD . /tmp/deployment
RUN cd /tmp/deployment &&\
    ls -all &&\
    mvn package &&\
    cp deployment-api/target/deployment-api.war /usr/local/tomcat/webapps/deployment-service.war