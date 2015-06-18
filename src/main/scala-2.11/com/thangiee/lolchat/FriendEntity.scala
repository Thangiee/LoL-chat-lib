package com.thangiee.lolchat

import com.thangiee.lolchat.chatMode._
import com.thangiee.lolchat.error.NotFound
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.RosterEntry
import org.scalactic.Or
import org.scalactic.OptionSugar._

import scala.collection.JavaConversions._
import scala.util.Try

/** This class represent a friend in the user's friend list.
  * This class is '''NOT''' meant to be created directly, instead, use methods
  * from an instance of [[Session]].
  */
class FriendEntity(private[lolchat] val entry: RosterEntry, private[lolchat] val presence: Presence) {
  val name = entry.getName
  val id   = parseIdFromAddr(entry.getUser).getOrElse("-1")

  /** return the friend's chat mode */
  def chatMode: ChatMode = presence.getMode match {
    case Presence.Mode.chat => Chat
    case Presence.Mode.away => Away
    case _                  => Busy
  }

  /** return true if this friend is online */
  def isOnline: Boolean = presence.getType match {
    case Presence.Type.available => true
    case _                       => false
  }

  /** return this friend's status (encode in XML)
    * @note Status of offline friends will be an empty string */
  def status: String = if (isOnline) Try(presence.getStatus.replace("&apos;", "")).getOrElse("") else ""

  /** return all group names this friend belong to
    * @note all friends belong **Default group if they have not been added to another group*/
  def groupNames: Seq[String] = entry.getGroups.map(_.getName)

  /** return the name of the champion this friend is currently using
    * @note only available when this friend is in a match */
  def selectedChamp: String Or NotFound = parseStatus("skinname").toOr(NotFound())

  /** return this friend's game status */
  def gameStatus: String Or NotFound = parseStatus("gameStatus").toOr(NotFound())

  /** return this friend's summoner level */
  def level: Int = parseStatus("level").map(_.toInt).getOrElse(0)

  /** return this friend's number of wins */
  def wins: Int = parseStatus("wins").map(_.toInt).getOrElse(0)

  /** return this friend's status message */
  def statusMsg: String = parseStatus("statusMsg").getOrElse("")

  /** return this friend's ranked league tier
    * i.e. Bronze, Silver, Gold, etc... */
  def rankedTier: String Or NotFound = parseStatus("rankedLeagueTier").toOr(NotFound())

  /** return this friend's ranked division tier
    * i.e. I, II, III, IV, V */
  def rankedDivision: String Or NotFound = parseStatus("rankedLeagueDivision").toOr(NotFound())

  /** return this friend's league name */
  def leagueName: String Or NotFound = parseStatus("rankedLeagueName").toOr(NotFound())

  /** return the game start time in millisecond if this friend is in a game */
  def gameStartTime: Long Or NotFound = parseStatus("timeStamp").map(_.toLong).toOr(NotFound())

  /** Parse information from friend status.
    * Some values are only available use certain circumstances.
    * For example, skinname is only available when the friend is in a game.
    *
    * Possible value: profileIcon, level, wins, leaves, odinWins, odinLeaves,
    * rankedLosses, rankedRating, tier, statusMsg, gameQueueType, timeStamp,
    * gameStatus, rankedLeagueName, skinname(selected Champion),
    * rankedLeagueDivision(I, II, III, IV, V), rankedLeagueTier(Bronze, Silver, Gold, etc...)
    * rankedLeagueQueue, rankedWins
    */
  private def parseStatus(value: String): Option[String] = {
    val pattern = s"(?<=$value>).*?(?=</$value)"
    val result = pattern.r.findFirstIn(status).getOrElse("")
    if (!result.isEmpty) Some(result) else None
  }
}