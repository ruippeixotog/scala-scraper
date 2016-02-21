package net.ruippeixotog.scalascraper.browser

import java.io.File

import net.ruippeixotog.scalascraper.model.Document

trait Browser {

  def get(url: String): Document
  def post(url: String, form: Map[String, String]): Document

  def parseFile(path: String, charset: String = "UTF-8"): Document
  def parseFile(file: File): Document
  def parseFile(file: File, charset: String): Document

  def parseString(html: String): Document
}
