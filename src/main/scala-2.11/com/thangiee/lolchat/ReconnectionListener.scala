package com.thangiee.lolchat

import org.jivesoftware.smack.{XMPPConnection, ConnectionListener}

/** Provide callback methods for [[Session#addReconnectionListener]] */
trait ReconnectionListener extends ConnectionListener {
  private var attempt = 0

  /** Called after losing connection */
  def onLostConnection(): Unit

  /** Count down to a reconnection attempt
   * @param sec seconds til reconnection attempt */
  def onReconnectingIn(sec: Int): Unit

  /** Called after a reconnection attempt failed
   * @param attempt number of consecutive failed reconnection attempts */
  def onReconnectionFailed(attempt: Int): Unit

  /** Called after successfully reconnecting*/
  def onReconnected(): Unit

  override def reconnectionFailed(e: Exception): Unit = {
    attempt += 1
    onReconnectionFailed(attempt)
  }

  override def reconnectionSuccessful(): Unit = {
    attempt = 0
    onReconnected()
  }

  override def connectionClosedOnError(e: Exception): Unit = onLostConnection()

  override def reconnectingIn(i: Int): Unit = onReconnectingIn(i)

  override def connected(xmppConnection: XMPPConnection): Unit = {}

  override def connectionClosed(): Unit = {}

  override def authenticated(xmppConnection: XMPPConnection, b: Boolean): Unit = {}
}
