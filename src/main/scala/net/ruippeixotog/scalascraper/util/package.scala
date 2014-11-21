package net.ruippeixotog.scalascraper

package object util {
  type Validated[+R, +A] = Either[R, A]
}
