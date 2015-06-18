package com.thangiee.lolchat

import java.util

import com.thangiee.lolchat.changedPresence._
import com.thangiee.lolchat.chatMode._
import com.thangiee.lolchat.error.{NotFound, NotConnected}
import com.thangiee.lolchat.region.Region
import org.jivesoftware.smack.chat.ChatManager
import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.packet.Presence.Mode._
import org.jivesoftware.smack.packet.Presence.Type.{available, _}
import org.jivesoftware.smack.packet.{Message, Presence, Stanza}
import org.jivesoftware.smack.roster.Roster.SubscriptionMode
import org.jivesoftware.smack.roster.{Roster, RosterListener}
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.{ReconnectionManager, StanzaListener}
import org.scalactic.OptionSugar._
import org.scalactic.TrySugar._
import org.scalactic._

import scala.collection.JavaConversions._
import scala.util.Try

/** A user session connected to Riot pvp chat server.
  *
  * This class is not meant to be created directly.
  * Use [[LoLChat#login]] to create a new session.
  *
  * @param user the username used to login
  * @param pass the password used to login
  * @param region the logged in server region
  * @param conn socket connection to an XMPP server
  */
class Session(val user: String, val pass: String, val region: Region, private[lolchat] val conn: XMPPTCPConnection) {
  type Id = String
  private var isOnlineSnapShot = Map[Id, Boolean]()
  private var modeSnapShot     = Map[Id, ChatMode]()
  private var statusSnapShot   = Map[Id, String]()

  private var friendListListener: Option[FriendListListener] = None

  private var presenceMode = chat
  private var presenceType = available

  private var _statusMsg     = "Using LoL Hangouts App"
  private var _profileIconId = 1
  private var _level         = 30
  private var _wins          = 1337
  private var _tier          = "CHALLENGER"
  private var _division      = "I"

  ReconnectionManager.getInstanceFor(conn).enableAutomaticReconnection()
  ReconnectionManager.getInstanceFor(conn).setFixedDelay(7)
  Roster.getInstanceFor(conn).setSubscriptionMode(SubscriptionMode.manual)

  // set up listener for handling received friend requests and accepting/rejecting them
  conn.addAsyncStanzaListener(
    new StanzaListener {
      def processPacket(stanza: Stanza): Unit =
        for {
          id <- parseIdFromAddr(stanza.getFrom)
          listener <- friendListListener
        } yield {
          val accept = listener.onReceivedFriendRequest(id) // accept friend request?
          val response = new Presence(if (accept) Presence.Type.subscribed else Presence.Type.unsubscribed)
          response.setTo(stanza.getFrom)
          conn.sendStanza(response)
        }
    },
    new StanzaFilter {
      // filter incoming friend request pkt for the listener above
      def accept(stanza: Stanza): Boolean = stanza match {
        case p: Presence => p.getType == Presence.Type.subscribe
        case _           => false
      }
    }
  )

  friends.foreach(updateSnapShots)
  Roster.getInstanceFor(conn).addRosterListener(new RosterListener {
    def entriesUpdated(addrs: util.Collection[String]): Unit = {}

    def entriesDeleted(addrs: util.Collection[String]): Unit =
      for ( listener <- friendListListener; id <- parseIdFromAddr(addrs.head) ) {listener.onFriendRemoved(id)}

    def entriesAdded(addrs: util.Collection[String]): Unit =
      for ( listener <- friendListListener; id <- parseIdFromAddr(addrs.head) ) {listener.onFriendAdded(id)}

    def presenceChanged(presence: Presence): Unit = {
      for {
        listener <- friendListListener
        id <- parseIdFromAddr(presence.getFrom)
        friend <- friends.find(_.id == id)
        previousIsOnline <- isOnlineSnapShot.get(id)
        previousMode <- modeSnapShot.get(id)
        previousStatus <- statusSnapShot.get(id)
      } yield {
        val onChanged = listener.onFriendPresenceChanged(friend)(_) // partial function

        // notify when a friend login/logoff
        if (!previousIsOnline && friend.isOnline) onChanged(Login)
        else if (previousIsOnline && !friend.isOnline) onChanged(Logout)

        // notify when chat mode of a friend change
        else if (previousMode != Chat && friend.chatMode == Chat) onChanged(Available)
        else if (previousMode != Away && friend.chatMode == Away) onChanged(AFK)
        else if (previousMode != Busy && friend.chatMode == Busy) onChanged(Playing)

        // notify when a friend chat status change
        else if (previousStatus != friend.status) onChanged(StatusMsg)

        updateSnapShots(friend)
      }
    }
  })

