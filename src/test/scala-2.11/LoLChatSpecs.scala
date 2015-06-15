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
    LoLChat.login("TestAccountBob", "testtest123", NA) shouldBe a[Good[Session, _]]
  }

  it should "return an FailAuthentication error on a fail login attempt due to invalid username/password" in {
    LoLChat.login("TestAccountBob", "wrongpass", NA) shouldBe a[Bad[_, FailAuthentication]]
  }

  it should "return an NotConnected error when failing to reach the host" in {
    val regionMock = mock[Region]
    (regionMock.url _).expects().returning("fake.url")

    LoLChat.login("TestAccountBob", "testtest123", regionMock) shouldBe a[Bad[_, NotConnected]]
  }

  "LoLChat" should "keep track of logged in sessions" in {
    LoLChat.sessions.size shouldEqual 0

    val bobSession = LoLChat.login("TestAccountBob", "testtest123", NA)
    val aliceSession = LoLChat.login("TestAccountAlice", "testtest123", NA)
    LoLChat.sessions.size shouldEqual 2

    LoLChat.login("fakeUser", "fakePass", NA)
    LoLChat.sessions.size shouldEqual 2

    for {
      bobSessFound <- LoLChat.findSession("TestAccountBob")
      bobSessLogin <- bobSession
    } yield {
      bobSessFound shouldEqual bobSessLogin
    }

    LoLChat.logout("TestAccountBob")
    LoLChat.sessions.size shouldEqual 1
    LoLChat.logout(aliceSession.get)
    LoLChat.sessions.size shouldEqual 0
  }

}
