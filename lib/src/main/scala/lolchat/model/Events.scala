package lolchat.model

sealed trait FriendListEvent
case class FriendRequest(fromId: String) extends FriendListEvent
case class FriendAdded(id: String) extends FriendListEvent
case class FriendRemoved(id: String) extends FriendListEvent
case class FriendUpdated(friend: Friend) extends FriendListEvent

sealed trait ConnectionEvent
case object ConnectionLost extends ConnectionEvent
case object Reconnected extends ConnectionEvent
case object ReconnectFailed extends ConnectionEvent
case class ReconnectIn(sec: Int) extends ConnectionEvent

case class Msg(fromId: String, txt: String)