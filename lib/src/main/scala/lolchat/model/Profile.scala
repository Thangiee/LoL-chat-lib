package lolchat.model

case class Profile(
  iconId: Int = 1,
  level: Int = 1,
  wins: Int = 0,
  masteryScore: Int = 0,
  tier: String = "",
  division: String = "",
  statusMsg: String = ""
)

object Profile {
  def parseXML(xml: String): Profile = {
    import lolchat.util.parsing._
    def parse[A] = parseXml[A](xml) _

    Profile(
      parse("profileIcon")(_.toInt).getOrElse(1),
      parse("level")(_.toInt).getOrElse(0),
      parse("rankedWins")(_.toInt).getOrElse(0),
      parse("championMasteryScore")(_.toInt).getOrElse(0),
      parse("tier")(identity).getOrElse(""),
      parse("rankedLeagueDivision")(identity).getOrElse(""),
      parse("statusMsg")(identity).getOrElse("")
    )
  }
}