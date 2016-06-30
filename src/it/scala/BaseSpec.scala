import cats.data.Xor
import lolchat._
import lolchat.model.{ChatError, Region, Session}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

trait BaseSpec extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterEach with BeforeAndAfterAll {
  val bob   = TestAcc("TestAccountBob", "testtest123", "68659985", "B0B123")
  val alice = TestAcc("TestAccountAlice", "testtest123", "68510001", "alice817")

  val bobSess   = Session(bob.user, bob.pass, Region.NA, acceptFriendRequest = true)
  val aliceSess = Session(alice.user, alice.pass, Region.NA, acceptFriendRequest = true)

  def whenReady[A, B](res: ChatResult[A])(f: Xor[ChatError, A] => B): B = whenReady(res.value)(f)
}

case class TestAcc(user: String, pass: String, summId: String, inGameName: String)