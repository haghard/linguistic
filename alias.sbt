
/*
export ENV=development
export CONFIG=./server/conf

export ENV=production
export CONFIG=./server/conf

*/

addCommandAlias("engine-0", "runMain linguistic.Application " +
  "-DENV=development " +
  "-Dakka.remote.netty.tcp.port=2551 " +
  "-Dakka.http.port=9443 " +
  "-Dakka.cluster.roles.0=linguistic-engine ")

addCommandAlias("engine-1", "runMain linguistic.Application " +
  "-Dakka.remote.netty.tcp.port=2552 " +
  "-Dakka.http.port=9001 " +
  "-DENV=development " +
  "-Dakka.cluster.roles.0=linguistic-engine ")

addCommandAlias("engine-2", "runMain linguistic.Application " +
  "-Dakka.remote.netty.tcp.port=2553 " +
  "-Dakka.http.port=9002 " +
  "-DENV=development " +
  "-Dakka.cluster.roles.0=linguistic-engine ")
