package com.thangiee.lolchat

object error {
  trait Error
  case class NotFound(msg: String = "") extends Error

  sealed trait LoginError extends Error
  case class FailAuthentication(user: String, pass: String) extends LoginError
  case class NotConnected(hostURL: String) extends LoginError
  case class UnexpectedError(throwable: Throwable) extends LoginError
}
