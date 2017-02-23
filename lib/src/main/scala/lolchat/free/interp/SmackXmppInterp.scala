package lolchat.free.interp

import java.util
import javax.net.ssl.SSLSocketFactory

import lolchat._
import lolchat.data.{AsyncResult, _}
import lolchat.model._
import lolchat.util.parsing._
import org.jivesoftware.smack.ReconnectionManager.ReconnectionPolicy
import org.jivesoftware.smack.SmackException._
import org.jivesoftware.smack.chat.{ChatManager, Chat => SmackChat}
import org.jivesoftware.smack.packet.{Message, Presence, Stanza}
import org.jivesoftware.smack.parsing.ParsingExceptionCallback
import org.jivesoftware.smack.roster.Roster.SubscriptionMode
import org.jivesoftware.smack.roster.{Roster, RosterEntry, RosterListener}
import org.jivesoftware.smack.sasl.SASLErrorException
import org.jivesoftware.smack.tcp.{XMPPTCPConnection, XMPPTCPConnectionConfiguration}
import org.jivesoftware.smack.{ConnectionListener, ReconnectionManager, XMPPConnection}
import cats.syntax.all._

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.util.Try

object SmackXmppInterp extends free.Chat.Interp[AsyncResult] {

  private var sessions = Map.empty[Session, (XMPPTCPConnection, Presence)]

  def sessionCount: Int = sessions.size

  def findSession(p: Session => Boolean): Option[Session] = sessions.keys.find(p)

  def isLogin(sess: Session): AsyncResult[Boolean] =
    getConnection(sess)(conn => AsyncResult.right(conn.isConnected && conn.isAuthenticated))

  def login(sess: Session): AsyncResult[Unit] = {
    def attemptLogin(conn: XMPPTCPConnection): AsyncResult[Unit] = { // todo: exception on blank username and filled passwd
      AsyncResult(Either.catchNonFatal {
        if (!conn.isConnected) conn.connect()
        if (!conn.isAuthenticated) conn.login()

        // Ignore "IllegalArgumentException: No enum constant org.jivesoftware.smack.packet.Presence.Mode.mobile"
        // cause by Smack parsing "mobile" (not defined by XMPP) for online friends on LoL official chat app.
        // More info: https://community.igniterealtime.org/thread/54798
        conn.setParsingExceptionCallback(new ParsingExceptionCallback {})

        Roster.getInstanceFor(conn).reloadAndWait()
      } leftMap {
        case e: SASLErrorException  => Error(401, "Invalid username and/or password.", e)
        case e: NoResponseException => Error(503, "No response from the server at the moment.", e)
        case e: ConnectionException => Error(503, "Unable to connect to the server.", e);
        case e: Throwable           => Error(500, "Unexpected error. See detail value for more info.", e)
      } leftMap { err =>
        conn.disconnect()
        err
      })
    }

    (sess.user.isEmpty || sess.passwd.isEmpty, sessions.get(sess)) match {
      case (true, _)            => AsyncResult.left[Unit](Error(400, "Username and Password can't be empty."))
      case (_, Some((conn, _))) => attemptLogin(conn) // session exists already, login again only if need be
      case (false, None)        =>                    // login for new session
        val config = XMPPTCPConnectionConfiguration.builder()
          .setHost(sess.region.url)
          .setPort(5223)
          .setServiceName("pvp.net")
          .setSocketFactory(SSLSocketFactory.getDefault)
          .setUsernameAndPassword(sess.user, "AIR_" + sess.passwd)
          .setConnectTimeout(7.seconds.toMillis.toInt)
          .build()

        val conn = new XMPPTCPConnection(config)

        def setupFriendRequestHandler(): Unit = {
          Roster.getInstanceFor(conn).setSubscriptionMode(SubscriptionMode.manual)
          conn.addAsyncStanzaListener(
            (stanza: Stanza) => parseId(stanza.getFrom).foreach(id => {
              sess.friendListEventSrc.update(Some(FriendRequest(id)))
              val response = new Presence(if (sess.acceptFriendRequest) Presence.Type.subscribed else Presence.Type.unsubscribed)
              response.setTo(stanza.getFrom)
              conn.sendStanza(response)
            }),
            {
              // filter incoming friend request pkt for the listener above
              case p: Presence => p.getType == Presence.Type.subscribe
              case _ => false
            }
          )
        }

        def setupMsgStream(): Unit = ChatManager.getInstanceFor(conn)
          .addChatListener((chat: SmackChat, _) =>
            chat.addMessageListener((_, msg: Message) =>
              parseId(msg.getFrom).foreach(id => sess.msgEventSrc update Some(Msg(id, msg.getBody)))))

        def setupFriendListEventStream(): Unit = {
          val roster: Roster = Roster.getInstanceFor(conn)
          roster.addRosterListener(new RosterListener {
            def entriesUpdated(addresses: util.Collection[String]): Unit = {}
            def entriesDeleted(addresses: util.Collection[String]): Unit =
              addresses.flatMap(parseId).foreach(id => sess.friendListEventSrc.update(Some(FriendRemoved(id))))
            def entriesAdded(addresses: util.Collection[String]): Unit =
              addresses.flatMap(parseId).foreach(id => sess.friendListEventSrc.update(Some(FriendAdded(id))))
            def presenceChanged(presence: Presence): Unit = {
              val entry = roster.getEntry(presence.getFrom)
              if (entry != null) {
                val f = mkFriend(entry, presence)
                sess.friendListEventSrc.update(Some(FriendUpdated(f)))
              }
            }
          })
        }

        def setupConnectionEventStream(): Unit = {
          ReconnectionManager.getInstanceFor(conn).enableAutomaticReconnection()
          ReconnectionManager.getInstanceFor(conn).setReconnectionPolicy(ReconnectionPolicy.FIXED_DELAY)
          ReconnectionManager.getInstanceFor(conn).setFixedDelay(5)

          conn.addConnectionListener(new ConnectionListener {
            def connected(connection: XMPPConnection): Unit = {}
            def reconnectionFailed(e: Exception): Unit = sess.connectionEventSrc.update(Some(ReconnectFailed))
            def reconnectionSuccessful(): Unit = sess.connectionEventSrc.update(Some(Reconnected))
            def authenticated(connection: XMPPConnection, resumed: Boolean): Unit = {}
            def connectionClosedOnError(e: Exception): Unit = sess.connectionEventSrc.update(Some(ConnectionLost))
            def connectionClosed(): Unit = {}
            def reconnectingIn(seconds: Int): Unit = sess.connectionEventSrc.update(Some(ReconnectIn(seconds)))
          })
        }

        attemptLogin(conn).map(_ => {
          // after successful login
          setupFriendRequestHandler()
          setupMsgStream()
          setupFriendListEventStream()
          setupConnectionEventStream()
          sessions = sessions + (sess -> (conn, new Presence(Presence.Type.available, "", 1, Presence.Mode.chat)))
        })
    }
  }

