package com.thangiee.lolchat

import javax.net.ssl.SSLSocketFactory

import com.thangiee.lolchat.error._
import com.thangiee.lolchat.region.Region
import org.jivesoftware.smack.SmackException.{NoResponseException, ConnectionException}
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.sasl.SASLErrorException
import org.jivesoftware.smack.tcp.{XMPPTCPConnectionConfiguration, XMPPTCPConnection}
import org.scalactic.OptionSugar._
import org.scalactic.{Bad, Good, Or}

import scala.util.Try

/** Use to create and manage sessions connected to Riot pvp chat server. */
object LoLChat {
  type Username = String
  private var _sessions = Map[Username, Session]()

  /** return all logged in sessions */
  def sessions: Map[Username, Session] = _sessions

  /** Search for a logged in session with the given username
    *
    * @param user the username used to login
    * @return a Session or an error message if not found
    */
  def findSession(user: String): Session Or NoSession = _sessions.get(user).toOr(NoSession(s"No session under username $user"))

  /** Login a user to the chat server.
    *
    * This will create and return a Session for the logging in user or
    * return a LoginError if the attempt to login fails. The LoginError can be
    * one of the follow types:
    *   - FailAuthentication when fail to login due to invalid username or password
    *   - NotConnected when there is no connection
    *   - UnexpectedError
    *
    * @param user the username
    * @param pass the password
    * @param region the region to login to
    * @param offlineLogin appear offline after logging in
    * @return a Session if login successfully or a LoginError if something went wrong
    */
  def login(user: String, pass: String, region: Region, offlineLogin: Boolean = false): Session Or LoginError = {
    val config = XMPPTCPConnectionConfiguration.builder()
      .setHost(region.url)
      .setPort(5223)
      .setServiceName("pvp.net")
      .setSocketFactory(SSLSocketFactory.getDefault)
      .setUsernameAndPassword(user, "AIR_" + pass)
      .setConnectTimeout(7000) // timeout in sec
      .build()

    val conn = new XMPPTCPConnection(config)
    Try {conn.connect(); conn.login()} recover {
      case e: SASLErrorException  => return Bad(FailAuthentication(user, pass))
      case e: ConnectionException => return Bad(NotConnected(region.url))
      case e: NoResponseException => return Bad(NotConnected(region.url))
      case t: Throwable           => return Bad(UnexpectedError(t))
    }

    // make sure roster is loaded after logging in
    Try(Roster.getInstanceFor(conn).reloadAndWait()).recover { case t: Throwable => return Bad(UnexpectedError(t)) }

    val session = new Session(user, pass, region, conn)
    if (offlineLogin) session.appearOffline() else session.appearOnline()

    _sessions += user -> session // keep track of the session
    Good(session)
  }

  /** Logout a given session
    * @param session the session to logout */
  def logout(session: Session): Unit = {
    session.disableReconnection()
    session.conn.disconnect()
    _sessions -= session.user
  }

  /** Logout a session belonging to the given user
    * @param user the username for the session to logout */
  def logout(user: String): Unit = _sessions.get(user).foreach(logout)

  /** Logout all sessions */
  def endAllSessions(): Unit = {
    _sessions.foreach { case (_, session) => logout(session) }
    _sessions = _sessions.empty
  }
}