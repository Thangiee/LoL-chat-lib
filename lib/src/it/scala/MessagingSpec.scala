import cats.data.Xor
import lolchat._
import lolchat.data._
import org.scalatest.concurrent.AsyncAssertions.Waiter
import org.scalatest.concurrent.PatienceConfiguration.Timeout

import scala.concurrent.duration._

class MessagingSpec extends BaseSpec {

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

  "LoLChat" should "be able to send and receive message" in {
    val waiter = new Waiter

    bobSess.msgStream.foreach(msg => {
      waiter(msg.fromId should be(alice.summId))
      waiter(msg.txt should be("hello world"))
      waiter.dismiss()
    })

    LoLChat.run(sendMsg(bob.summId, "hello world")(aliceSess))
    waiter.await(Timeout(10.seconds))
  }

  it should "be able to send a message to a friend using their in game name" in {
    whenReady(LoLChat.run(sendMsgToFriend(alice.inGameName, "testing")(bobSess))) { res =>
      res should be(Xor.Right(None)) // i.e. no error
    }
  }

  "sendMsgToFriend that not on user's friend list" should "yield an err message" in {
    whenReady(LoLChat.run(sendMsgToFriend("fakeFriend", "testing")(bobSess))) { res =>
      res should be(Xor.Right(Some("fakeFriend not in your friends list."))) // i.e. no error
    }
  }

}
