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
  
Delete dangling containers  `docker rmi $(docker images -q -f dangling=true)`

Delete all stopped containers `docker rm $(docker ps -q -f status=exited)`

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


Akka links

CPU considerations for Java applications running in Docker and Kubernetes:
    https://www.lightbend.com/blog/cpu-considerations-for-java-applications-running-in-docker-and-kubernetes


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
        `sbt -Denv=development docker && docker push haghard/linguistic:0.3`
        
    Docker image for production
        `sbt docker`
    

Run docker container
  https://blog.csanchez.org/2017/05/31/running-a-jvm-in-a-container-without-getting-killed/
  
  To run it with `--net=host` is necessary because you are passing env val   
  
  `docker run --net=host -it -p 2551:2551 -e HOSTNAME=78.155.219.177 -e AKKA_PORT=2551 -e ETCD=80.93.177.253 -e HTTP_PORT=9443 -e CASSANDRA=80.93.177.253,78.155.207.129 -e JMX_PORT=1089 -e TZ="Europe/Moscow" haghard/linguistic:0.3`


1GB

https://doc.akka.io/docs/akka/current/remoting.html#remote-configuration-nat
docker run --net=host -it -p 2551:2551 -p 9443:9443 -e HOSTNAME=192.168.77.10 -e AKKA_PORT=2551 -e ETCD=192.168.77.85 -e HTTP_PORT=9443 -e CASSANDRA=192.168.77.85,192.168.77.42 -e JMX_PORT=1089 -e TZ="Europe/Moscow" -m 500MB haghard/linguistic:0.3


Suppose we have 2 machines 95.213.199.95,95.213.235.117


docker run -d -e CASSANDRA_BROADCAST_ADDRESS=95.213.199.95 -e CASSANDRA_SEEDS=95.213.199.95,95.213.235.117 \
      -e CASSANDRA_CLUSTER_NAME="linguistic" -e CASSANDRA_HOME="/var/lib/cassandra"  \
      -e CASSANDRA_START_RPC="true" -e CASSANDRA_RACK="wr1" -e CASSANDRA_DC="spb"  \
      -e CASSANDRA_ENDPOINT_SNITCH="GossipingPropertyFileSnitch"  \
      -p 7000:7000 -p 7001:7001 -p 9042:9042 -p 9160:9160 -p 7199:7199  \
      -v /media/haghard/data/linguistic-db:/var/lib/cassandra cassandra:3.11.3
    
docker run -d -e CASSANDRA_BROADCAST_ADDRESS=95.213.235.117 -e CASSANDRA_SEEDS=95.213.199.95,95.213.235.117  \
    -e CASSANDRA_CLUSTER_NAME="linguistic" -e CASSANDRA_HOME="/var/lib/cassandra"  \
    -e CASSANDRA_START_RPC="true" -e CASSANDRA_RACK="wr2" -e CASSANDRA_DC="spb" \
    -e CASSANDRA_ENDPOINT_SNITCH="GossipingPropertyFileSnitch"  \
    -p 7000:7000 -p 7001:7001 -p 9042:9042 -p 9160:9160 -p 7199:7199  \
    -v /media/haghard/data/linguistic-db:/var/lib/cassandra cassandra:3.11.3




docker run -d -p 2380:2380 -p 2379:2379 quay.io/coreos/etcd:v2.3.7 \
  -name etcd0 \
  -advertise-client-urls http://95.213.199.95:2379 \
  -listen-client-urls http://0.0.0.0:2379 \
  -initial-advertise-peer-urls http://95.213.199.95:2380 \
  -listen-peer-urls http://0.0.0.0:2380 \
  -initial-cluster etcd0=http://95.213.199.95:2380,etcd1=http://95.213.235.117:2380 \
  -initial-cluster-state new
      
docker run -d -p 2380:2380 -p 2379:2379 quay.io/coreos/etcd:v2.3.7 \
  -name etcd1 \
  -advertise-client-urls http://95.213.235.117:2379 \
  -listen-client-urls http://0.0.0.0:2379 \
  -initial-advertise-peer-urls http://95.213.235.117:2380 \
  -listen-peer-urls http://0.0.0.0:2380 \
  -initial-cluster etcd0=http://95.213.199.95:2380,etcd1=http://95.213.235.117:2380 \
  -initial-cluster-state new        


