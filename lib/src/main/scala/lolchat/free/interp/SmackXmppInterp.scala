package lolchat.free.interp

import java.util
import javax.net.ssl.SSLSocketFactory

import cats.data.Xor
import lolchat._
import lolchat.data._
import lolchat.free.ChatF
import lolchat.free.ChatF._
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

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.util.Try

object SmackXmppInterp extends ChatInterpreter[ChatResult] {
  private var sessions = Map.empty[Session, (XMPPTCPConnection, Presence)]

  def sessionCount: Int = sessions.size

  val interpreter: Interpreter = new Interpreter {
    def apply[A](fa: ChatF[A]): ChatResult[A] = fa match {
      case Login(sess)                            => login(sess)
      case Logout(sess)                           => logout(sess)
      case ChangeAppearance(sess, app)            => changeAppearance(sess, app)
      case Friends(sess)                          => getFriends(sess)
      case SendMsg(sess, toId, txt)               => sendMsg(sess, toId, txt)
      case SendFriendReq(sess, id)                => sendFriendReq(sess, id)
      case RemoveFriend(sess, id)                 => removeFriend(sess, id)
      case GroupNames(sess)                       => groupNames(sess)
      case CreateFriendGroup(sess, groupName)     => createFriendGroup(sess, groupName)
      case MoveFriendToGroup(sess, friend, group) => moveFriendToGroup(sess, friend, group)
      case GetProfile(sess)                       => getProfile(sess)
      case UpdateProfile(sess, profile)           => updateProfile(sess, profile)
    }
  }

  private def login(sess: Session): ChatResult[Unit] = {

    def attemptLogin(conn: XMPPTCPConnection): ChatResult[Unit] = {
      data.ChatResult(Xor.catchNonFatal {
        if (!conn.isConnected) conn.connect()
        if (!conn.isAuthenticated) conn.login()

        // Ignore "IllegalArgumentException: No enum constant org.jivesoftware.smack.packet.Presence.Mode.mobile"
        // cause by Smack parsing "mobile" (not defined by XMPP) for online friends on LoL official chat app.
        // More info: https://community.igniterealtime.org/thread/54798
        conn.setParsingExceptionCallback(new ParsingExceptionCallback {})

        Roster.getInstanceFor(conn).reloadAndWait()
      } leftMap {
        case e: SASLErrorException  => data.ChatError("Invalid username and/or password.", e)
        case e: NoResponseException => data.ChatError("No response from the server at the moment.", e)
        case e: ConnectionException => data.ChatError("Unable to connect to the server.", e);
        case e: Throwable           => data.ChatError("Unexpected error. See detail value for more info.", e)
      } leftMap { err =>
        conn.disconnect()
        err
      })
    }

    (sess.passwd.isEmpty || sess.passwd.isEmpty, sessions.get(sess)) match {
      case (true, _)            => data.ChatResult.left[Unit](data.ChatError("Username and Password can't be empty."))
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
              sess.friendListEventSrc.fire(FriendRequest(id))
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
              parseId(msg.getFrom).foreach(id => sess.msgEventSrc fire Msg(id, msg.getBody))))

        def setupFriendListEventStream(): Unit = {
          val roster: Roster = Roster.getInstanceFor(conn)
          roster.addRosterListener(new RosterListener {
            def entriesUpdated(addresses: util.Collection[String]): Unit = {}
            def entriesDeleted(addresses: util.Collection[String]): Unit =
              addresses.flatMap(parseId).foreach(id => sess.friendListEventSrc.fire(FriendRemoved(id)))
            def entriesAdded(addresses: util.Collection[String]): Unit =
              addresses.flatMap(parseId).foreach(id => sess.friendListEventSrc.fire(FriendAdded(id)))
            def presenceChanged(presence: Presence): Unit = {
              val entry = roster.getEntry(presence.getFrom)
              if (entry != null) {
                val f = mkFriend(entry, presence)
                sess.friendListEventSrc.fire(FriendUpdated(f))
              }
            }
          })
        }

