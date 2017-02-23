import lolchat._
import lolchat.data._
import lolchat.model.Profile

class ProfileSpec extends BaseSpec {

  val profile = Profile(
    iconId = 10,
    level = 25,
    wins = 999,
    masteryScore = 30,
    tier = "GOLD",
    division = "III"
  )

  override def beforeAll(): Unit = {
    whenReady(LoLChat.run(login(bobSess)))(identity)
  }

  "LoLChat" should "be able to get & set user profile info" in {
    val prg = for {
      _ <- setProfile(profile)
      p <- getProfile
    } yield p

    whenReady(LoLChat.run(prg(bobSess)))(res => res should be(Right(profile)))
  }

  it should "be able to modify user profile info" in {
    val prg = for {
      _ <- setProfile(profile)
      _ <- modifyProfile(p => p.copy(level = 30))
      p <- getProfile
    } yield p

    whenReady(LoLChat.run(prg(bobSess)))(res => res should be(Right(profile.copy(level = 30))))
  }
}
