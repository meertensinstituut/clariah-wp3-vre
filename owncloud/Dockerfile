FROM owncloud:latest

RUN apt-get update \
 && apt-get -y install watch \
 && apt-get -y install git \
 && apt-get -y install python \
 && apt-get -y install nano \
 && apt-get -y install sudo \
 # rdkafka:
 && cd /tmp \
 && mkdir librdkafka && cd librdkafka \
 && git clone https://github.com/edenhill/librdkafka.git . \
 && ./configure \
 && make \
 && make install \
 && pecl install rdkafka \
 && echo "extension=rdkafka.so" >> /usr/local/etc/php/php.ini \
 # occ requires pcntl:
 && docker-php-ext-install pcntl
