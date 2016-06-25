package lolchat.model

sealed trait Appearance
case object Online extends Appearance
case object Offline extends Appearance
case object Away extends Appearance
