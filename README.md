# Linguistic


## Run the application
```shell
$ sbt
> ~re-start

Cluster info
===
  http GET 192.168.0.62:9000/cluster/wordslist/shards  
  http --verify=no https://192.168.0.62:9443/cluster/wordslist/shards
  
  http GET 192.168.0.62:9000/cluster/wordslist/shards2
  http --verify=no https://192.168.0.62:9443/cluster/wordslist/shards2
  
  http GET 192.168.0.62:9000/cluster/wordslist/regions
  http --verify=no https://192.168.0.62:9443/cluster/wordslist/regions
  
  http GET 192.168.0.62:9000/cluster/homophones/shards
  http --verify=no https://192.168.0.62:9443/cluster/homophones/shards
  
  http GET 192.168.0.62:9000/cluster/homophones/shards2
  http --verify=no https://192.168.0.62:9443/cluster/homophones/shards2
  
  http GET 192.168.0.62:9000/cluster/homophones/regions
  http --verify=no https://192.168.0.62:9443/cluster/homophones/regions

Links
====
    https://github.com/softwaremill/akka-http-session
    http://www.cakesolutions.net/teamblogs/composing-microservices-with-docker-part1
    http://www.cakesolutions.net/teamblogs/composing-microservices-with-docker-part2
    https://www.javacodegeeks.com/2016/11/developing-modern-applications-scala-web-apis-akka-http.html
    https://medium.com/@ianjuma/deploying-scala-on-docker-e8d98aa39bcf#.oonxy2zi8
    https://www.digitalocean.com/community/tutorials/how-to-provision-and-manage-remote-docker-hosts-with-docker-machine-on-ubuntu-16-04

Generating self-signed certificates

The first step is to create a certificate authority that will sign the linguistic.com certificate. The root CA certificate has a couple of additional attributes (ca:true, keyCertSign) that mark it explicitly as a CA certificate, and will be kept in a trust store.
====

    keytool -genkeypair -v \
       -alias linguistic.com \
       -dname "CN=linguistic.com, O=Chatter Company, L=Spb, ST=Spb, C=RU" \
       -keystore server/src/main/resources/linguistic.jks  \
       -keypass avmiejtq  \
       -storepass akdfopjb \
       -keyalg RSA \
       -keysize 4096 \
       -ext KeyUsage:critical="keyCertSign" \
       -ext BasicConstraints:critical="ca:true" \
       -validity 365


Export the linguistic public certificate as linguistic.crt so that it can be used in trust stores.
====

	keytool -export -v \
	  -alias linguistic.com \
	  -file linguistic.crt \
	  -keypass avmiejtq \
	  -storepass akdfopjb \
	  -keystore server/src/main/resources/linguistic.jks \
	  -rfc

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

#### Env ###
  You can run this server using either prod or dev env. If there is production.conf in resources it will be attached during docker build, otherwise application.conf will be used 
  
    
#### Clean docker images ####  
  
Delete dangling containers  `docker rmi $( docker images -q -f dangling=true)`

Delete all stopped containers `docker rm $( docker ps -q -f status=exited)`

  `docker rm $(docker ps -a -q)`

### Ssh links ###

  http://typesafehub.github.io/ssl-config/
  http://typesafehub.github.io/ssl-config/CertificateGeneration.html
  
RadixTree
===

  https://upload.wikimedia.org/wikipedia/commons/thumb/a/ae/Patricia_trie.svg/400px-Patricia_trie.svg.png
  https://en.wikipedia.org/wiki/Radix_tree
  
WebUi
===
  
  https://getbootstrap.com/components/#navbar
  http://foat.me/articles/reactive-fun-with-scala-js/
  http://www.baeldung.com/geolocation-by-ip-with-maxmind

Graph
===

  https://github.com/nvd3-community/nvd3/tree/gh-pages
  https://github.com/nvd3-community/nvd3/blob/gh-pages/examples/forceDirected.html

Scalajs-react
===
  
  https://japgolly.github.io/scalajs-react/#examples/websockets
  https://japgolly.github.io/scalajs-react/#examples/product-table

 
ScalaJs
===
    http://www.scala-js.org/tutorial/basic/
    http://www.scala-js.org/tutorial/basic/
    https://github.com/japgolly/scalajs-react/blob/master/doc/USAGE.md
    https://github.com/chandu0101/scalajs-react-components
    https://japgolly.github.io/scalajs-react/#examples/todo        

SSL
===
    http://typesafehub.github.io/ssl-config/CertificateGeneration.html


Run docker container

  To run with `--net=host` is necessary because you are passing env val   

  
  `docker run --net=host -it -p 2551:2551 -e HOSTNAME=192.168.0.146 -e AKKA_PORT=2551 -e HTTP_PORT=9443 -e JMX_PORT=1089 -e TZ="Europe/Moscow" haghard/linguistic:0.1`

  if you want pass SECRET 
  
  `docker run --net=host -it -p 2551:2551 -e HOSTNAME=192.168.0.146 -e PORT=2551 -e JMX_PORT=1089 -e SERVER_SECRET=... haghard/linguistic:0.1`