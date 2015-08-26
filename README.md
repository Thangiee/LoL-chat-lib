# League-of-Legend-Chat-Lib-Scala
[![Dependency Status](https://www.versioneye.com/user/projects/557fbd2f38666400200000dd/badge.svg?style=flat)](https://www.versioneye.com/user/projects/557fbd2f38666400200000dd)

A scala wrapper([Smack](http://www.igniterealtime.org/projects/smack/)) library designed for chating and interacting with League of Legends chat servers.

Setup
-----
1. Download the [**jar**](https://github.com/Thangiee/League-of-Legend-Chat-Lib-Scala/tree/master/bin) and add it to your project. 

2. Import dependencies with SBT
  
  **Projects that target a JVM supporting Java7:**
  
    ```scala
    
    resolvers += "jcenter" at "https://jcenter.bintray.com/"
    
    libraryDependencies ++= Seq(
      "org.igniterealtime.smack" % "smack-java7" % "4.1.3",
      "com.github.thangiee" %% "league-of-legend-chat-lib-scala" % "0.2.1"
    ) 
    ```
    
  **Projects that target android:**
    
    ```scala
    
    resolvers += "jcenter" at "https://jcenter.bintray.com/"
    
    libraryDependencies ++= Seq(
      "org.igniterealtime.smack" % "smack-android" % "4.1.3",
      "com.github.thangiee" %% "league-of-legend-chat-lib-scala" % "0.2.1"
    )
    ```
  
  For other build tools look here: https://github.com/igniterealtime/Smack/wiki/Smack-4.1-Readme-and-Upgrade-Guide
  
Examples
--------
* Login to a session
  ```scala
  
  LoLChat.login("username", "password", region.NA).foreach { session => 
    // do stuff after a successful login
  }
  
  // login with error handling 
  LoLChat.login("username", "password", region.NA) match {
    case Good(session) => // do stuff after a successful login
    case Bad(NotConnected(url))              => println(s"Failed to connect to $url")
    case Bad(FailAuthentication(user, pass)) => println("Invalid username or password")
    case Bad(UnexpectedError(throwable))     => throwable.printStackTrace()
  }
  
  // get a logged in session
  LoLChat.findSession("username")
  ```
* Chat
  ```scala
  
    LoLChat.findSession("username").map { sess =>
    
      // send message to all friends
      sess.friends.foreach { f =>
        sess.sendMsg(f.id, s"hello ${f.name}")
      }
      
      // send message to specific friend
      sess.findFriendByName("Bob").foreach{ f =>
        sess.sendMsg(f.id, "Sup")
      }
      
      // listen for messages sent from friends
      sess.addReceiveMsgListener(new ReceiveMsgListener {
        def onReceivedMessage(fromId: String, textMsg: String): Unit = {
          sess.findFriendById(fromId).foreach(f => println(s"${f.name}: $textMsg"))
        }
      })
    }
  ```
  
* [Take a look at the test cases for more examples.](https://github.com/Thangiee/League-of-Legend-Chat-Lib-Scala/tree/master/src/test/scala-2.11)

