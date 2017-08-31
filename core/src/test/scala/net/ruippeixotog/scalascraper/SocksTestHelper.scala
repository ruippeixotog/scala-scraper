package net.ruippeixotog.scalascraper

import java.net.{ ConnectException, Socket, SocketException }

import org.specs2.matcher.ThrownMessages
import org.specs2.mutable.Specification

// The tests wrapped by `skipIfProxyUnavailable` are executed only if an external SOCKS proxy server is running. By
// default it tries to connect to localhost:9050; if no service is listening in that endpoint, the test is skipped.
//
// The host and port of the SOCKS server can be changed by setting the TEST_SOCKS_PROXY_HOST and TEST_SOCKS_PROXY_PORT
// ports. Any proxy may be used, but tests usually expect the proxy server to execute the request from a different
// external IP (i.e. proxies running on the LAN doesn't work for testing purposes).
trait SocksTestHelper extends ThrownMessages { this: Specification =>

  val socksProxyHost = Option(System.getenv("TEST_SOCKS_PROXY_HOST")).getOrElse("localhost")
  val socksProxyPort = Option(System.getenv("TEST_SOCKS_PROXY_PORT")).fold(9050)(_.toInt)

  def skipIfProxyUnavailable[A](block: => A): A = {
    try {
      new Socket(socksProxyHost, socksProxyPort).close() // test if port is open
      block
    } catch {
      case _: ConnectException | _: SocketException => skip("A SOCKS proxy is not available")
    }
  }
}
