package linguistic

import java.security._
import java.io.{InputStream, FileInputStream, File}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}

trait SslSupport {

  val algorithm = "SunX509"

  private def create(in: InputStream, keyPass: String, storePass: String) = {
    val keyStore = KeyStore.getInstance("JKS")
    keyStore.load(in, storePass.toCharArray)

    val keyManagerFactory = KeyManagerFactory.getInstance(algorithm)
    keyManagerFactory.init(keyStore, keyPass.toCharArray)

    val tmf = TrustManagerFactory.getInstance(algorithm)
    (tmf init keyStore)

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)

    (ConnectionContext https sslContext)
  }

  def https(keyPass: String, storePass: String): HttpsConnectionContext = {
    val file = new File("./linguistic.jks")
    if(file.exists) {
      create(new FileInputStream(file), keyPass, storePass)
    } else {
      resource.managed(getClass.getResourceAsStream("/linguistic.jks")).map { in =>
        create(in, keyPass, storePass)
      }.opt.fold(throw new Exception("jks hasn't been found"))(identity)
    }
  }
}