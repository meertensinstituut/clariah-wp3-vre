FROM maven:3.6-jdk-11

WORKDIR /build

COPY ./parent-pom/pom.xml ./parent-pom/pom.xml
COPY ./integration/pom.xml ./integration/pom.xml
COPY ./recognizer/pom.xml ./recognizer/pom.xml
COPY ./switchboard/pom.xml ./switchboard/pom.xml
COPY ./tagger/pom.xml ./tagger/pom.xml
COPY ./deployment-service/pom.xml ./deployment-service/pom.xml
COPY ./deployment-service/deployment-lib/pom.xml ./deployment-service/deployment-lib/pom.xml
COPY ./deployment-service/deployment-api/pom.xml ./deployment-service/deployment-api/pom.xml

RUN mvn dependency:go-offline -f ./parent-pom/pom.xml
RUN mvn dependency:go-offline -f ./integration/pom.xml
RUN mvn dependency:go-offline -f ./recognizer/pom.xml
RUN mvn dependency:go-offline -f ./switchboard/pom.xml
RUN mvn dependency:go-offline -f ./tagger/pom.xml

# use go-offline-maven-plugin to prevent failure on not finding deployment-lib jar:
RUN cd deployment-service && mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