  def logout(sess: Session): AsyncResult[Unit] =
    for {
      conn <- getConnection(sess)(AsyncResult.right(_))
      _    <- AsyncResult.catchNonFatal {
        ReconnectionManager.getInstanceFor(conn).disableAutomaticReconnection()
        conn.disconnect()
        sessions = sessions - sess
      }
    } yield ()

  def changeAppearance(sess: Session, appearance: Appearance): AsyncResult[Unit] =
    modifyPresence(sess) { presence =>
      appearance match {
        case Online  => presence.setType(Presence.Type.available); presence.setMode(Presence.Mode.chat)
        case Offline => presence.setType(Presence.Type.unavailable); presence.setMode(Presence.Mode.away)
        case Away => presence.setType(Presence.Type.available); presence.setMode(Presence.Mode.away)
      }
    }

  def getAppearance(sess: Session): AsyncResult[Appearance] =
    sessions.get(sess) match {
      case Some((_, presence)) =>
        val appearance = (presence.getType, presence.getMode) match {
          case (Presence.Type.available, Presence.Mode.chat) => Online
          case (Presence.Type.unavailable, _)                => Offline
          case (Presence.Type.available, Presence.Mode.away) => Away
          case _ => Offline
        }
        AsyncResult.right(appearance)
      case None => AsyncResult.left[Appearance](Error(401, "Session not found. Try logging in first."))
    }

  def friends(sess: Session): AsyncResult[Vector[Friend]] = getConnection(sess) { conn =>
    AsyncResult.catchNonFatal {
      val r = Roster.getInstanceFor(conn)
      for { entry <- r.getEntries.toVector } yield {
        val presence = r.getPresence(entry.getUser)
        mkFriend(entry, presence)
      }
    }
  }

  def sendMsg(sess: Session, toId: String, txt: String): AsyncResult[Unit] = {
    val msg = new Message(s"sum$toId@pvp.net", Message.Type.chat)
    msg.setBody(txt)
    getConnection(sess)(conn => AsyncResult.catchNonFatal(conn.sendStanza(msg)))
  }

  def sendFriendRequest(sess: Session, id: String): AsyncResult[Unit] =
    sendPkt(sess, id, new Presence(Presence.Type.subscribe))

