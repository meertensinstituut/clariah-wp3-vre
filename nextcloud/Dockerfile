FROM nextcloud:latest

RUN apt-get update \
 && apt-get -y install watch \
 git \
 python \
 vim \
 sudo \
 # rdkafka:
 && cd /tmp \
 && mkdir librdkafka && cd librdkafka \
 && git clone https://github.com/edenhill/librdkafka.git . \
 && ./configure \
 && make \
 && make install \
 && pecl install rdkafka \
 && echo "extension=rdkafka.so" >> /usr/local/etc/php/php.ini \
 && echo "memory_limit = 512M" >> /usr/local/etc/php/php.ini \
 # occ requires pcntl:
 && docker-php-ext-install pcntl \
 && echo "finish docker file!"