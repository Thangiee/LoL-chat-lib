package com.thangiee.lolchat

import org.scalactic.{Bad, Good, Or}

object region {

  trait Region {
    /** this region full name */
    def name: String
    /** the url use to connect to this region */
    def url: String
    /** abbreviation of this region's name */
    def id: String
  }

  case object BR extends Region {
    override val url : String = "chat.br.lol.riotgames.com"
    override val name: String = "Brazil"
    override val id  : String = "br"
  }

  case object EUNE extends Region {
    override val url : String = "chat.eun1.lol.riotgames.com"
    override val name: String = "Europe Nordic and East"
    override val id  : String = "eune"
  }

  case object EUW extends Region {
    override val url : String = "chat.euw1.lol.riotgames.com"
    override val name: String = "Europe West"
    override val id  : String = "euw"
  }

  case object KR extends Region {
    override val url : String = "chat.kr.lol.riotgames.com"
    override val name: String = "Korea"
    override val id  : String = "kr"
  }

  case object LAN extends Region {
    override val url : String = "chat.la1.lol.riotgames.com"
    override val name: String = "Latin America North"
    override val id  : String = "lan"
  }

  case object LAS extends Region {
    override val url : String = "chat.la2.lol.riotgames.com"
    override val name: String = "Latin America South"
    override val id  : String = "las"
  }

  case object NA extends Region {
    override val url : String = "chat.na2.lol.riotgames.com"
    override val name: String = "North America"
    override val id  : String = "na"
  }

  case object OCE extends Region {
    override val url : String = "chat.oc1.lol.riotgames.com"
    override val name: String = "Oceania"
    override val id  : String = "oce"
  }

  case object RU extends Region {
    override val url : String = "chat.ru.lol.riotgames.com"
    override val name: String = "Russia"
    override val id  : String = "ru"
  }

  case object TR extends Region {
    override val url : String = "chat.tr.lol.riotgames.com"
    override val name: String = "Turkey"
    override val id  : String = "tr"
  }

  def getFromId(regionId: String): Region Or ErrMsg = regionId.toLowerCase match {
    case BR.id   => Good(BR)
    case EUNE.id => Good(EUNE)
    case EUW.id  => Good(EUW)
    case KR.id   => Good(KR)
    case LAN.id  => Good(LAN)
    case LAS.id  => Good(LAS)
    case NA.id   => Good(NA)
    case OCE.id  => Good(OCE)
    case RU.id   => Good(RU)
    case TR.id   => Good(TR)
    case _       => Bad(s"regionId: $regionId does not match any available region.")
  }
}