  def removeFriend(sess: Session, id: String): AsyncResult[Unit] =
    sendPkt(sess, id, new Presence(Presence.Type.unsubscribed))

  def groupNames(sess: Session): AsyncResult[Vector[String]] =
    getRoster(sess)(_.getGroups.map(_.getName).toVector)

  def createGroup(sess: Session, name: String): AsyncResult[Unit] =
    getRoster(sess)(_.createGroup(name))

  def moveFriendToGroup(sess: Session, friend: Friend, group: String): AsyncResult[Unit] =
    getRoster(sess) { roster =>
      roster.getGroup(group).addEntry(roster.getEntry(s"sum${friend.id}@pvp.net")) //todo:
    }

  def getProfile(sess: Session): AsyncResult[Profile] = {
    sessions.get(sess) match {
      case Some((_, presence)) => AsyncResult.right(Profile.parseXML(presence.getStatus))
      case None => AsyncResult.left[Profile](Error(401, "Session not found. Try logging in first."))
    }
  }

  def updateProfile(sess: Session, profile: Profile): AsyncResult[Unit] = {
    modifyPresence(sess)(presence => {
      val status =
        s"""
           |<body>
           |<profileIcon>${profile.iconId}</profileIcon>
           |<level>${profile.level}</level>
           |<tier>${profile.tier}</tier>
           |<championMasteryScore>${profile.masteryScore}</championMasteryScore>
           |<rankedLeagueDivision>${profile.division}</rankedLeagueDivision>
           |<rankedLeagueTier>${profile.tier}</rankedLeagueTier>
           |<rankedLeagueQueue>RANKED_SOLO_5x5</rankedLeagueQueue>
           |<rankedWins>${profile.wins}</rankedWins>
           |<statusMsg>${profile.statusMsg}</statusMsg>
           |<gameStatus>outOfGame</gameStatus>
           |</body>
        """.stripMargin
      presence.setStatus(status)
    })
  }

  // ======================
  // Helper functions below
  // ======================
  private def mkFriend(entry: RosterEntry, presence: Presence): Friend = {
    val status = Try(presence.getStatus.replace("&apos;", "")).getOrElse("")
    val parseStatus: (String) => Option[String] = parseXml(status)(_)(identity)
    Friend(
      name = entry.getName,
      id = parseId(entry.getUser).getOrElse("-1"),
      chatMode = presence.getMode match {
        case Presence.Mode.chat => Chat
        case Presence.Mode.away => AFK
        case p                  => Busy
      },
      isOnline = !(presence.getType == Presence.Type.unavailable),
      groupName = entry.getGroups.map(_.getName).toVector,
      selectedChamp = parseStatus("skinname"),
      gameStatus = parseStatus("gameStatus"),
      level = parseStatus("level").map(_.toInt).getOrElse(0),
      wins = parseStatus("wins").map(_.filterNot(" ,".toSet).toInt).getOrElse(0),  // filter some tokens to avoid NumberFormatException (1000+ wins)
      statusMsg = parseStatus("statusMsg").getOrElse(""),
      rankedTier = parseStatus("rankedLeagueTier"),
      rankedDivision = parseStatus("rankedLeagueDivision"),
      leagueName = parseStatus("rankedLeagueName"),
      gameStartTime = parseStatus("timeStamp").map(_.toLong),
      profileIconId = parseStatus("profileIcon").map(_.toInt)
    )
  }

  private def sendPkt(sess: Session, id:String, pkt: Stanza): AsyncResult[Unit] = {
    pkt.setTo(s"sum$id@pvp.net")
    getConnection(sess)(conn => AsyncResult.catchNonFatal(conn.sendStanza(pkt)))
  }

  private def getRoster[A](sess: Session)(f: Roster => A): AsyncResult[A] =
    getConnection(sess)(conn => AsyncResult.catchNonFatal(f(Roster.getInstanceFor(conn))))

  private def modifyPresence(sess: Session)(modify: (Presence) => Unit): AsyncResult[Unit] =
    sessions.get(sess) match {
      case Some((conn, p)) => modify(p); AsyncResult.catchNonFatal(conn.sendStanza(p)).map { _ => sessions = sessions + (sess -> (conn, p)) }
      case None => AsyncResult.left[Unit](Error(401, "Session not found. Try logging in first."))
    }

  private def getConnection[A](sess: Session)(f: XMPPTCPConnection => AsyncResult[A]): AsyncResult[A] =
    sessions.get(sess) match {
      case Some((conn, _)) => f(conn)
      case None => AsyncResult.left[A](Error(401, "Session not found. Try logging in first."))
    }
}
