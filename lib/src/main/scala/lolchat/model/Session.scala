package lolchat.model

import cats.data.OptionT
import lolchat.data.Region
import rx._

case class Session(user: String, passwd: String, region: Region, acceptFriendRequest: Boolean = false) {
  private[lolchat] val msgEventSrc = Var[Option[Msg]](None)
  def msgStream(implicit ctx: Ctx.Owner) = OptionT[Rx, Msg](Rx(msgEventSrc()))

  private[lolchat] val friendListEventSrc = Var[Option[FriendListEvent]](None)
  def friendListStream(implicit ctx: Ctx.Owner) = OptionT[Rx, FriendListEvent](Rx(friendListEventSrc()))

  private[lolchat] val connectionEventSrc = Var[Option[ConnectionEvent]](None)
  def connectionEventStream(implicit ctx: Ctx.Owner)= OptionT[Rx, ConnectionEvent](Rx(connectionEventSrc()))
}
