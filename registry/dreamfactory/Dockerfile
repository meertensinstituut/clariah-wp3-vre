FROM ubuntu:16.04

MAINTAINER Matthijs Brouwer <matthijs.brouwer@meertens.knaw.nl>

#ENV ADMIN_FIRSTNAME Dream
#ENV ADMIN_LASTNAME Factory
#ENV ADMIN_EMAIL noreply@dreamfactory.dev
#ENV ADMIN_PASSWORD password

#ENV APP_KEY_SERVICES fdabbd4461d6fe6bc6e6e647b296e6ab91d4da8aba071160d2d8a543279b2b0d
#ENV USER_SERVICES_EMAIL services@vre.dev
#ENV USER_SERVICES_PASSWORD pwservices
#ENV DB_SERVICES_HOST postgres
#ENV DB_SERVICES_PORT 5432
#ENV DB_SERVICES_DATABASE services
#ENV DB_SERVICES_USER services
#ENV DB_SERVICES_PASSWORD pwservices
        
#ENV APP_KEY_OBJECTS f7bcae3915d9a29db3363238d3a242af8ad77f45e4a1647bcb7f196b2db36bb6
#ENV USER_OBJECTS_EMAIL objects@vre.dev
#ENV USER_OBJECTS_PASSWORD pwobjects
#ENV DB_OBJECTS_HOST postgres
#ENV DB_OBJECTS_PORT 5432  
#ENV DB_OBJECTS_DATABASE objects
#ENV DB_OBJECTS_USER objects
#ENV DB_OBJECTS_PASSWORD pwobjects

RUN apt-get update && apt-get install -y \
    apache2 libapache2-mod-php git-core curl zip unzip php-zip php-xml php-curl \
    php-mbstring php-sqlite3 php-pgsql jq postgresql-client
        
RUN service apache2 stop && \
    echo "ServerName localhost" | tee /etc/apache2/conf-available/fqdn.conf 

ADD config/apache/dreamfactory.conf /etc/apache2/sites-available/dreamfactory.conf  

RUN a2enmod deflate && \
    a2enmod rewrite && \
    a2ensite dreamfactory && a2dissite 000-default
    
RUN mkdir /postgres
ADD config/postgres/* /postgres/
    
RUN php -r "copy('https://getcomposer.org/installer', '/tmp/composer-setup.php');" && \
    php /tmp/composer-setup.php --install-dir=/usr/local/bin --filename=composer && \
    rm -rf /tmp/composer-setup.php        
    
EXPOSE 80

RUN git clone -b 2.11.1 https://github.com/dreamfactorysoftware/dreamfactory.git /opt/dreamfactory

WORKDIR /opt/dreamfactory

RUN composer remove dreamfactory/df-mongodb --update-no-dev && composer install

RUN chown -R www-data:www-data /opt/dreamfactory

RUN mkdir /dreamfactory && \
    mkdir /dreamfactory/system 
    
ADD config/dreamfactory/package.json /dreamfactory/
ADD config/dreamfactory/system/* /dreamfactory/system/
ADD config/dreamfactory/start.sh /dreamfactory/
        
CMD bash -C '/postgres/start.sh'; bash -C '/dreamfactory/start.sh'; 'bash'    
    
