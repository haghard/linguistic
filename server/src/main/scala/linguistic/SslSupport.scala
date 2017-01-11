package linguistic

import java.io.{FileInputStream, FileReader, File}
import java.security._
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}

trait SslSupport {

  val algorithm = "SunX509"

  def https(keyPass: String, storePass: String): HttpsConnectionContext = {

    //new FileReader(new File("./linguistic.jks"))
    val f = new File("./linguistic.jks")
    if(f.exists()) {
      val in = new FileInputStream(new File("./linguistic.jks"))
      val keyStore = KeyStore.getInstance("JKS")
      keyStore.load(in, storePass.toCharArray)

      val keyManagerFactory = KeyManagerFactory.getInstance(algorithm)
      keyManagerFactory.init(keyStore, keyPass.toCharArray)

      val tmf = TrustManagerFactory.getInstance(algorithm)
      (tmf init keyStore)

      val sslContext = SSLContext.getInstance("TLS")
      sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)

      (ConnectionContext https sslContext)
    } else {
      resource.managed(getClass.getResourceAsStream("/linguistic.jks")).map { in =>

        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(in, storePass.toCharArray)

        val keyManagerFactory = KeyManagerFactory.getInstance(algorithm)
        keyManagerFactory.init(keyStore, keyPass.toCharArray)

        val tmf = TrustManagerFactory.getInstance(algorithm)
        (tmf init keyStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)

        (ConnectionContext https sslContext)
      }.opt.get
    }
  }
}