# Create basic docker image with empty maven repo:
#FROM tomcat:9-jre11
#RUN mkdir -p /root/.m2/repository
#WORKDIR /usr/local/deployment

FROM knawhuc/clariah-wp3-vre-deployment AS old

FROM knawhuc/clariah-wp3-vre-switchboard-builder
COPY --from=old /root/.m2/repository /root/.m2/repository

RUN apt-get update && \
    apt-get install -y curl vim maven && \
    rm -rf /var/lib/apt/lists/*

ENV CATALINA_OPTS="-Xmx700m --add-exports java.base/jdk.internal.ref=ALL-UNNAMED"

ADD ./conf/conf.xml /conf/conf.xml
ADD logging.properties /usr/local/tomcat/conf/logging.properties

RUN rm -rf /user/local/deployment
COPY ./ /usr/local/deployment
WORKDIR /usr/local/deployment

RUN echo "remove duplicate plexus" && rm /usr/share/maven/boot/plexus-classworlds-2.5.2.jar

# api.war contains both api and lib; hence the rename to service.war:
RUN mvn clean install && \
    cp deployment-api/target/deployment-api.war /usr/local/tomcat/webapps/deployment-service.war

CMD catalina.sh run