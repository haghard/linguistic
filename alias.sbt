
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
  "-DCONFIG=./server/conf " +
  "-Dakka.remote.netty.tcp.port=2551 " +
  "-Dakka.http.port=9443 " +
  "-DHOSTNAME=78.155.217.68 " +
  "-Dakka.cluster.roles.0=linguistic-engine " +
  "-DDISCOVERY=78.155.207.122 " +
  "-Dcassandra.hosts=78.155.207.122,78.155.218.24 " +
  "-Dcassandra-journal.contact-points.0=78.155.207.122 " +
  "-Dcassandra-journal.contact-points.1=78.155.218.24 " +
  "-Dcassandra-snapshot-store.contact-points.0=78.155.207.122 " +
  "-Dcassandra-snapshot-store.contact-points.1=78.155.218.24 "
)

addCommandAlias(
  "engine-1",
  "runMain linguistic.Application " +
  "-DENV=development " +
  "-DCONFIG=./server/conf " +
  "-Dakka.remote.netty.tcp.port=2552 " +
  "-Dakka.http.port=9444 " +
  "-DHOSTNAME=78.155.207.123 " +
  "-Dakka.cluster.roles.0=linguistic-engine " +
  "-DDISCOVERY=78.155.207.122 " +
  "-Dcassandra.hosts=78.155.207.122,78.155.218.24 " +
  "-Dcassandra-journal.contact-points.0=78.155.207.122 " +
  "-Dcassandra-journal.contact-points.1=78.155.218.24 " +
  "-Dcassandra-snapshot-store.contact-points.0=78.155.207.122 " +
  "-Dcassandra-snapshot-store.contact-points.1=78.155.218.24 "
)


addCommandAlias(
  "engine-3",
  "runMain linguistic.Application " +
  "-DENV=development " +
  "-DCONFIG=./server/conf " +
  "-Dakka.remote.netty.tcp.port=2554 " +
  "-Dakka.http.port=9446 " +
  "-DHOSTNAME=192.168.0.62 " +
  "-Dakka.cluster.roles.0=linguistic-engine " +
  "-DDISCOVERY=78.155.207.122 " +
  "-Dcassandra.hosts=78.155.207.122,78.155.218.24 " +
  "-Dcassandra-journal.contact-points.0=78.155.207.122 " +
  "-Dcassandra-journal.contact-points.1=78.155.218.24 " +
  "-Dcassandra-snapshot-store.contact-points.0=78.155.207.122 " +
  "-Dcassandra-snapshot-store.contact-points.1=78.155.218.24 "  +
  "-Dakka.cluster.seed-nodes.0=akka.tcp://linguistics@78.155.217.68:2551 " +
  "-Dakka.cluster.seed-nodes.1=akka.tcp://linguistics@78.155.217.68:2552 "
)