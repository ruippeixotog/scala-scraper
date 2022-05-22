package net.ruippeixotog.scalascraper

import java.io.Closeable

package object util {
  def using[A <: Closeable, R](closeable: A)(f: A => R): R =
    try f(closeable)
    finally closeable.close()
}
