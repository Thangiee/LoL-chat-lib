package com.thangiee.lolchat

object region {

  trait Region {
    /** this region full name */
    def name: String
    /** the url use to connect to this region */
    def url: String
    /** abbreviation of this region's name */
    def id: String
  }

  object BR extends Region {
    override val url : String = "chat.br.lol.riotgames.com"
    override val name: String = "Brazil"
    override val id  : String = "br"
  }

  object EUNE extends Region {
    override val url : String = "chat.eun1.lol.riotgames.com"
    override val name: String = "Europe Nordic and East"
    override val id  : String = "eune"
  }

  object EUW extends Region {
    override val url : String = "chat.euw1.lol.riotgames.com"
    override val name: String = "Europe West"
    override val id  : String = "euw"
  }

  object KR extends Region {
    override val url : String = "chat.kr.lol.riotgames.com"
    override val name: String = "Korea"
    override val id  : String = "kr"
  }

  object LAN extends Region {
    override val url : String = "chat.la1.lol.riotgames.com"
    override val name: String = "Latin America North"
    override val id  : String = "lan"
  }

  object LAS extends Region {
    override val url : String = "chat.la2.lol.riotgames.com"
    override val name: String = "Latin America South"
    override val id  : String = "las"
  }

  object NA extends Region {
    override val url : String = "chat.na2.lol.riotgames.com"
    override val name: String = "North America"
    override val id  : String = "na"
  }

  object OCE extends Region {
    override val url : String = "chat.oc1.lol.riotgames.com"
    override val name: String = "Oceania"
    override val id  : String = "oce"
  }

  object RU extends Region {
    override val url : String = "chat.ru.lol.riotgames.com"
    override val name: String = "Russia"
    override val id  : String = "ru"
  }

  object TR extends Region {
    override val url : String = "chat.tr.lol.riotgames.com"
    override val name: String = "Turkey"
    override val id  : String = "tr"
  }

}



