package com.thangiee.lolchat

object chatMode {
  sealed trait ChatMode
  case object Chat extends ChatMode // green circle
  case object Away extends ChatMode // red circle
  case object Busy extends ChatMode // yellow circle
}
