package lolchat.model

// todo: error code?
case class ChatError(msg: String, throwable: Option[Throwable])

object ChatError {
  def apply(msg: String): ChatError = ChatError(msg, None)
  def apply(msg: String, throwable: Throwable): ChatError = ChatError(msg, Some(throwable))
}