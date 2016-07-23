package lolchat.data

import scala.util.Try

// todo: error code?
case class Error(msg: String, detail: Option[String])

object Error {
  def apply(msg: String): Error = Error(msg, None)
  def apply(msg: String, throwable: Throwable): Error = Error(msg, Try(throwable.getMessage).toOption)
}