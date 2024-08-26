package linguistic

import java.security._
import java.io.{File, FileInputStream, InputStream}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}
import akka.pki.pem.{DERPrivateKeyLoader, PEMDecoder}

import java.security.cert.{Certificate, CertificateFactory}
import java.security.interfaces.RSAPrivateCrtKey
import java.util.Base64
import scala.io.Source
import scala.util.{Failure, Success, Try}

trait SslSupport {

  private val algorithm = "SunX509"

  /*private def create(in: InputStream, keyPass: String, storePass: String): HttpsConnectionContext = {
    val keyStore = KeyStore.getInstance("JKS")
    keyStore.load(in, storePass.toCharArray)

    val keyManagerFactory = KeyManagerFactory.getInstance(algorithm)
    keyManagerFactory.init(keyStore, keyPass.toCharArray)

    val tmf = TrustManagerFactory.getInstance(algorithm)
    tmf.init(keyStore)

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom())

    ConnectionContext.httpsServer(sslContext)
  }*/

  private def create(in: InputStream, keyPass: String, storePass: String): Try[HttpsConnectionContext] =
    Try {
      val ks = KeyStore.getInstance("PKCS12")
      ks.load(in, storePass.toCharArray /*new Array[Char](0)*/ )

      val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      keyManagerFactory.init(ks, keyPass.toCharArray /*Array[Char]()*/ )

      val sslContext = SSLContext.getInstance("TLS")
      sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom())
      ConnectionContext.httpsServer(sslContext)
    }

  def base64Encode(bs: Array[Byte]): String =
    new String(Base64.getUrlEncoder.withoutPadding.encode(bs))

  private def fsa(in: InputStream, keyPass: String, storePass: String): Try[HttpsConnectionContext] =
    Try {

      val privateKey = DERPrivateKeyLoader.load(PEMDecoder.decode(Source.fromResource("/fsa/privkey.pem").mkString))

      val ks       = KeyStore.getInstance("PKCS12")
      val password = "gsfh3@vnoJZihnMsfhLoe2xN234RujuGmhokGjEVx".toCharArray
      ks.load(in, password)

      val as = ks.aliases().asIterator()
      while (as.hasNext)
        println("Alias: " + as.next())

      /*val extractedPrivKey = ks.getKey(alias, password).asInstanceOf[RSAPrivateCrtKey]
      println("PK: " + base64Encode(extractedPrivKey.getEncoded))
      val cert = ks.getCertificate(alias)*/

      val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      keyManagerFactory.init(ks, password)

      val sslContext = SSLContext.getInstance("TLS")
      sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom())
      ConnectionContext.httpsServer(sslContext)
    }

  private def selfSign(in: InputStream): Try[HttpsConnectionContext] =
    Try {
      val ks       = KeyStore.getInstance("PKCS12")
      val password = Array[Char]()
      ks.load(in, password)

      val alias = "private"
      val as    = ks.aliases().asIterator()
      while (as.hasNext)
        println("Alias: " + as.next())

      val extractedPrivKey = ks.getKey(alias, password).asInstanceOf[RSAPrivateCrtKey]
      println("PK: " + base64Encode(extractedPrivKey.getEncoded))
      val cert = ks.getCertificate(alias)

      val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      keyManagerFactory.init(ks, password)

      val sslContext = SSLContext.getInstance("TLS")
      sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom())
      ConnectionContext.httpsServer(sslContext)
    }

  //

  /*def https(keyPass: String, storePass: String): HttpsConnectionContext = {
    val file = new File("./linguistic.jks")
    if (file.exists) {
      create(new FileInputStream(file), keyPass, storePass)
    } else {
      resource
        // .managed(getClass.getResourceAsStream("/linguistic.jks"))
        // .managed(getClass.getResourceAsStream("/self-sign-cert.jks"))
        .managed(getClass.getResourceAsStream("/fsa.jks"))
        .map(in => create(in, keyPass, storePass))
        .opt
        .fold(throw new Exception("jks error"))(identity)
    }
  }*/

  def httpsSelfSign(): HttpsConnectionContext =
    resource
      .managed(getClass.getResourceAsStream("/self-sign-cert.jks"))
      .map { in =>
        // fsa(in, keyPass, storePass) match {
        selfSign(in) match {
          case Failure(ex) =>
            ex.printStackTrace()
            throw ex
          case Success(ctx) =>
            ctx
        }
      }
      .opt
      .fold(throw new Exception("jks error"))(identity)

  def httpsFsa(): HttpsConnectionContext = {
    val privateKey =
      DERPrivateKeyLoader.load(PEMDecoder.decode(Source.fromResource("fsa/privkey.pem").mkString))
    val cf  = CertificateFactory.getInstance("X.509")
    val cer = cf.generateCertificate(classOf[SslSupport].getResourceAsStream("/fsa/fullchain.pem"))
    println(s"$cer")

    val ks = KeyStore.getInstance("PKCS12")
    ks.load(null)
    ks.setKeyEntry(
      "fsa",
      privateKey,
      Array[Char](),
      Array[Certificate](cer)
    )

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, Array[Char]())

    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom())
    ConnectionContext.httpsServer(context)
  }

}
