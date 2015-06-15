package com.thangiee.lolchat

import org.jivesoftware.smack.chat.{ChatManagerListener, ChatMessageListener}
import org.jivesoftware.smack.packet.Message

/** Provide callback methods for [[Session#addReceiveMsgListener]] */
trait ReceiveMsgListener extends ChatManagerListener {
  def chatCreated(chat: org.jivesoftware.smack.chat.Chat, createdLocally: Boolean): Unit =
    chat.addMessageListener(new ChatMessageListener {
      def processMessage(chat: org.jivesoftware.smack.chat.Chat, msg: Message): Unit =
        parseIdFromAddr(chat.getParticipant).foreach { id =>
          onReceivedMessage(id, msg.getBody)
        }
    })

  /** Called after a message is received.
    *
    * @param fromId summoner id of the sender
    * @param textMsg the message
    */
  def onReceivedMessage(fromId: String, textMsg: String)
}

