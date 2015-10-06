package net.ruippeixotog.scalascraper.util

object ProxyUtils {

  /**
   * Set HTTP and HTTPS proxy JVM-wide
   */
  def setProxy(host: String, port: Int): Unit = setProxy(Some(host), Some(port))

  /**
   * Remove HTTP and HTTPS proxy configuration JVM-wide
   */
  def removeProxy(): Unit = setProxy(None, None)

  /**
   * Implicit conversions to String or Java null
   */
  private implicit def optionStrToJavaStr(value: Option[String]): String = value.getOrElse(null)

  private implicit def optionIntToJavaStr(value: Option[Int]): String = value match {
    case Some(n) => String.valueOf(n)
    case None => null
  }

  private def setProxy(host: Option[String], port: Option[Int]): Unit = {
    System.setProperty("http.proxyHost", host)
    System.setProperty("http.proxyPort", port)
    System.setProperty("https.proxyHost", host)
    System.setProperty("https.proxyPort", port)
  }

}
