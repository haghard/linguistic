addCommandAlias(
  "engine-0",
  "server/runMain linguistic.Application\n" +
  "-DENV=development\n" +
  "-DCONFIG=./conf\n" +
  "-Dakka.http.port=9443\n" +
  "-Dakka.remote.artery.canonical.port=2550\n" +
  "-Dakka.remote.artery.canonical.hostname=127.0.0.1\n"+
  //"-Dakka.cluster.seed-nodes.0=akka://linguistics@127.0.0.1:2551\n" +
  //"-Dakka.cluster.seed-nodes.1=akka://linguistics@127.0.0.2:2552\n" +
  "-Dcassandra.hosts=127.0.0.1"
)

//sudo ifconfig lo0 127.0.0.2 add
addCommandAlias(
  "engine-1",
  "server/runMain linguistic.Application\n" +
  "-DENV=development\n" +
  "-DCONFIG=./conf\n" +
  "-Dakka.http.port=9443\n" +
  "-akka.remote.artery.canonical.hostname=127.0.0.2\n" +
  "-Dakka.remote.artery.canonical.port=2550\n" +
  "-Dcassandra.hosts=127.0.0.1"
)

/*
docker-compose -f docker-compose5.yml up
docker exec -it dc1a8598a31f cqlsh

cqlsh> drop KEYSPACE linguistics ;
cqlsh> CREATE KEYSPACE IF NOT EXISTS linguistics WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 };
*/