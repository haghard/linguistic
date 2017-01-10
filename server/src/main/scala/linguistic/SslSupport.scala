package linguistic

import java.security._
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}

trait SslSupport {

  def https(keypass: String, storepass: String): HttpsConnectionContext = {

    resource.managed(getClass.getResourceAsStream("/chatter.jks"))
      .map { in =>
        val algorithm = "SunX509"
        //PKCS12

        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(in, storepass.toCharArray)


        val keyManagerFactory = KeyManagerFactory.getInstance(algorithm)
        keyManagerFactory.init(keyStore, keypass.toCharArray)

        val tmf = TrustManagerFactory.getInstance(algorithm)
        (tmf init keyStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)

        (ConnectionContext https sslContext)
      }.opt.get
  }
}