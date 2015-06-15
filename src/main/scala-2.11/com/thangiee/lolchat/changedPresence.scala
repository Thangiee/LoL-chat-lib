package com.thangiee.lolchat

object changedPresence {
  sealed trait ChangedPresence
  case object Available extends ChangedPresence
  case object AFK extends ChangedPresence
  case object Playing extends ChangedPresence
  case object Login extends ChangedPresence
  case object Logout extends ChangedPresence
  case object StatusMsg extends ChangedPresence
}
