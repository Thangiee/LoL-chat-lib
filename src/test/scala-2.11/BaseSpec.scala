import org.scalamock.scalatest.MockFactory
import org.scalatest._

trait BaseSpec extends FlatSpec with Matchers with MockFactory with BeforeAndAfterEach with BeforeAndAfterAll {
  val bob   = TestAcc("TestAccountBob", "testtest123", "68659985", "B0B123")
  val alice = TestAcc("TestAccountAlice", "testtest123", "68510001", "alice817")
}

case class TestAcc(user: String, pass: String, summId: String, inGameName: String)