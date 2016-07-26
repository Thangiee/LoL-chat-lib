package lolchat.model

case class Friend(
  name: String,
  id: String,
  chatMode: ChatMode,
  isOnline: Boolean,
  groupName: Vector[String],
  selectedChamp: Option[String],
  gameStatus: Option[String],
  level: Int,
  wins: Int,
  statusMsg: String,
  rankedTier: Option[String],
  rankedDivision: Option[String],
  leagueName: Option[String],
  gameStartTime: Option[Long],
  profileIconId: Option[Int]
)

