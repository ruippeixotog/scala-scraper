package net.ruippeixotog.scalascraper

import java.net.{ ConnectException, Socket, SocketException }

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

trait SocksTestHelper extends ThrownMessages { this: Specification =>

  val socksProxyHost = Option(System.getenv("SOCKS_PROXY_HOST")).getOrElse("localhost")
  val socksProxyPort = Option(System.getenv("SOCKS_PROXY_PORT")).fold(9050)(_.toInt)

  def skipIfProxyUnavailable[A](block: => A): A = {
    try {
      new Socket(socksProxyHost, socksProxyPort).close() // test if port is open
      block
    } catch {
      case _: ConnectException | _: SocketException => skip("A SOCKS proxy is not available")
    }
  }
}
