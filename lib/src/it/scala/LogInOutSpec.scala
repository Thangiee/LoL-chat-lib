import cats.data.Xor
import lolchat._
import lolchat.data._
import lolchat.model._

class LogInOutSpec extends BaseSpec {

  override def afterEach(): Unit = {
    whenReady(LoLChat.run(logout(bobSess)))(identity)
    whenReady(LoLChat.run(logout(aliceSess)))(identity)
  }

  "Login" should "succeed given a valid LoL account" in {
    whenReady(LoLChat.run(login(bobSess))) { res =>
      res.isRight should be(true)
    }
  }

  it should "result in a ChatError given invalid credentials" in {
    val session = Session(bob.user, "badpass", Region.NA)
    whenReady(LoLChat.run(login(session))) { res =>
      res should matchPattern { case Xor.Left(Error(401, _, _)) => }
    }
  }

  "Multiple login ops" should "not result in a ChatError" in {
    val prg = for { _ <- login; _ <- login } yield ()

    whenReady(LoLChat.run(prg(bobSess))) { res =>
      res.fold(chatErr => false, succ => true) should be(true)
    }
  }

  "Logout" should "succeed given a logged in session" in {
    val prg = for { _ <- login; _ <- logout } yield ()
    whenReady(LoLChat.run(prg(bobSess))) { res =>
      res.isRight should be(true)
    }
  }

  "LoLChat" should "keep track of sessions" in {
    whenReady(LoLChat.run(login(bobSess)))(_ => LoLChat.sessionCount should be(1))
    whenReady(LoLChat.run(logout(bobSess)))(_ => LoLChat.sessionCount should be(0))
  }

  it should "be able to login offline" in {
    whenReady(LoLChat.run(offlineLogin(bobSess)))(identity)

    val prg = for { _ <- login; f <- friendByName(bob.inGameName) } yield f
    whenReady(LoLChat.run(prg(aliceSess))) { res =>
      val f: Friend = res.fold(err => fail(err.msg), _.getOrElse(fail(s"${bob.inGameName} not on FL")))
      f.isOnline should be(false)
    }
  }

  "Running an operation before login" should "result in a ChatError" in {
    whenReady(LoLChat.run(getProfile(bobSess))) { res =>
      res should matchPattern { case Xor.Left(Error(401, _, _)) => }
    }
  }
}