  /** Initialize information that other summoner on your friend list will see.
    *
    * @param iconId id of the icon
    * @param level summoner level
    * @param wins number of wins
    * @param tier the league tier (I, II, III, etc...)
    * @param division the league division (SILVER, GOLD, CHALLENGER, etc...)
    */
  def initProfileInfo(iconId: Int, level: Int, wins: Int, tier: String, division: String): Unit = {
    _profileIconId = iconId
    _level = level
    _wins = wins
    _tier = tier
    _division = division
    update()
  }

  /** return the summoner's profile icon id */
  def profileIconId: Int = _profileIconId

  /** return the summoner's level */
  def level: Int = _level

  /** return the summoner's wins */
  def wins: Int = _wins

  /** return the summoner's league tier */
  def tier: String = _tier

  /** return the summoner's league division */
  def division: String = _division

  /** return the current status message */
  def statusMsg: String = _statusMsg

  /** Change the summoner status message
    *
    * @param newMsg status message to change to
    */
  def statusMsg_=(newMsg: String) = { _statusMsg = newMsg; update(); }

  /** appear online to all friends (green circle) */
  def appearOnline(): Unit = { presenceType = available; presenceMode = chat; update() }

  /** appear offline to all friends */
  def appearOffline(): Unit = { presenceType = unavailable; update() }

  /** appear away to all friends (red circle) */
  def appearAway(): Unit = { presenceType = available; presenceMode = away; update(); }

  /** Return Id that is mapped to this summoner */
  def summId: String Or NotFound = parseIdFromAddr(conn.getUser).toOr(NotFound("Unable to find ID"))

  /** Send a message to another summoner
    *
    * @param toId summoner ID of the recipient
    * @param text the message
    * @return [[NotConnected]] Error if there is no connection
    */
  def sendMsg(toId: String, text: String): Unit Or NotConnected = {
    val msg = new Message(s"sum$toId@pvp.net", Message.Type.chat)
    msg.setBody(text)
    Try(conn.sendStanza(msg)).toOr.badMap(e => NotConnected(region.url))
  }

  /** Send a friend request to another summoner
    *
    * @param summId summoner ID of the recipient
    * @return [[NotConnected]] Error if there is no connection
    */
  def sendFriendRequest(summId: String): Unit Or NotConnected = {
    val subscribePkt = new Presence(Presence.Type.subscribe)
    subscribePkt.setTo(s"sum$summId@pvp.net")
    Try(conn.sendStanza(subscribePkt)).toOr.badMap(e => NotConnected(region.url))
  }

  /** Remove a friend from your friends list
    *
    * @param summId summoner ID of the recipient
    * @return [[NotConnected]] Error if there is no connection
    */
  def removeFriend(summId: String): Unit Or NotConnected = {
    val unsubscribePkt = new Presence(Presence.Type.unsubscribed)
    unsubscribePkt.setTo(s"sum$summId@pvp.net")
    Try(conn.sendStanza(unsubscribePkt)).toOr.badMap(e => NotConnected(region.url))
  }

  /** Return names of all friend groups */
  def groupNames: Seq[String] = Roster.getInstanceFor(conn).getGroups.map(_.getName).toSeq

