# Linguistic


## Run the application
```shell
$ sbt
> ~re-start

Cluster info routes 
    http --verify=no https://...:9443/cluster/wordslist/shards      
    http --verify=no https://...:9443/cluster/wordslist/shards2    
    http --verify=no https://...:9443/cluster/wordslist/regions    
    
    http --verify=no https://...:9443/cluster/homophones/shards    
    http --verify=no https://...:9443/cluster/homophones/shards2    
    http --verify=no https://...:9443/cluster/homophones/regions

My avatar 
    https://avatars.githubusercontent.com/u/1887034?v=3
  
Generating self-signed certificates

The first step is to create a certificate authority that will sign the linguistic.com certificate. The root CA certificate has a couple of additional attributes (ca:true, keyCertSign) that mark it explicitly as a CA certificate, and will be kept in a trust store.

    `keytool -genkeypair -v \
       -alias linguistic.com \
       -dname "CN=linguistic.com, O=Linguistic Company, L=Spb, ST=Spb, C=RU" \
       -keystore server/src/main/resources/linguistic.jks  \
       -keypass ...  \
       -storepass ... \
       -keyalg RSA \
       -keysize 4096 \
       -ext KeyUsage:critical="keyCertSign" \
       -ext BasicConstraints:critical="ca:true" \
       -validity 365`


Export the linguistic public certificate as linguistic.crt so that it can be used in trust stores.

	`keytool -export -v \
	  -alias linguistic.com \
	  -file linguistic.crt \
	  -keypass ... \
	  -storepass ... \
	  -keystore server/src/main/resources/linguistic.jks \
	  -rfc`

Checking the status of ssh :

  Download SSLyze:
      
      https://github.com/iSECPartners/sslyze/releases
    
  And then run SSLyze against the application:
      
      python /Volumes/Data/Setup/sslyze-0_8-osx64/sslyze.py --regular linguistic.com:9443
      python /Volumes/Data/Setup/sslyze-0_8-osx64/sslyze.py --regular 192.168.0.62:9443
  
Time zone options 

  TZ="UTC+5" - America/New_York 
  TZ="UTC-3" - Europe/Moscow
  TZ="UTC+8" - Los-Angeles
    
#### Clean docker images ####  
  
Delete dangling containers  `docker rmi $( docker images -q -f dangling=true)`

Delete all stopped containers `docker rm $( docker ps -q -f status=exited)`

  `docker rm $(docker ps -a -q)`

### Ssh links ###

  http://typesafehub.github.io/ssl-config/
  http://typesafehub.github.io/ssl-config/CertificateGeneration.html
  
RadixTree

  https://upload.wikimedia.org/wikipedia/commons/thumb/a/ae/Patricia_trie.svg/400px-Patricia_trie.svg.png
  https://en.wikipedia.org/wiki/Radix_tree
  
Useful Links

    https://github.com/softwaremill/akka-http-session
    http://www.cakesolutions.net/teamblogs/composing-microservices-with-docker-part1
    http://www.cakesolutions.net/teamblogs/composing-microservices-with-docker-part2
    https://www.javacodegeeks.com/2016/11/developing-modern-applications-scala-web-apis-akka-http.html
    https://medium.com/@ianjuma/deploying-scala-on-docker-e8d98aa39bcf#.oonxy2zi8
    https://www.digitalocean.com/community/tutorials/how-to-provision-and-manage-remote-docker-hosts-with-docker-machine-on-ubuntu-16-04
    http://www.jasondavies.com/wordtree/
    http://chandu0101.github.io/sjrc/
    https://www.jasondavies.com/wordtree/
    https://github.com/d3/d3/wiki/Gallery
    http://bl.ocks.org/mbostock/4063550
    https://ochrons.github.io/scalajs-spa-tutorial/en/routing.html

WebUi
  
  https://getbootstrap.com/components/#navbar
  http://foat.me/articles/reactive-fun-with-scala-js/
  http://www.baeldung.com/geolocation-by-ip-with-maxmind

Graph

  https://github.com/nvd3-community/nvd3/tree/gh-pages
  https://github.com/nvd3-community/nvd3/blob/gh-pages/examples/forceDirected.html

Scalajs-react
  
  https://japgolly.github.io/scalajs-react/#examples/websockets
  https://japgolly.github.io/scalajs-react/#examples/product-table
 
ScalaJs

    http://www.scala-js.org/tutorial/basic/
    http://www.scala-js.org/tutorial/basic/
    https://github.com/japgolly/scalajs-react/blob/master/doc/USAGE.md
    https://github.com/chandu0101/scalajs-react-components
    https://japgolly.github.io/scalajs-react/#examples/todo        

SSL

    http://typesafehub.github.io/ssl-config/CertificateGeneration.html        

To build Docker images

    Docker image for development
        `sbt -Denv=development docker`
        
    Docker image for production
        `sbt docker`


Suppose we have 2 machines 185.143.172.184 and 185.143.172.11

Install Cassandra

    docker run -d -e CASSANDRA_BROADCAST_ADDRESS=80.93.177.253 -e CASSANDRA_SEEDS=80.93.177.253,78.155.207.129  \
        -e CASSANDRA_CLUSTER_NAME="haghard_cluster" -e CASSANDRA_HOME="/var/lib/cassandra"  \
        -e CASSANDRA_START_RPC="true" -e CASSANDRA_RACK="wr1" -e CASSANDRA_DC="spb"  \
        -e CASSANDRA_ENDPOINT_SNITCH="GossipingPropertyFileSnitch"  \
        -p 7000:7000 -p 7001:7001 -p 9042:9042 -p 9160:9160 -p 7199:7199  \
        -v /home/haghard/db-3.10:/var/lib/cassandra cassandra:3.10
    
    docker run -d -e CASSANDRA_BROADCAST_ADDRESS=78.155.207.129 -e CASSANDRA_SEEDS=80.93.177.253,78.155.207.129  \
        -e CASSANDRA_CLUSTER_NAME="haghard_cluster" -e CASSANDRA_HOME="/var/lib/cassandra"  \
        -e CASSANDRA_START_RPC="true" -e CASSANDRA_RACK="wr2" -e CASSANDRA_DC="spb" \
        -e CASSANDRA_ENDPOINT_SNITCH="GossipingPropertyFileSnitch"  \
        -p 7000:7000 -p 7001:7001 -p 9042:9042 -p 9160:9160 -p 7199:7199  \
        -v /home/haghard/db-3.10:/var/lib/cassandra cassandra:3.10

Install Etcd

    docker run -d -p 2380:2380 -p 2379:2379 quay.io/coreos/etcd:v2.3.7 \
      -name etcd0 \
      -advertise-client-urls http://80.93.177.253:2379 \
      -listen-client-urls http://0.0.0.0:2379 \
      -initial-advertise-peer-urls http://80.93.177.253:2380 \
      -listen-peer-urls http://0.0.0.0:2380 \
      -initial-cluster etcd0=http://80.93.177.253:2380,etcd1=http://185.143.172.11:2380 \
      -initial-cluster-state new
      
    docker run -d -p 2380:2380 -p 2379:2379 quay.io/coreos/etcd:v2.3.7 \
      -name etcd1 \
      -advertise-client-urls http://78.155.207.129:2379 \
      -listen-client-urls http://0.0.0.0:2379 \
      -initial-advertise-peer-urls http://78.155.207.129:2380 \
      -listen-peer-urls http://0.0.0.0:2380 \
      -initial-cluster etcd0=http://80.93.177.253:2380,etcd1=http://78.155.207.129:2380 \
      -initial-cluster-state new
  
Etcd registry   
  curl http://...:2379/v2/keys/constructr/linguistics/nodes
  curl http://...:2379/v2/keys/constructr/linguistics/nodes
  

Run docker container

  To run it with `--net=host` is necessary because you are passing env val   
  
  `docker run --net=host -it -p 2551:2551 -e HOSTNAME=192.168.0.146 -e AKKA_PORT=2551 -e DISCOVERY=78.155.207.129 -e HTTP_PORT=9443 -e cassandra.hosts=80.93.177.253,78.155.207.129 -e JMX_PORT=1089 -e TZ="Europe/Moscow" haghard/linguistic:0.1`



"engine-0",
  "runMain linguistic.Application " +
  "-DENV=development " +
  "-DCONFIG=./server/conf " +
  "-Dakka.remote.netty.tcp.port=2551 " +
  "-Dakka.http.port=9443 " +
  "-DHOSTNAME=78.155.217.68 " +
  "-Dakka.cluster.roles.0=linguistic-engine " +
  "-DDISCOVERY=78.155.207.122 " +
  "-Dcassandra.hosts=78.155.207.122,78.155.218.24 "