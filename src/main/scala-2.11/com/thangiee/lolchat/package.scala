package com.thangiee

package object lolchat {

  private[thangiee] def parseIdFromAddr(addr: String): Option[String] = "[0-9]+".r.findFirstIn(addr)

}
