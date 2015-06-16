package com.thangiee.lolchat

object error {
  trait Error

  sealed trait LoginError
  case class FailAuthentication(user: String, pass: String) extends Error with LoginError
  case class NotConnected(hostURL: String) extends Error with LoginError
  case class UnexpectedError(throwable: Throwable) extends Error with LoginError
}
