package linguistic.dao

import akka.actor.{ActorSystem, ClassicActorSystemProvider, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.dispatch.MessageDispatcher
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession
import com.datastax.oss.driver.api.core.metadata.EndPoint
import com.datastax.oss.driver.api.core.{CqlIdentifier, CqlSession}
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint

import java.net.InetSocketAddress
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.{asJavaIterableConverter, collectionAsScalaIterableConverter}

object CassandraSessionExtension extends ExtensionId[CassandraSessionExtension] with ExtensionIdProvider {

  override def get(system: ActorSystem): CassandraSessionExtension = super.get(system)

  override def get(system: ClassicActorSystemProvider): CassandraSessionExtension = super.get(system)

  override def lookup: CassandraSessionExtension.type = CassandraSessionExtension

  override def createExtension(system: ExtendedActorSystem): CassandraSessionExtension =
    new CassandraSessionExtension(system)
}

class CassandraSessionExtension(system: ActorSystem) extends Extension {
  val keyspace = system.settings.config.getString("akka.persistence.cassandra.journal.keyspace")
  val conf = system.settings.config
  
  val cassandraHosts =
    conf
      .getStringList("datastax-java-driver.basic.contact-points")
      //.asScala.map(new InetSocketAddress(_)).head
      .asScala.map(new InetSocketAddress(_, 9042))

  /*
      val cluster = Cluster
        .builder()
        .addContactPointsWithPorts(cassandraHosts.asJava)
        /*.withCredentials(
          conf.getString("cassandra-journal.authentication.username"),
          conf.getString("cassandra-journal.authentication.password")
        )*/
        //.withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder().withLocalDc(localDC).build())
        .withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.ONE))
        .build

      val session = retry(() => (cluster connect keySpace), 5)

      cluster.getConfiguration().getCodecRegistry().register(InstantCodec.instance)
      session
        .executeDDL(
          s"CREATE TABLE IF NOT EXISTS ${keySpace}.users(login text, created_at timestamp, password text, photo text, PRIMARY KEY (login))"
        )
        .one()
     */
  //new DefaultEndPoint(???)


  ////CREATE KEYSPACE IF NOT EXISTS linguistics WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 3 };
  //select * from linguistics.linguistics_journal where persistence_id='/sharding/wordsShard/z' and partition_nr=0;
  val session: CqlSession = {
    val s =
      CqlSession
        .builder()
        //.addContactEndPoints(cassandraHost)
        //.addContactPoint(cassandraHost)
        /*.withCredentials(
          conf.getString("cassandra-journal.authentication.username"),
          conf.getString("cassandra-journal.authentication.password")
        )*/
        .withKeyspace(CqlIdentifier.fromCql(keyspace))
        .build()

    s.execute(
      s"CREATE TABLE IF NOT EXISTS ${keyspace}.users(login text, created_at timestamp, password text, photo text, PRIMARY KEY (login))"
      )

    s

  }
}
