
/*
export ENV=development
export CONFIG=./server/conf

export ENV=production
export CONFIG=./server/conf

*/

addCommandAlias(
  "engine-0",
  "runMain linguistic.Application " +
  "-DENV=development " +
  "-Dakka.remote.netty.tcp.port=2551 " +
  "-Dakka.http.port=9443 " +
  "-DHOSTNAME=192.168.0.62 " +
  "-Dakka.cluster.roles.0=linguistic-engine " +
  "-Dakka.cluster.seed-nodes.0=akka.tcp://linguistics@192.168.0.62:2551 " +
  "-Dakka.cluster.seed-nodes.1=akka.tcp://linguistics@192.168.0.146:2551 "
)

addCommandAlias(
  "engine-1",
  "runMain linguistic.Application " +
  "-DENV=development " +
  "-Dakka.remote.netty.tcp.port=2551 " +
  "-Dakka.http.port=9443 " +
  "-DHOSTNAME=192.168.0.146 " +
  "-Dakka.cluster.roles.0=linguistic-engine " +
  "-Dakka.cluster.seed-nodes.0=akka.tcp://linguistics@192.168.0.62:2551 " +
  "-Dakka.cluster.seed-nodes.1=akka.tcp://linguistics@192.168.0.146:2551 "
)