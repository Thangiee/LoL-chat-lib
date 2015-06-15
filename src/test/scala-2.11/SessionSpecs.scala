import com.thangiee.lolchat.changedPresence.ChangedPresence
import com.thangiee.lolchat.region.NA
import com.thangiee.lolchat.{FriendEntity, FriendListListener, LoLChat, ReceiveMsgListener}
import org.scalatest.concurrent.AsyncAssertions.{Dismissals, Waiter}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.SpanSugar._

class SessionSpecs extends BaseSpec {

  // You should NOT use get to unwrap the Session in your code.
  // This test assume that logging in will always be successful.
  val bobSession   = LoLChat.login(bob.user, bob.pass, NA).get
  val aliceSession = LoLChat.login(alice.user, alice.pass, NA).get

  // This test assume alice and bob are already friends
  override def beforeEach(): Unit = {
    bobSession.friends.map(_.id).contains(alice.summId) shouldBe true
    aliceSession.friends.map(_.name).contains(bob.inGameName) shouldBe true
    super.beforeEach()
  }

  "A session" should "be able to send and receive messages" in {
    val w = new Waiter
    val message = "hello world"

    bobSession.addReceiveMsgListener(new ReceiveMsgListener {
      def onReceivedMessage(fromId: String, textMsg: String): Unit = {
        w {fromId shouldEqual alice.summId}
        w {textMsg shouldEqual message}
        w.dismiss()
      }
    })

    aliceSession.sendMsg(bob.summId, message)
    w.await(Timeout(7.seconds))
  }

  it should "be able to add and remove friends" in {
    val addWaiter = new Waiter()
    val removeWaiter = new Waiter()
    var shouldAccept = false

    bobSession.setFriendListListener(new FriendListListener {
      def onFriendPresenceChanged(friend: FriendEntity)(changedPresence: ChangedPresence): Unit = {}

      def onReceivedFriendRequest(fromId: String): Boolean = shouldAccept

      def onFriendRemoved(id: String): Unit = {
        removeWaiter {bobSession.friends.size shouldEqual 0}
        removeWaiter {aliceSession.friends.size shouldEqual 0}
        removeWaiter.dismiss()
      }

      def onFriendAdded(id: String): Unit = {
        addWaiter {bobSession.friends.size shouldEqual 1}
        addWaiter {aliceSession.friends.size shouldEqual 1}
        addWaiter.dismiss()
      }
    })

    // test remove friend
    aliceSession.removeFriend(bob.summId)
    removeWaiter.await(Timeout(10.seconds))

    // test reject friend request
    aliceSession.sendFriendRequest(bob.summId)
    bobSession.friends.size shouldEqual 0
    aliceSession.friends.size shouldEqual 0

    // test accept friend request
    shouldAccept = true
    aliceSession.sendFriendRequest(bob.summId)
    addWaiter.await(Timeout(10.seconds))
  }

  it should "be able to create a friend groups and move friends to/from with other groups" in {
    bobSession.createFriendGroup("testGroup")
    bobSession.groupNames should contain("testGroup")

    bobSession.findFriendByName(alice.inGameName) match {
      case Some(f) =>
        bobSession.moveFriendToGroup(f, "testGroup")
        f.groupNames should contain("testGroup")
        bobSession.moveFriendToGroup(f, "General")
        f.groupNames shouldNot contain("testGroup")
        f.groupNames should contain("General")
        bobSession.groupNames shouldNot contain("testGroup")
        bobSession.groupNames should contain("General")
      case None =>
        fail("fail to find alice")
    }
  }

}
