import com.thangiee.lolchat.changedPresence._
import com.thangiee.lolchat.region.NA
import com.thangiee.lolchat.{FriendEntity, FriendListListener, LoLChat, ReceiveMsgListener}
import org.scalactic.{Bad, Good}
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
      case Good(f) =>
        bobSession.moveFriendToGroup(f, "testGroup")
        f.groupNames should contain("testGroup")
        bobSession.moveFriendToGroup(f, "General")
        f.groupNames shouldNot contain("testGroup")
        f.groupNames should contain("General")
        bobSession.groupNames shouldNot contain("testGroup")
        bobSession.groupNames should contain("General")
      case Bad(notFound)    =>
        fail("fail to find alice")
    }
  }

  it should "be able to listen for various friend activities" in {
    val w = new Waiter

    bobSession.setFriendListListener(new FriendListListener {
      def onFriendPresenceChanged(friend: FriendEntity)(changedPresence: ChangedPresence): Unit = {
        changedPresence match {
          case Available => w.dismiss()
          case AFK       => w.dismiss()
          case Login     => w.dismiss()
          case Logout    => w.dismiss()
          case StatusMsg => w { friend.statusMsg shouldEqual "testtest" }; w.dismiss()
          case Playing   => // unable to test this
        }
      }
      def onReceivedFriendRequest(fromId: String): Boolean = ???
      def onFriendRemoved(id: String): Unit = ???
      def onFriendAdded(id: String): Unit = ???
    })

    aliceSession.appearAway()    // afk
    aliceSession.appearOnline()  // available
    aliceSession.appearOffline() // logout
    aliceSession.appearOnline()  // login
    aliceSession.statusMsg = "testtest" // change status message

    w.await(Timeout(10.seconds), Dismissals(5))
  }

  it should "be able to set user profile info" in {
    bobSession.initProfileInfo(
      iconId = 1,
      level = 25,
      wins = 999,
      tier = "GOLD",
      division = "III"
    )

    Thread.sleep(2000)

    aliceSession.findFriendById(bob.summId) match {
      case Good(f) =>
        f.name shouldEqual bob.inGameName
        f.level shouldEqual 25
        f.wins shouldEqual 999
        f.rankedTier shouldEqual Good("GOLD")
        f.rankedDivision shouldEqual Good("III")
      case Bad(notFond) => fail("unable to find bob")
    }
  }
}
