package lolchat.util

import scala.util.Try

object parsing {

  def parseId(txt: String): Option[String] = "[0-9]+".r.findFirstIn(txt)

  def parseXml[A](xml: String)(value: String)(as: String => A): Option[A] = {
    val pattern = s"(?<=$value>).*?(?=</$value)"
    pattern.r.findFirstIn(xml).flatMap(value => Try(as(value)).toOption)
  }

}
