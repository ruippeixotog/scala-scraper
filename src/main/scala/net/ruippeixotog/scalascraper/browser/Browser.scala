package net.ruippeixotog.scalascraper.browser

import java.io.{ File, InputStream }

import net.ruippeixotog.scalascraper.model.Document

trait Browser {

  /**
    * The type of documents created by this browser.
    */
  type DocumentType <: Document

  def userAgent: String

  def get(url: String): DocumentType
  def post(url: String, form: Map[String, String]): DocumentType

  def parseFile(file: File, charset: String): DocumentType
  def parseFile(file: File): DocumentType = parseFile(file, "UTF-8")
  def parseFile(path: String, charset: String): DocumentType = parseFile(new File(path), charset)
  def parseFile(path: String): DocumentType = parseFile(new File(path), "UTF-8")

  def parseResource(name: String, charset: String = "UTF-8"): DocumentType =
    parseInputStream(getClass.getResourceAsStream(name), charset)

  /** Closes the InputStream after reading */
  def parseInputStream(inputStream: InputStream, charset: String = "UTF-8"): DocumentType

  def parseString(html: String): DocumentType

  def cookies(url: String): Map[String, String]
  def clearCookies(): Unit
}
