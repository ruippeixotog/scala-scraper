package net.ruippeixotog.scalascraper.util

object ProxyUtils {

  private val HTTP_PROXY_HOST: String = "http.proxyHost"
  private val HTTP_PROXY_PORT: String = "http.proxyPort"
  private val HTTPS_PROXY_PORT: String = "https.proxyPort"
  private val HTTPS_PROXY_HOST: String = "https.proxyHost"

  /**
   * Set HTTP and HTTPS proxy JVM-wide
   */
  def setProxy(host: String, port: Int): Unit = {
    System.setProperty(HTTP_PROXY_HOST, host)
    System.setProperty(HTTP_PROXY_PORT, String.valueOf(port))
    System.setProperty(HTTPS_PROXY_HOST, host)
    System.setProperty(HTTPS_PROXY_PORT, String.valueOf(port))
  }

  /**
   * Remove HTTP and HTTPS proxy configuration JVM-wide
   */
  def removeProxy(): Unit = {
    System.clearProperty(HTTP_PROXY_HOST)
    System.clearProperty(HTTP_PROXY_PORT)
    System.clearProperty(HTTPS_PROXY_HOST)
    System.clearProperty(HTTPS_PROXY_PORT)
  }

}
