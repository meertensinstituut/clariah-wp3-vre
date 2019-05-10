FROM knawhuc/clariah-wp3-vre-builder as builder

ENV CATALINA_OPTS="-Xmx700m --add-exports java.base/jdk.internal.ref=ALL-UNNAMED"

ADD ./conf/conf.xml /conf/conf.xml
ADD logging.properties /usr/local/tomcat/conf/logging.properties

RUN rm -rf /user/local/deployment
COPY ./ /usr/local/deployment
WORKDIR /usr/local/deployment

RUN mvn clean install

FROM tomcat:9-jre11-slim

# api.war contains both api and lib; hence the rename to service.war:
COPY --from=builder /usr/local/deployment/deployment-api/target/deployment-api.war /usr/local/tomcat/webapps/deployment-service.war

CMD catalina.sh run