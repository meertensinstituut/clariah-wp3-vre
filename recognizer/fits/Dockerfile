FROM tomcat:latest

RUN apt-get update \
 && apt-get -y dist-upgrade \
 && apt-get -y install file less openjdk-8-jdk

COPY fits-zip/* /tmp/

RUN mkdir /tmp/fits \
 && cd /tmp/fits \
 && ls -R /tmp \
 && if [ -f /tmp/fits-1.2.0.zip ] ; \
 then cp /tmp/fits-1.2.0.zip /tmp/fits/fits-1.2.0.zip ; \
 else wget http://projects.iq.harvard.edu/files/fits/files/fits-1.2.0.zip ; \
 fi \
 && unzip fits-1.2.0.zip \
 && mkdir /app \
 && mv fits-1.2.0 /app/fits \
 && mkdir /app/fits/log-lib \
 && mv /app/fits/lib/slf4j-log4j12-1.7.12.jar /app/fits/log-lib \
 && sed -i 's|APPCLASSPATH=""|APPCLASSPATH="${FITS_HOME}/log-lib/slf4j-log4j12-1.7.12.jar"|g' /app/fits/fits-env.sh \
 && chmod +x /app/fits/*.sh \
 && wget http://projects.iq.harvard.edu/files/fits/files/fits-1.1.3.war \
 && cp fits-1.1.3.war /usr/local/tomcat/webapps/fits.war \
 && mkdir /usr/local/tomcat/webapps/fits \
 && cd /usr/local/tomcat/webapps/fits \
 && unzip -n ../fits.war \
 && echo "fits.home=/app/fits" >> $CATALINA_HOME/conf/catalina.properties \
 && echo 'shared.loader=${fits.home}/lib/*.jar' >> $CATALINA_HOME/conf/catalina.properties

CMD ["catalina.sh", "run"]
