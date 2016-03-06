package net.ruippeixotog.scalascraper.util

object ProxyUtils {

  private[this] val HTTP_PROXY_HOST: String = "http.proxyHost"
  private[this] val HTTP_PROXY_PORT: String = "http.proxyPort"
  private[this] val HTTPS_PROXY_PORT: String = "https.proxyPort"
  private[this] val HTTPS_PROXY_HOST: String = "https.proxyHost"

  /**
    * Sets the JVM-wide HTTP and HTTPS proxy configuration.
    * @param host the proxy host
    * @param port the proxy port
    */
  def setProxy(host: String, port: Int): Unit = {
    System.setProperty(HTTP_PROXY_HOST, host)
    System.setProperty(HTTP_PROXY_PORT, String.valueOf(port))
    System.setProperty(HTTPS_PROXY_HOST, host)
    System.setProperty(HTTPS_PROXY_PORT, String.valueOf(port))
  }

  /**
    * Returns the current JVM-wide HTTP and HTTPS proxy configuration.
    * @return the current JVM-wide HTTP and HTTPS proxy configuration.
    */
  def getProxy: Option[(String, Int)] = {
    for {
      host <- Option(System.getProperty(HTTP_PROXY_HOST))
      port <- Option(System.getProperty(HTTP_PROXY_PORT))
    } yield (host, port.toInt)
  }

  /**
    * Unsets the JVM-wide HTTP and HTTPS proxy configuration.
    */
  def removeProxy(): Unit = {
    System.clearProperty(HTTP_PROXY_HOST)
    System.clearProperty(HTTP_PROXY_PORT)
    System.clearProperty(HTTPS_PROXY_HOST)
    System.clearProperty(HTTPS_PROXY_PORT)
  }
}
