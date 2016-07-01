# League-of-Legend-Chat-Lib-Scala
[![Dependency Status](https://www.versioneye.com/user/projects/557fbd2f38666400200000dd/badge.svg?style=flat)](https://www.versioneye.com/user/projects/557fbd2f38666400200000dd)

Pure functional library for interacting with League of Legends chat servers.

Setup
-----
* Import with SBT
  
  **Projects that target a JVM supporting Java7+:**
  
    ```scala
    
    resolvers += "jcenter" at "https://jcenter.bintray.com/"
    
    libraryDependencies ++= Seq(
      "org.igniterealtime.smack" % "smack-java7" % "4.1.7",
      "com.github.thangiee" %% "league-of-legend-chat-lib-scala" % "0.3.0"
    ) 
    ```
    
  **Projects that target android:**
    
    ```scala
    
    resolvers += "jcenter" at "https://jcenter.bintray.com/"
    
    libraryDependencies ++= Seq(
      "org.igniterealtime.smack" % "smack-android" % "4.1.7",
      "com.github.thangiee" %% "league-of-legend-chat-lib-scala" % "0.3.0"
    )
    ```
  
  For other build tools look here: https://github.com/igniterealtime/Smack/wiki/Smack-4.1-Readme-and-Upgrade-Guide
  
Usage
--------
Import library

```scala
import lolchat._
import lolchat.model._
```

Create a session and login.

```scala
val sess = Session("username", "passwd", Region.NA)
LoLChat.run(login(sess))
```

You can combine multiple operations with for-comprehension.

```scala
val getFriends =
  for {
    _ <- login
    f <- friends
  } yield f

val result: ChatResult[Vector[Friend]] = LoLChat.run(getFriends(sess))
```

Notice resulting type. `LoLChat.run(...)` will return `ChatResult[...]` which is an asynchronous computation that can
either be a `ChatError` type or some successful type determined by the operation (in this case `Vector[Friend]`).

With that said, you can handle errors that may occur like invalid credentials, connection issues, etc...

```scala
LoLChat.run(getFriends(sess)).fold(
  error   => println(error.msg),
  friends => friends.foreach(println)
)
```

**See all operations you can use [here](#src\main\scala\lolchat\ops.scala)**

---

Each Session also contain event streams:

* Incoming message stream

```scala
sess.msgStream.foreach(msg => println(s"From summId:${msg.fromId} message:${msg.txt}"))
```

* Friend list event stream

```scala
sess.friendListStream.foreach {
  case FriendAdded(summId)   =>
  case FriendRemoved(summId) =>
  case FriendRequest(summId) =>
  case FriendUpdated(friend) =>
}
```

* Connection event Stream

```scala
sess.connectionEventStream.foreach {
  case ConnectionLost   =>
  case Reconnected      =>
  case ReconnectFailed  =>
  case ReconnectIn(sec) =>
}
```