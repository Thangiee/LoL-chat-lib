package lolchat.data

case class Region(url: String) extends AnyVal

object Region {
  val NA = Region("chat.na2.lol.riotgames.com")
  val BR = Region("chat.br.lol.riotgames.com")
  val EUNE = Region("chat.eun1.lol.riotgames.com")
  val EUW = Region("chat.euw1.lol.riotgames.com")
  val KR = Region("chat.kr.lol.riotgames.com")
  val LAN = Region("chat.la1.lol.riotgames.com")
  val LAS = Region("chat.la2.lol.riotgames.com")
  val OCE = Region("chat.oc1.lol.riotgames.com")
  val RU = Region("chat.ru.lol.riotgames.com")
  val TR = Region("chat.tr.lol.riotgames.com")
}
