addCommandAlias(
  "engine-0",
  "server/runMain linguistic.Application " +
  "-DENV=development " +
  "-DCONFIG=./conf " +
  "-Dakka.remote.netty.tcp.port=2551 " +
  "-Dakka.http.port=9443 " +
  "-DHOSTNAME=192.168.77.10 " +
  "-Dakka.cluster.seed-nodes.0=akka.tcp://linguistics@192.168.77.10:2551\n" +
  "-Dakka.cluster.seed-nodes.1=akka.tcp://linguistics@192.168.77.83:2551\n" +
  "-Dcassandra.hosts=130.193.44.21,130.193.44.47"
)

addCommandAlias(
  "engine-1",
  "server/runMain linguistic.Application " +
  "-DENV=development " +
  "-DCONFIG=./conf " +
  "-Dakka.remote.netty.tcp.port=2551 " +
  "-Dakka.http.port=9443 " +
  "-DHOSTNAME=192.168.77.83 " +
  "-Dakka.cluster.seed-nodes.0=akka.tcp://linguistics@192.168.77.10:2551\n" +
  "-Dakka.cluster.seed-nodes.1=akka.tcp://linguistics@192.168.77.83:2551\n" +
  "-Dcassandra.hosts=130.193.44.21,130.193.44.47 "
)