package lolchat.data

case class Region(url: String, abbr: String)

object Region {
  val NA = Region("chat.na2.lol.riotgames.com", "na")
  val BR = Region("chat.br.lol.riotgames.com", "br")
  val EUNE = Region("chat.eun1.lol.riotgames.com", "eune")
  val EUW = Region("chat.euw1.lol.riotgames.com", "euw")
  val KR = Region("chat.kr.lol.riotgames.com", "kr")
  val LAN = Region("chat.la1.lol.riotgames.com", "lan")
  val LAS = Region("chat.la2.lol.riotgames.com", "las")
  val OCE = Region("chat.oc1.lol.riotgames.com", "oce")
  val RU = Region("chat.ru.lol.riotgames.com", "ru")
  val TR = Region("chat.tr.lol.riotgames.com", "tr")
}
