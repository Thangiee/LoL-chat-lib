package lolchat

import cats.free.Free
import lolchat.free.ChatF
import lolchat.free.ChatF._
import lolchat.model._

trait ops {

  def pure[A](a: A): ChatOp[A] = ChatOp(_ => Free.pure(a))

  def login: ChatOp[Unit] = ChatOp(sess => Free.liftF(ChatF.Login(sess)))

  def offlineLogin: ChatOp[Unit] = for {_ <- login; _ <- appearOffline } yield ()

  def appearOnline: ChatOp[Unit] = ChatOp(sess => Free.liftF(ChatF.ChangeAppearance(sess, Online)))

  def appearOffline: ChatOp[Unit] = ChatOp(sess => Free.liftF(ChatF.ChangeAppearance(sess, Offline)))

  def appearAway: ChatOp[Unit] = ChatOp(sess => Free.liftF(ChatF.ChangeAppearance(sess, Away)))

  def friends: ChatOp[Vector[Friend]] = ChatOp(sess => Free.liftF(ChatF.Friends(sess)))

  def onlineFriends: ChatOp[Vector[Friend]] = friends.map(_.filter(_.isOnline))

  def friendByName(inGameName: String): ChatOp[Option[Friend]] =
    friends.map(_.find(_.name.toLowerCase == inGameName.toLowerCase))

  def friendById(id: String): ChatOp[Option[Friend]] = friends.map(_.find(_.id == id))

  def sendMsg(toId: String, txt: String): ChatOp[Unit] = ChatOp(sess => Free.liftF(ChatF.SendMsg(sess, toId, txt)))

  def sendMsgToFriend(friendName: String, txt: String): ChatOp[Option[ErrMsg]] =
    friendByName(friendName).flatMap {
      case Some(f) => sendMsg(f.id, txt).map(_ => None)
      case None    => pure(Some(s"$friendName not in your friends list."))
    }

  def sendFriendRequest(id: String): ChatOp[Unit] = ChatOp(sess => Free.liftF(SendFriendReq(sess, id)))

  def removeFriend(id: String): ChatOp[Unit] = ChatOp(sess => Free.liftF(RemoveFriend(sess, id)))

  def friendGroupsName: ChatOp[Vector[String]] = ChatOp(sess => Free.liftF(GroupNames(sess)))

  def createFriendGroup(name: String): ChatOp[Unit] = ChatOp(sess => Free.liftF(CreateFriendGroup(sess, name)))

  def moveFriendToGroup(friendName: String, group: String): ChatOp[Option[ErrMsg]] =
    for {
      friend <- friendByName(friendName)
      _      <- if (friend.isDefined) createFriendGroup(group) else pure()
      result <- friend match {
        case Some(f) => ChatOp(sess => Free.liftF(MoveFriendToGroup(sess, f, group))).map(_ => None)
        case None    => pure(Some(s"$friendName not in your friends list."))
      }
    } yield result

  def getProfile: ChatOp[Profile] = ChatOp(sess => Free.liftF(GetProfile(sess)))

  def setProfile(profile: Profile): ChatOp[Unit] = ChatOp(sess => Free.liftF(UpdateProfile(sess, profile)))

  def modifyProfile(f: Profile => Profile): ChatOp[Unit] =
    for {
      profile <- getProfile
      res     <- setProfile(f(profile))
    } yield res

}

object ops extends ops
