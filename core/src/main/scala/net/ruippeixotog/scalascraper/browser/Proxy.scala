package net.ruippeixotog.scalascraper.browser

/** A proxy configuration to be used by `Browser`s.
  *
  * @param host
  *   the proxy host
  * @param port
  *   the proxy port
  * @param proxyType
  *   the protocol used by a proxy (e.g. HTTP, SOCKS)
  */
case class Proxy(host: String, port: Int, proxyType: Proxy.Type)

object Proxy {

  /** The protocol used by a proxy (e.g. HTTP, SOCKS).
    */
  sealed trait Type
  case object HTTP extends Type
  case object SOCKS extends Type
}
