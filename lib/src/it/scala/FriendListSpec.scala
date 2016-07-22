import lolchat._
import lolchat.model._
import org.scalatest.concurrent.AsyncAssertions.Waiter
import org.scalatest.concurrent.PatienceConfiguration.Timeout

import scala.concurrent.duration._

class FriendListSpec extends BaseSpec {

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

  "FriendUpdated event" should "be trigger by a friend appearing away" in {
    val waiter = new Waiter
    bobSess.friendListStream.foreach {
      case FriendUpdated(_) => waiter.dismiss()
      case event => fail(s"unexpected event $event")
    }

    LoLChat.run(appearAway(aliceSess))
    waiter.await(Timeout(10.seconds))
  }

  it should "be trigger by a friend appearing offline" in {
    val waiter = new Waiter
    bobSess.friendListStream.foreach {
      case FriendUpdated(_) => waiter.dismiss()
      case event => fail(s"unexpected event $event")
    }

    LoLChat.run(appearOffline(aliceSess))
    waiter.await(Timeout(10.seconds))
  }

  it should "be trigger by a friend appearing online" in {
    val waiter = new Waiter
    bobSess.friendListStream.foreach {
      case FriendUpdated(_) => waiter.dismiss()
      case event => fail(s"unexpected event $event")
    }

    LoLChat.run(appearOnline(aliceSess))
    waiter.await(Timeout(10.seconds))
  }

  it should "be trigger by a friend updating their status" in {
    val waiter = new Waiter
    bobSess.friendListStream.foreach {
      case FriendUpdated(f) => waiter.dismiss()
      case event => fail(s"unexpected event $event")
    }

    LoLChat.run(modifyProfile(p => p.copy(statusMsg = "testing"))(aliceSess))
    waiter.await(Timeout(10.seconds))
  }
}
