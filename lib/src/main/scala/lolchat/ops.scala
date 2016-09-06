package lolchat

import cats.free.Free
import lolchat.free.Chat.{all => chatF}
import lolchat.model._

trait ops {

  def pure[A](a: A): ChatOp[A] = ChatOp(_ => Free.pure(a))

  def isLogin: ChatOp[Boolean] = ChatOp(sess => chatF.isLogin(sess))

  def login: ChatOp[Unit] = ChatOp(sess => chatF.login(sess))
  
  def logout: ChatOp[Unit] = ChatOp(sess => chatF.logout(sess))
  
  def offlineLogin: ChatOp[Unit] = for {_ <- login; _ <- appearOffline } yield ()
  
  def appearOnline: ChatOp[Unit] = ChatOp(sess => chatF.changeAppearance(sess, Online))
  
  def appearOffline: ChatOp[Unit] = ChatOp(sess => chatF.changeAppearance(sess, Offline))
  
  def appearAway: ChatOp[Unit] = ChatOp(sess => chatF.changeAppearance(sess, Away))

  def getAppearance: ChatOp[Appearance] = ChatOp(sess => chatF.getAppearance(sess))

  def friends: ChatOp[Vector[Friend]] = ChatOp(sess => chatF.friends(sess))

  def onlineFriends: ChatOp[Vector[Friend]] = friends.map(_.filter(_.isOnline))

  def friendByName(inGameName: String): ChatOp[Option[Friend]] =
    friends.map(_.find(_.name.toLowerCase == inGameName.toLowerCase))

  def friendById(id: String): ChatOp[Option[Friend]] = friends.map(_.find(_.id == id))

  def sendMsg(toId: String, txt: String): ChatOp[Unit] = ChatOp(sess => chatF.sendMsg(sess, toId, txt))

  def sendMsgToFriend(friendName: String, txt: String): ChatOp[Option[ErrMsg]] =
    friendByName(friendName).flatMap {
      case Some(f) => sendMsg(f.id, txt).map(_ => None)
      case None    => pure(Some(s"$friendName not in your friends list."))
    }

  def sendFriendRequest(id: String): ChatOp[Unit] = ChatOp(sess => chatF.sendFriendRequest(sess, id))

  def removeFriend(id: String): ChatOp[Unit] = ChatOp(sess => chatF.removeFriend(sess, id))

  def groupNames: ChatOp[Vector[String]] = ChatOp(sess => chatF.groupNames(sess))

  def createGroup(name: String): ChatOp[Unit] = ChatOp(sess => chatF.createGroup(sess, name))

  def moveFriendToGroup(friendName: String, group: String): ChatOp[Option[ErrMsg]] =
    for {
      friend <- friendByName(friendName)
      _      <- if (friend.isDefined) createGroup(group) else pure()
      result <- friend match {
        case Some(f) => ChatOp(sess => chatF.moveFriendToGroup(sess, f, group)).map(_ => None)
        case None    => pure(Some(s"$friendName not in your friends list."))
      }
    } yield result

  def getProfile: ChatOp[Profile] = ChatOp(sess => chatF.getProfile(sess))

  def setProfile(profile: Profile): ChatOp[Unit] = ChatOp(sess => chatF.updateProfile(sess, profile))

  def modifyProfile(f: Profile => Profile): ChatOp[Profile] =
    for {
      profile <- getProfile
      updatedProfile = f(profile)
      _       <- setProfile(updatedProfile)
    } yield updatedProfile

}

object ops extends ops
