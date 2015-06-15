package com.thangiee

package object lolchat {
  type ErrMsg = String

  private[thangiee] def parseIdFromAddr(addr: String): Option[String] = "[0-9]+".r.findFirstIn(addr)

}
