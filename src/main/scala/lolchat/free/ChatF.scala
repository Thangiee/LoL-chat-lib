package lolchat.free

import lolchat.model._

import scala.language.higherKinds

sealed trait ChatF[A]
object ChatF {
  case class Login(session: Session) extends ChatF[Unit]
  case class ChangeAppearance(session: Session, appearance: Appearance) extends ChatF[Unit]
  case class Friends(session: Session) extends ChatF[Vector[Friend]]
  case class SendMsg(session: Session, toId: String, txt: String) extends ChatF[Unit]
  case class SendFriendReq(sess: Session, id: String) extends ChatF[Unit]
  case class RemoveFriend(sess: Session, id: String) extends ChatF[Unit]
  case class GroupNames(sess: Session) extends ChatF[Vector[String]]
  case class CreateFriendGroup(sess: Session, groupName: String) extends ChatF[Unit]
  case class MoveFriendToGroup(sess: Session, friend: Friend, group: String) extends ChatF[Unit]
  case class GetProfile(sess: Session) extends ChatF[Profile]
  case class UpdateProfile(sess: Session, profile: Profile) extends ChatF[Unit]
}