Check Etcd registry   
  curl http://212.92.98.127:2379/v2/keys/constructr/linguistics/nodes  
    
docker run --net=host -it -p 2551:2551 -p 9443:9443 -e HOSTNAME=95.213.199.95 -e AKKA_PORT=2551 -e ETCD=95.213.199.95 -e HTTP_PORT=9443 -e CASSANDRA=95.213.199.95,95.213.235.117 -e JMX_PORT=1089 -m 650MB haghard/linguistic:0.3
docker run --net=host -it -p 2551:2551 -p 9443:9443 -e HOSTNAME=95.213.235.117 -e AKKA_PORT=2551 -e ETCD=95.213.235.117 -e HTTP_PORT=9443 -e CASSANDRA=95.213.199.95,95.213.235.117 -e JMX_PORT=1089 -m 650MB haghard/linguistic:0.3


http --verify=no https://212.92.98.127:9443/cluster/words/regions
http --verify=no https://212.92.98.127:9443/cluster/homophones/regions

http --verify=no https://212.92.98.127:9443/cluster/homophones/shards
http --verify=no https://95.213.236.24:9443/cluster/homophones/shards




How to run 2 node from sbt

on 192.168.77.10
sbt 
engine-0 -Dcassandra.username=fsa -Dcassandra.password=...

on 192.168.77.85
sbt 
engine-1 -Dcassandra.username=fsa -Dcassandra.password=...




https://github.com/ktoso/akka-codepot-workshop
https://github.com/ktoso/akka-codepot-workshop/blob/aac84e3a5c1ca6edcdbaa2befd35cd161b2da69a/src/main/scala/akka/codepot/engine/search/SearchMaster.scala


CREATE KEYSPACE IF NOT EXISTS linguistics WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 3 };
select * from linguistics.linguistics_journal where persistence_id='/sharding/wordsShard/a' and partition_nr=0;
select * from linguistics.linguistics_journal where persistence_id='/sharding/wordsCoordinator' and partition_nr=0;
                                    

https://github.com/wolfgarbe/SymSpell#ports

Blog Posts
The Pruning Radix Trie — a Radix trie on steroids (https://seekstorm.com/blog/pruning-radix-trie/)
1000x Faster Spelling Correction algorithm (https://seekstorm.com/blog/1000x-spelling-correction/)
SymSpell vs. BK-tree: 100x faster fuzzy string search & spell checking (https://seekstorm.com/blog/symspell-vs-bk-tree/)

https://github.com/wolfgarbe/symspell?tab=readme-ov-file
https://wolfgarbe.medium.com/1000x-faster-spelling-correction-algorithm-2012-8701fcd87a5f
https://github.com/MighTguY/customized-symspell


Suggestion, autocompletion, and correction
search_as_you_type
                     

docker-compose -f docker-compose5.yml up
docker exec -it dc1a8598a31f cqlsh

cqlsh> drop KEYSPACE linguistics ;
cqlsh> CREATE KEYSPACE IF NOT EXISTS linguistics WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 };

select * from linguistics.linguistics_journal where persistence_id='/sharding/wordsShard/a' and partition_nr=0;


https://github.com/dwyl/english-words



```
jcmd 39719 VM.native_memory


 Internal (reserved=847KB, committed=847KB)
        (malloc=815KB #14548) 
        (mmap: reserved=32KB, committed=32KB) 
                                           

jcmd 39719 VM.stringtable
39719:
StringTable statistics:
Number of buckets       :     65536 =    524288 bytes, each 8
Number of entries       :     25911 =    414576 bytes, each 16
Number of literals      :     25911 =   2007056 bytes, avg  77.000
Total footprint         :           =   2 945 920 bytes
Average bucket size     :     0.395
Variance of bucket size :     0.396
Std. dev. of bucket size:     0.630
Maximum bucket size     :         5

jcmd 39719 VM.symboltable
39719:
SymbolTable statistics:
Number of buckets       :     32768 =    262144 bytes, each 8
Number of entries       :    179624 =   2873984 bytes, each 16
Number of literals      :    179624 =  13860920 bytes, avg  77.000
Total footprint         :           =  16 997 048 bytes
Average bucket size     :     5.482
Variance of bucket size :     5.470
Std. dev. of bucket size:     2.339
Maximum bucket size     :        17
   
```

```

jcmd 39719 VM.native_memory summary

```

curl --no-buffer -k https://127.0.0.1:9443/jvm