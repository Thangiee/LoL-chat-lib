package lolchat.model

sealed trait ChatMode
case object Chat extends ChatMode // green circle
case object AFK extends ChatMode // red circle
case object Busy extends ChatMode // yellow circle