        def setupConnectionEventStream(): Unit = {
          ReconnectionManager.getInstanceFor(conn).enableAutomaticReconnection()
          ReconnectionManager.getInstanceFor(conn).setReconnectionPolicy(ReconnectionPolicy.RANDOM_INCREASING_DELAY)

          conn.addConnectionListener(new ConnectionListener {
            def connected(connection: XMPPConnection): Unit = {}
            def reconnectionFailed(e: Exception): Unit = sess.connectionEventSrc.fire(ReconnectFailed)
            def reconnectionSuccessful(): Unit = sess.connectionEventSrc.fire(Reconnected)
            def authenticated(connection: XMPPConnection, resumed: Boolean): Unit = {}
            def connectionClosedOnError(e: Exception): Unit = sess.connectionEventSrc.fire(ConnectionLost)
            def connectionClosed(): Unit = {}
            def reconnectingIn(seconds: Int): Unit = sess.connectionEventSrc.fire(ReconnectIn(seconds))
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

  private def logout(sess: Session): ChatResult[Unit] =
    getConnection(sess) { conn =>
      ReconnectionManager.getInstanceFor(conn).disableAutomaticReconnection()
      conn.disconnect()
      sessions = sessions - sess
      data.ChatResult.right(())
    }

  private def changeAppearance(sess: Session, appearance: Appearance): ChatResult[Unit] =
    modifyPresence(sess) { presence =>
      appearance match {
        case Online  => presence.setType(Presence.Type.available); presence.setMode(Presence.Mode.chat)
        case Offline => presence.setType(Presence.Type.unavailable); presence.setMode(Presence.Mode.away)
        case Away => presence.setType(Presence.Type.available); presence.setMode(Presence.Mode.away)
      }
    }

  private def getFriends(sess: Session): ChatResult[Vector[Friend]] = getConnection(sess) { conn =>
    data.ChatResult.catchNonFatal {
      val r = Roster.getInstanceFor(conn)
      for { entry <- r.getEntries.toVector } yield {
        val presence = r.getPresence(entry.getUser)
        mkFriend(entry, presence)
      }
    }
  }

  private def sendMsg(sess: Session, toId: String, txt: String): ChatResult[Unit] = {
    val msg = new Message(s"sum$toId@pvp.net", Message.Type.chat)
    msg.setBody(txt)
    getConnection(sess)(conn => data.ChatResult.catchNonFatal(conn.sendStanza(msg)))
  }

  private def sendFriendReq(sess: Session, id: String): ChatResult[Unit] =
    sendPkt(sess, id, new Presence(Presence.Type.subscribe))

  private def removeFriend(sess: Session, id: String): ChatResult[Unit] =
    sendPkt(sess, id, new Presence(Presence.Type.unsubscribed))

  private def groupNames(sess: Session): ChatResult[Vector[String]] =
    getRoster(sess)(_.getGroups.map(_.getName).toVector)

  private def createFriendGroup(sess: Session, groupName: String): ChatResult[Unit] =
    getRoster(sess)(_.createGroup(groupName))

  private def moveFriendToGroup(sess: Session, friend: Friend, group: String): ChatResult[Unit] =
    getRoster(sess) { roster =>
      roster.getGroup(group).addEntry(roster.getEntry(s"sum${friend.id}@pvp.net")) //todo:
    }

  private def getProfile(sess: Session): ChatResult[Profile] =
    sessions.get(sess) match {
      case Some((_, presence)) => data.ChatResult.right(Profile.parseXML(presence.getStatus))
      case None => data.ChatResult.left[Profile](data.ChatError("Session not found. Try logging in first."))
    }

  private def updateProfile(sess: Session, profile: Profile): ChatResult[Unit] = {
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
      gameStartTime = parseStatus("timeStamp").map(_.toLong)
    )
  }

  private def sendPkt(sess: Session, id:String, pkt: Stanza): ChatResult[Unit] = {
    pkt.setTo(s"sum$id@pvp.net")
    getConnection(sess)(conn => data.ChatResult.catchNonFatal(conn.sendStanza(pkt)))
  }

  private def getRoster[A](sess: Session)(f: Roster => A): ChatResult[A] =
    getConnection(sess)(conn => data.ChatResult.catchNonFatal(f(Roster.getInstanceFor(conn))))

  private def modifyPresence(sess: Session)(modify: (Presence) => Unit): ChatResult[Unit] =
    sessions.get(sess) match {
      case Some((conn, p)) => modify(p); data.ChatResult.catchNonFatal(conn.sendStanza(p)).map { _ => sessions = sessions + (sess -> (conn, p)) }
      case None => data.ChatResult.left[Unit](data.ChatError("Session not found. Try logging in first."))
    }

  private def getConnection[A](sess: Session)(f: XMPPTCPConnection => ChatResult[A]): ChatResult[A] =
    sessions.get(sess) match {
      case Some((conn, _)) => f(conn)
      case None => data.ChatResult.left[A](data.ChatError("Session not found. Try logging in first."))
    }
}
