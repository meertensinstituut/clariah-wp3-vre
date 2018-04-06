FROM ubuntu:16.04

EXPOSE 8983
  
USER root

WORKDIR "/root" 

RUN mkdir lib

ADD https://github.com/meertensinstituut/mtas/releases/download/v7.2.1.1/mtas-7.2.1.1.jar /root/lib/

ADD config /root/config

RUN apt-get update && apt-get install -y locales lsof software-properties-common python-software-properties curl subversion && \
    locale-gen en_US.UTF-8 en_US && update-locale LANG=en_US.UTF-8 LANGUAGE=en_US:en
            
RUN mathurl=$(curl -s 'http://www.apache.org/dyn/closer.lua/commons/math/binaries/commons-math3-3.6.1-bin.tar.gz' |   grep -o '<strong>[^<]*</strong>' |   sed 's/<[^>]*>//g' |   head -1) && \
    if echo "$mathurl" | grep -q '^.*[^ ].*$'; then \
      curl -f -o /root/lib/commons-math3-3.6.1-bin.tar.gz -O $mathurl || true; \
    fi && \
    if [ ! -f /root/lib/commons-math3-3.6.1-bin.tar.gz ]; then \
      echo "Commons-math3 not found in mirror, falling back to apache archive"; \ 
      mathurl="http://archive.apache.org/dist/commons/math/binaries/commons-math3-3.6.1-bin.tar.gz"; \
      curl -f -o /root/lib/commons-math3-3.6.1-bin.tar.gz -O $mathurl; \
    fi && \ 
    tar xzf lib/commons-math3-3.6.1-bin.tar.gz -C lib commons-math3-3.6.1/commons-math3-3.6.1.jar --strip-components=1 && \
    rm lib/commons-math3-3.6.1-bin.tar.gz
    
RUN add-apt-repository -y ppa:webupd8team/java && \
    apt-get update && \
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    apt-get install -y oracle-java8-installer && \
    rm -rf /var/lib/apt/lists/*

RUN solrurl=$(curl -s 'http://www.apache.org/dyn/closer.lua/lucene/solr/7.2.1/solr-7.2.1.tgz' |   grep -o '<strong>[^<]*</strong>' |   sed 's/<[^>]*>//g' |   head -1) && \
    if echo "$solrurl" | grep -q '^.*[^ ].*$'; then \
      curl -f -o /root/solr-7.2.1.tgz -O $solrurl || true; \
    fi && \
    if [ ! -f /root/solr-7.2.1.tgz ]; then \
      echo "Solr 7.2.1 not found in mirror, falling back to apache archive"; \ 
      solrurl="http://archive.apache.org/dist/lucene/solr/7.2.1/solr-7.2.1.tgz"; \
      curl -f -o /root/solr-7.2.1.tgz -O $solrurl; \ 
    fi && \    
    tar xzf solr-7.2.1.tgz solr-7.2.1/bin/install_solr_service.sh --strip-components=2 && \
    bash ./install_solr_service.sh solr-7.2.1.tgz && rm install_solr_service.sh && rm -rf solr-7.2.1.tgz
    
RUN printf "echo\n" >> /start.sh && \
    printf "echo \"========================== VRE - SOLR Search ===========================\"\n" >> /start.sh && \
    printf "echo \"  Lucene/Solr version 7.2.1\"\n" >> /start.sh && \
    printf "echo \"  Mtas release 7.2.1.1\"\n" >> /start.sh && \
    printf "echo \"  See https://meertensinstituut.github.io/mtas/ for more information\"\n" >> /start.sh && \
    printf "echo \"========================================================================\"\n" >> /start.sh && \
    printf "echo\n" >> /start.sh && \
    printf "service solr start\n" >> /start.sh && \
    chmod 755 /start.sh && \
    mkdir vrecore && mkdir vrecore/lib && \
    echo "name=vrecore" > vrecore/core.properties && \
    cp -rp config/conf vrecore && \
    cp lib/commons-math3-3.6.1.jar vrecore/lib/ && \
    cp lib/mtas-7.2.1.1.jar vrecore/lib/ && \
    chmod -R 777 vrecore && \
    mv vrecore /var/solr/data/
    
RUN usermod -a -G root solr && usermod -a -G www-data solr 

CMD bash -C '/start.sh'; 'bash'
