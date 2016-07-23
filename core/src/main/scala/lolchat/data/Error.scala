package lolchat.data

import scala.util.Try

case class Error(code: Int, msg: String, detail: Option[String])

object Error {
  def apply(code: Int, msg: String): Error = Error(code, msg, None)
  def apply(code: Int, msg: String, throwable: Throwable): Error = Error(code, msg, Try(throwable.getMessage).toOption)
}