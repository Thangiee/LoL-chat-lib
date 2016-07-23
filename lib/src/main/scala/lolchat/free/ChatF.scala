package lolchat.free

import lolchat.model._

import scala.language.higherKinds

sealed trait ChatF[A]
object ChatF {
  case class IsLogin(sess: Session) extends ChatF[Boolean]
  case class Login(sess: Session) extends ChatF[Unit]
  case class Logout(sess: Session) extends ChatF[Unit]
  case class ChangeAppearance(sess: Session, appearance: Appearance) extends ChatF[Unit]
  case class GetAppearance(sess: Session) extends ChatF[Appearance]
  case class Friends(sess: Session) extends ChatF[Vector[Friend]]
  case class SendMsg(sess: Session, toId: String, txt: String) extends ChatF[Unit]
  case class SendFriendReq(sess: Session, id: String) extends ChatF[Unit]
  case class RemoveFriend(sess: Session, id: String) extends ChatF[Unit]
  case class GroupNames(sess: Session) extends ChatF[Vector[String]]
  case class CreateFriendGroup(sess: Session, groupName: String) extends ChatF[Unit]
  case class MoveFriendToGroup(sess: Session, friend: Friend, group: String) extends ChatF[Unit]
  case class GetProfile(sess: Session) extends ChatF[Profile]
  case class UpdateProfile(sess: Session, profile: Profile) extends ChatF[Unit]
}