  /** Create a friend group with the given name */
  def createFriendGroup(name: String): Unit = Try(Roster.getInstanceFor(conn).createGroup(name))

  /** Move a friend from one group to another.
    * If the group does not exists, it will automatically be created.
    *
    * @note Each friends can be at most in one group.
    * @param friend to be moved to the group
    * @param groupName name of the group to move to
    */
  def moveFriendToGroup(friend: FriendEntity, groupName: String): Unit = Try {
    createFriendGroup(groupName)
    Roster.getInstanceFor(conn).getGroup(groupName).addEntry(friend.entry)
  }

  /** Set a listener to receive friend list events.
    *
    * @note Only one listener can be set and subsequent calls will replace the previous listener.
    * @see [[FriendListListener]]
    * @param listener the FriendListListener to set
    */
  def setFriendListListener(listener: FriendListListener): Unit = friendListListener = Some(listener)

  /** Add a listener to receive dis/reconnection events.
    *
    * @see [[ReconnectionListener]]
    * @param listener the ReconnectionListener to add
    */
  def addReconnectionListener(listener: ReconnectionListener): Unit = conn.addConnectionListener(listener)

  /** Add a listener to receive incoming message events.
    *
    * @see [[ReceiveMsgListener]]
    * @param listener the ReceiveMsgListener to add
    */
  def addReceiveMsgListener(listener: ReceiveMsgListener): Unit =
    ChatManager.getInstanceFor(conn).addChatListener(listener)

  /** return all friends for the user of this session */
  def friends: Set[FriendEntity] = {
    val r = Roster.getInstanceFor(conn)
    r.getEntries.map(entry => new FriendEntity(entry, r.getPresence(entry.getUser)))(collection.breakOut)
  }

  /** Find a friend by their in game name.
    *
    * @note login name and in game name can be different for some user
    * @param inGameName the friend's name to look for
    * @return the found friend
    */
  def findFriendByName(inGameName: String): FriendEntity Or NotFound = {
    val r = Roster.getInstanceFor(conn)
    r.getEntries.find(_.getName == inGameName)
      .map(entry => new FriendEntity(entry, r.getPresence(entry.getUser)))
      .toOr(NotFound(s"Unable to find friend $inGameName"))
  }

  /** Find a friend by their summoner Id.
    *
    * @param id the friend's id to look for
    * @return the found friend
    */
  def findFriendById(id: String): FriendEntity Or NotFound = {
    val r = Roster.getInstanceFor(conn)
    r.getEntries.find(_.getUser == s"sum$id@pvp.net")
      .map(entry => new FriendEntity(entry, r.getPresence(entry.getUser)))
      .toOr(NotFound(s"Unable to find friend id $id"))
  }

  private def update(): Unit = {
    val status = "<body>" +
                 "<profileIcon>" + _profileIconId + "</profileIcon>" +
                 "<level>" + _level + "</level>" +
                 "<wins>" + _wins + "</wins>" +
                 "<tier>" + _tier + "</tier>" +
                 "<rankedLeagueDivision>" + _division + "</rankedLeagueDivision>" +
                 "<rankedLeagueTier>" + _tier + "</rankedLeagueTier>" +
                 "<rankedLeagueQueue>" + "RANKED_SOLO_5x5" + "</rankedLeagueQueue>" +
                 "<rankedWins>" + _wins + "</rankedWins>" +
                 "<statusMsg>" + statusMsg + "</statusMsg>" +
                 "<gameStatus>outOfGame</gameStatus>" +
                 "</body>"

    conn.sendStanza(new Presence(presenceType, status, 1, presenceMode))
  }

  private def updateSnapShots(friend: FriendEntity) {
    isOnlineSnapShot += friend.id -> friend.isOnline
    modeSnapShot += friend.id -> friend.chatMode
    statusSnapShot += friend.id -> friend.status
  }
}
