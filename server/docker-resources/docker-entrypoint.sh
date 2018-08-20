#!/bin/bash

set -e
set -x

APP_OPTS="-server \
          -XX:MaxGCPauseMillis=400 \
          -XX:+UseStringDeduplication \
          -XX:+UseG1GC \
          -XX:ConcGCThreads=4 -XX:ParallelGCThreads=4 \
          -XX:+UnlockExperimentalVMOptions \
          -XX:+UseCGroupMemoryLimitForHeap \
          -XX:MaxRAMFraction=1 \
          -XshowSettings \
          -Dcom.sun.management.jmxremote.port=${JMX_PORT} \
          -Dcom.sun.management.jmxremote.ssl=false \
          -Dcom.sun.management.jmxremote.authenticate=false \
          -Dcom.sun.management.jmxremote.local.only=false \
          -Dcom.sun.management.jmxremote.rmi.port=${JMX_PORT} \
          -Dcom.sun.management.jmxremote=true \
          -Dakka.remote.netty.tcp.port=${AKKA_PORT} \
          -Dakka.http.port=${HTTP_PORT} \
          -Djava.rmi.server.hostname=${HOSTNAME} \
          -DENV=${ENV} \
          -DHOSTNAME=${HOSTNAME} \
          -DCONFIG=${CONFIG} \
          -Dcassandra.hosts=${CASSANDRA} \
          -DETCD="${ETCD}

#production, development

java ${APP_OPTS} -cp ${APP_BASE}/conf -jar ${APP_BASE}/linguistic-${VERSION}.jar