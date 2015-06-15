package com.thangiee.lolchat

import com.thangiee.lolchat.changedPresence.ChangedPresence

/** Provide callback methods for [[Session#setFriendListListener]] */
trait FriendListListener {

  /** Called after a friend request is received.
    *
    * @param fromId the summoner id belonging to the received friend request
    * @return true to accept the friend request, otherwise false to reject
    */
  def onReceivedFriendRequest(fromId: String): Boolean

  /** Called after a friend is added
    * @param id summoner id of the added friend */
  def onFriendAdded(id: String): Unit

  /** Called after a friend is removed
    * @param id summoner id of the removed friend */
  def onFriendRemoved(id: String): Unit

  /** Called after the presence of the a friend changed
    *
    * @note do use pattern matching on ChangedPresence
    * @param friend the friend with the changed presence
    * @param changedPresence can be one of the following type:
    *                        - Available (green circle)
    *                        - AFK (red circle)
    *                        - Playing (yellow circle)
    *                        - Login
    *                        - Logout
    *                        - StatusMsg
    */
  def onFriendPresenceChanged(friend: FriendEntity)(changedPresence: ChangedPresence): Unit
}
