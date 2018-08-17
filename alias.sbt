addCommandAlias(
  "engine-0",
  "server/runMain linguistic.Application " +
  "-DENV=development " +
  "-DCONFIG=./server/conf " +
  "-Dakka.remote.netty.tcp.port=2551 " +
  "-Dakka.http.port=9443 " +
  "-DHOSTNAME=192.168.77.10 " +
  "-Dakka.cluster.roles.0=linguistic-engine " +
  "-DDISCOVERY=192.168.77.85 " +
  "-Dcassandra.hosts=192.168.77.85,192.168.77.42 "
)

addCommandAlias(
  "engine-1",
  "server/runMain linguistic.Application " +
  "-DENV=development " +
  "-DCONFIG=./server/conf " +
  "-Dakka.remote.netty.tcp.port=2552 " +
  "-Dakka.http.port=9443 " +
  "-DHOSTNAME=192.168.77.10 " +
  "-Dakka.cluster.roles.0=linguistic-engine " +
  "-DDISCOVERY=192.168.77.85 " +
  "-Dcassandra.hosts=192.168.77.85,192.168.77.42 "
)

addCommandAlias(
  "engine-2",
  "runMain linguistic.Application " +
  "-DENV=development " +
  "-DCONFIG=./server/conf " +
  "-Dakka.remote.netty.tcp.port=2551 " +
  "-Dakka.http.port=9443 " +
  "-DHOSTNAME=185.143.173.41 " +
  "-Dakka.cluster.roles.0=linguistic-engine " +
  "-DDISCOVERY=192.168.77.85 " +
  "-Dcassandra.hosts=192.168.77.85,192.168.77.42 " +
  "-Dakka.cluster.seed-nodes.0=akka.tcp://linguistics@78.155.207.122:2551 " +
  "-Dakka.cluster.seed-nodes.1=akka.tcp://linguistics@78.155.207.123:2551 "
)