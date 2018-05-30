FROM ubuntu:16.04

MAINTAINER Matthijs Brouwer <matthijs.brouwer@meertens.knaw.nl>

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
    
