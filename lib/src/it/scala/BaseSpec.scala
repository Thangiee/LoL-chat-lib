import lolchat.data._
import lolchat.data.Error
import lolchat.model.Session
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import rx.Ctx

trait BaseSpec extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterEach with BeforeAndAfterAll {
    implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val bob   = TestAcc("TestAccountBob", "testtest123", "68659985", "B0B123")
  val alice = TestAcc("TestAccountAlice", "testtest123", "68510001", "alice817")

  val bobSess   = Session(bob.user, bob.pass, Region.NA, acceptFriendRequest = true)
  val aliceSess = Session(alice.user, alice.pass, Region.NA, acceptFriendRequest = true)

  def whenReady[A, B](res: AsyncResult[A])(f: Either[Error, A] => B): B = whenReady(res.value)(f)

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  implicit val exeCtx = scala.concurrent.ExecutionContext.Implicits.global
}

case class TestAcc(user: String, pass: String, summId: String, inGameName: String)