package net.ruippeixotog.scalascraper.browser

import java.io.{File, InputStream}

import net.ruippeixotog.scalascraper.model.Document

/** A client able to retrieve and parse HTML pages from the web and from local resources.
  *
  * An implementation of `Browser` can fetch pages via HTTP GET or POST requests, parse the downloaded page and return a
  * [[net.ruippeixotog.scalascraper.model.Document]] instance, which can be queried via the scraper DSL or using its
  * methods.
  *
  * Different [[net.ruippeixotog.scalascraper.browser.Browser]] implementations can embed pages with different runtime
  * behavior. For example, some browsers may limit themselves to parse the HTML content inside the page without
  * executing any scripts inside, while others may run JavaScript and allow for `Document` instances with dynamic
  * content. The documentation of each implementation should be read for more information on the semantics of its
  * `Document` and [[net.ruippeixotog.scalascraper.model.Element]] implementations.
  */
trait Browser {

  /** The concrete type of documents created by this browser.
    */
  type DocumentType <: Document

  /** The user agent used by this browser to retrieve HTML pages from the web.
    */
  def userAgent: String

  /** Retrieves and parses a web page using a GET request.
    *
    * @param url the URL of the page to retrieve
    * @return a `Document` containing the retrieved web page.
    */
  def get(url: String): DocumentType

  /** Submits a form via a POST request and parses the resulting page.
    *
    * @param url the URL of the page to retrieve
    * @param form a map containing the form fields to submit with their respective values
    * @return a `Document` containing the resulting web page.
    */
  def post(url: String, form: Map[String, String]): DocumentType

  /** Parses a local HTML file with a specified charset.
    *
    * @param file the HTML file to parse
    * @param charset the charset of the file
    * @return a `Document` containing the parsed web page.
    */
  def parseFile(file: File, charset: String): DocumentType

  /** Parses a local HTML file encoded in UTF-8.
    *
    * @param file the HTML file to parse
    * @return a `Document` containing the parsed web page.
    */
  def parseFile(file: File): DocumentType = parseFile(file, "UTF-8")

  /** Parses a local HTML file with a specified charset.
    *
    * @param path the path in the local filesystem where the HTML file is located
    * @param charset the charset of the file
    * @return a `Document` containing the parsed web page.
    */
  def parseFile(path: String, charset: String): DocumentType = parseFile(new File(path), charset)

  /** Parses a local HTML file encoded in UTF-8.
    *
    * @param path the path in the local filesystem where the HTML file is located
    * @return a `Document` containing the parsed web page.
    */
  def parseFile(path: String): DocumentType = parseFile(new File(path), "UTF-8")

  /** Parses a resource with a specified charset.
    *
    * @param name the name of the resource to parse
    * @param charset the charset of the resource
    * @return a `Document` containing the parsed web page.
    */
  def parseResource(name: String, charset: String = "UTF-8"): DocumentType =
    parseInputStream(getClass.getResourceAsStream(name), charset)

  /** Parses an input stream with its content in a specified charset. The provided input stream is always closed before
    * this method returns or throws an exception.
    *
    * @param inputStream the input stream to parse
    * @param charset the charset of the input stream content
    * @return a `Document` containing the parsed web page.
    */
  def parseInputStream(inputStream: InputStream, charset: String = "UTF-8"): DocumentType

  /** Parses an HTML string.
    *
    * @param html the HTML string to parse
    * @return a `Document` containing the parsed web page.
    */
  def parseString(html: String): DocumentType

  /** Returns the current set of cookies stored in this browser for a given URL.
    *
    * @param url the URL whose stored cookies are to be returned
    * @return a mapping of cookie names to their respective values.
    */
  def cookies(url: String): Map[String, String]

  /** Clears the cookie store of this browser.
    */
  def clearCookies(): Unit
}
