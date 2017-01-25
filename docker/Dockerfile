# Licensed under the Apache License, Version 2.0 (the "License"); 
# you may not use this file except in compliance with the License. 
# You may obtain  a copy of the License at 
#
# http://www.apache.org/licenses/LICENSE-2.0 Unless 
# 
# required by applicable law or agreed to in writing, software 
# distributed under the License is distributed on an "AS IS" 
# BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either 
# express or implied. See the License for the specific language 
# governing permissions and limitations under the License. 

# This Dockerfile will build the required environment and software 
# stack to easily get up and running with mudrod
# For more documentation please see https://github.com/mudrod/mudrod

FROM centos:7

MAINTAINER Mudrod Team <mudrod-all@jpl.nasa.gov>

# To enable systemd. See https://github.com/docker-library/docs/tree/master/centos
ENV container docker
RUN (cd /lib/systemd/system/sysinit.target.wants/; for i in *; do [ $i == \
systemd-tmpfiles-setup.service ] || rm -f $i; done); \
rm -f /lib/systemd/system/multi-user.target.wants/*;\
rm -f /etc/systemd/system/*.wants/*;\
rm -f /lib/systemd/system/local-fs.target.wants/*; \
rm -f /lib/systemd/system/sockets.target.wants/*udev*; \
rm -f /lib/systemd/system/sockets.target.wants/*initctl*; \
rm -f /lib/systemd/system/basic.target.wants/*;\
rm -f /lib/systemd/system/anaconda.target.wants/*;
VOLUME [ "/sys/fs/cgroup" ]

USER root
WORKDIR /tmp

# Install various tools
RUN yum -y install wget git which

# Install Oracle JDK 1.8u121-b13
RUN wget -q --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u121-b13/e9e7ea248e2c4826b92b3f075a80e441/jdk-8u121-linux-x64.rpm"
RUN yum -y install jdk-8u121-linux-x64.rpm
ENV JAVA_HOME /usr/java/default

# ########################
# # Apache Maven   #
# ########################
ENV M2_HOME /usr/local/apache-maven
ENV M2 $M2_HOME/bin 
ENV PATH $PATH:$M2

RUN mkdir $M2_HOME
RUN wget -q http://mirror.olnevhost.net/pub/apache/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz && \
    tar -xvzf apache-maven-3.0.5-bin.tar.gz -C $M2_HOME --strip-components=1 && \
    rm -f apache-maven-3.0.5-bin.tar.gz

# ########################
# # Apache Spark 1.6.2   #
# ########################
ENV SPARK_PKG_NAME spark-2.1.0
ENV SPARK_HOME /usr/local/spark
ENV PATH $PATH:$SPARK_HOME/bin

RUN wget -q http://archive.apache.org/dist/spark/$SPARK_PKG_NAME/$SPARK_PKG_NAME-bin-hadoop2.7.tgz  && \
    tar -xvzf $SPARK_PKG_NAME-bin-hadoop2.7.tgz && \
    rm -f $SPARK_PKG_NAME-bin-hadoop2.7.tgz && \
    mv $SPARK_PKG_NAME-bin-hadoop2.7 $SPARK_HOME

# Create spark user
RUN useradd -ms /bin/bash spark
RUN chown -R spark:spark $SPARK_HOME

# #######################
# # Elasticsearch 2.3.4 #
# #######################

# Download Elastic
ENV ES_PKG_NAME elasticsearch-2.3.4
ENV ES_HOME /usr/local/elasticsearch
RUN wget -q https://download.elastic.co/elasticsearch/release/org/elasticsearch/distribution/tar/elasticsearch/2.3.4/$ES_PKG_NAME.tar.gz && \
  tar -xvzf $ES_PKG_NAME.tar.gz && \
  rm -f $ES_PKG_NAME.tar.gz && \
  mv $ES_PKG_NAME $ES_HOME

# Create elastic user
RUN groupadd elasticsearch
RUN useradd -s /bin/nologin elasticsearch -g elasticsearch
RUN chown -R elasticsearch:elasticsearch $ES_HOME

# Define mountable directories.
RUN mkdir -p /data
RUN chown -R elasticsearch:elasticsearch /data
# VOLUME ["/data"]

# Mount elasticsearch.yml config
ADD config/elasticsearch.yml $ES_HOME/config/elasticsearch.yml

# Expose ports.
# - 9200: HTTP
# - 9300: transport
EXPOSE 9200
EXPOSE 9300

# Enable elasticsearch service
ADD elasticsearch.service /etc/systemd/system/elasticsearch.service
RUN systemctl enable elasticsearch.service

# #########################
# # Apache Tomcat 8.5.x   #
# #########################
USER root
ENV TOMCAT_HOME /opt/tomcat
# Create tomcat user and group
RUN groupadd tomcat && \
    mkdir $TOMCAT_HOME && \
    useradd -s /bin/nologin -g tomcat -d $TOMCAT_HOME tomcat

# Download tomcat
WORKDIR /tmp
RUN wget -q http://apache.spinellicreations.com/tomcat/tomcat-8/v8.5.11/bin/apache-tomcat-8.5.11.tar.gz && \
  tar -xvzf apache-tomcat-8.5.11.tar.gz -C $TOMCAT_HOME --strip-components=1 && \
  rm -f apache-tomcat-8.5.11.tar.gz

# Set file permissions
WORKDIR $TOMCAT_HOME
RUN chgrp -R tomcat conf bin lib
RUN chmod g+rwx conf
RUN chmod g+r conf/*
RUN chown -R tomcat logs/ temp/ webapps/ work/

# Enable tomcat service
ADD tomcat.service /etc/systemd/system/tomcat.service
RUN systemctl enable tomcat.service

EXPOSE 8080

# #########################
# # Mudrod Master Branch  #
# #########################

# Create mudrod user
# RUN useradd -ms /bin/bash mudrod
ENV MUDROD_HOME /usr/local/mudrod
RUN mkdir -p $MUDROD_HOME
# RUN chown -R mudrod:mudrod $MUDROD_HOME
# So you can call 'mudrod'
ENV PATH $PATH:$MUDROD_HOME/bin

# USER mudrod

# Clone master repo
RUN git clone https://github.com/mudrod/mudrod.git $MUDROD_HOME

# Update configs
RUN sed -i '/.*svmSgdModel.*/c <para name="svmSgdModel">file:\/\/\/usr\/local\/mudrod\/core\/src\/main\/resources\/javaSVMWithSGDModel<\/para>' $MUDROD_HOME/core/src/main/resources/config.xml
RUN sed -i '/.*mudrod\.log\.dir.*/c mudrod\.log\.dir=$TOMCAT_HOME/logs' $MUDROD_HOME/core/src/main/resources/log4j.properties
RUN sed -i '/.*mudrod\.log\.dir.*/c mudrod\.log\.dir=$TOMCAT_HOME/logs' $MUDROD_HOME/service/src/main/resources/log4j.properties

# Build app
WORKDIR $MUDROD_HOME
RUN mvn clean package

# Deploy app to tomcat
USER tomcat
RUN cp $MUDROD_HOME/service/target/mudrod-service-0.0.1-SNAPSHOT.war $TOMCAT_HOME/webapps

#
# # Ensure that all of the services are running
# #CMD sh $MUDROD_HOME/docker/run_services.sh
#
# # Set entry point to the Mudrod executable
# # ENTRYPOINT $MUDROD_HOME/docker/run_services.sh && bash
# ADD run_services.sh $MUDROD_HOME/docker/run_services.sh
# ENTRYPOINT $MUDROD_HOME/docker/run_services.sh && bash

USER root
WORKDIR /usr/local
ADD run_services.sh $MUDROD_HOME/docker/run_services.sh
# ADD entrypoint.sh /usr/local/entrypoint.sh
# ENTRYPOINT /usr/local/entrypoint.sh
# USER mudrod