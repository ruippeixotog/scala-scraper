package net.ruippeixotog.scalascraper.util

object LineCompare {

  /*
     this function compare two strings (multi-line) and
     remove leading and trailing whitespaces from each line
     return true if they are equal, otherwise false
   */
  def compare(excepted: String, actual: String): Boolean = {
    trim(excepted).split("\n") zip trim(actual).split("\n") forall { case (e, a) =>
      trim(e) == trim(a)
    }
  }

  /*
     this function remove leading and tailing whitespaces , tabs, newlines from a string
   */
  private def trim(text: String): String = {
    text.replaceAll("^[\\s\\t\\n]+", "").replaceAll("[\\s\\t\\n]+$", "")
  }
}
