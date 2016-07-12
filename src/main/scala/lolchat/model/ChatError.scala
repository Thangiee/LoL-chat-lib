package lolchat.model

import scala.util.Try

// todo: error code?
case class ChatError(msg: String, detail: Option[String])

object ChatError {
  def apply(msg: String): ChatError = ChatError(msg, None)
  def apply(msg: String, throwable: Throwable): ChatError = ChatError(msg, Try(throwable.getMessage).toOption)
}