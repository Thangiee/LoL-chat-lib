import com.thangiee.lolchat.error.{FailAuthentication, NotConnected}
import com.thangiee.lolchat.region.{NA, Region}
import com.thangiee.lolchat.{LoLChat, Session}
import org.scalactic.{Bad, Good}

class LoLChatSpecs extends BaseSpec {

  override def afterEach(): Unit = {
    super.afterEach()
    LoLChat.endAllSessions()
  }

  "LoLChat login" should "return a Session on a successful login" in {
    LoLChat.login(bob.user, bob.pass, NA) shouldBe a[Good[Session, _]]
  }

  it should "return an FailAuthentication error on a fail login attempt due to invalid username/password" in {
    LoLChat.login(bob.user, "wrongpass", NA) shouldBe a[Bad[_, FailAuthentication]]
  }

  it should "return an NotConnected error when failing to reach the host" in {
    val regionMock = mock[Region]
    (regionMock.url _).expects().returning("fake.url")

    LoLChat.login(bob.user, bob.pass, regionMock) shouldBe a[Bad[_, NotConnected]]
  }

  "LoLChat" should "keep track of logged in sessions" in {
    LoLChat.sessions.size shouldEqual 0

    val bobSession = LoLChat.login(bob.user, bob.pass, NA)
    val aliceSession = LoLChat.login(alice.user, alice.pass, NA)
    LoLChat.sessions.size shouldEqual 2

    LoLChat.login("fakeUser", "fakePass", NA)
    LoLChat.sessions.size shouldEqual 2

    for {
      bobSessFound <- LoLChat.findSession(bob.user)
      bobSessLogin <- bobSession
    } yield {
      bobSessFound shouldEqual bobSessLogin
    }

    LoLChat.logout(bob.user)
    LoLChat.sessions.size shouldEqual 1
    LoLChat.logout(aliceSession.get)
    LoLChat.sessions.size shouldEqual 0
  }

}
