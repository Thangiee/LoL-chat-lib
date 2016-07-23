package lolchat.model

import lolchat.util.EventStream
import io.dylemma.frp.{EventSource, Observer}
import lolchat.data.Region

case class Session(user: String, passwd: String, region: Region, acceptFriendRequest: Boolean = false) {
  private[lolchat] val obs = new Observer {}

  private[lolchat] val msgEventSrc = EventSource[Msg]()
  val msgStream: EventStream[Msg] = EventStream(this, msgEventSrc)

  private[lolchat] val friendListEventSrc = EventSource[FriendListEvent]()
  val friendListStream: EventStream[FriendListEvent] = EventStream(this, friendListEventSrc)

  private[lolchat] val connectionEventSrc = EventSource[ConnectionEvent]()
  val connectionEventStream: EventStream[ConnectionEvent] = EventStream(this, connectionEventSrc)
}
