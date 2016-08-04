import cats.data.Xor
import lolchat._
import lolchat.data._
import lolchat.model._
import org.scalatest.concurrent.AsyncAssertions.{Dismissals, Waiter}
import org.scalatest.concurrent.PatienceConfiguration.Timeout

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.Random

class FriendMgmtSpec extends BaseSpec {

  override def beforeAll(): Unit = {
    val prg = for {
      _ <- login
      f <- friends
    } yield f

    // tests assume alice and bob are already friends

    whenReady(LoLChat.run(prg(bobSess))) { res =>
      res.fold(err => false, friends => friends.exists(_.id == alice.summId)) should be(true)
    }

    whenReady(LoLChat.run(prg(aliceSess))) { res =>
      res.fold(err => false, friends => friends.exists(_.id == bob.summId)) should be(true)
    }
  }

  it should "be able to create a friend group" in {
    val testGroup = s"testGroup${Random.nextInt(1000)}"
    val prg = for {
      _     <- createGroup(testGroup)
      names <- groupNames
    } yield names

    whenReady(LoLChat.run(prg(bobSess))) {
      case Xor.Right(groupNames) => groupNames should contain(testGroup)
      case Xor.Left(error) => fail(error.msg)
    }
  }

  it should "be able to move a friend between groups" in {
    val testGroup = s"testGroup${Random.nextInt(1000)}"
    val prg = for {
      _ <- moveFriendToGroup(alice.inGameName, testGroup)
      f <- friendById(alice.summId)
    } yield f

    whenReady(LoLChat.run(prg(bobSess))) {
      case Xor.Right(Some(f)) => f.groupName should contain(testGroup)
      case Xor.Right(None)    => fail("fail to find alice in friend list")
      case Xor.Left(err)      => fail(err.msg)
    }
  }

  "LoLChat" should "be able to add and remove friends" in {
    val waiter = new Waiter
    val events = mutable.Queue[FriendListEvent]()

    bobSess.friendListStream.map {
      case e: FriendAdded   => events.enqueue(e); waiter.dismiss()
      case e: FriendRemoved => events.enqueue(e); waiter.dismiss()
      case _ =>
    }

    val id = alice.summId
    whenReady(LoLChat.run(removeFriend(id)(bobSess)))(identity)
    whenReady(LoLChat.run(sendFriendRequest(id)(bobSess)))(identity)

    waiter.await(Timeout(10.seconds), Dismissals(2))
    events should be(mutable.Queue[FriendListEvent](FriendRemoved(id), FriendAdded(id)))
  }

  it should "be able to get online friends" in {
    whenReady(LoLChat.run(onlineFriends(aliceSess))) { res =>
      val onFriends: Vector[Friend] = res.getOrElse(fail("Fail to get online friends"))
      onFriends.find(_.id == bob.summId) shouldBe defined
    }
  }
}
