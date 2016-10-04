package lolchat.free

import cats.free.Free
import lolchat.data._
import lolchat.model._

import scala.language.higherKinds

import freasymonad.cats.free

@free trait Chat {
  type ChatF[A] = Free[GrammarADT, A]
  sealed trait GrammarADT[A]

  def isLogin(sess: Session): ChatF[Boolean]
  def login(sess: Session): ChatF[Unit]
  def logout(sess: Session): ChatF[Unit]
  def changeAppearance(sess: Session, appearance: Appearance): ChatF[Unit]
  def getAppearance(sess: Session): ChatF[Appearance]
  def friends(sess: Session): ChatF[Vector[Friend]]
  def sendMsg(sess: Session, toId: String, txt: String): ChatF[Unit]
  def sendFriendRequest(sess: Session, id: String): ChatF[Unit]
  def removeFriend(sess: Session, id: String): ChatF[Unit]
  def groupNames(sess: Session): ChatF[Vector[String]]
  def createGroup(sess: Session, name: String): ChatF[Unit]
  def moveFriendToGroup(sess: Session, friend: Friend, group: String): ChatF[Unit]
  def getProfile(sess: Session): ChatF[Profile]
  def updateProfile(sess: Session, profile: Profile): ChatF[Unit]
}

