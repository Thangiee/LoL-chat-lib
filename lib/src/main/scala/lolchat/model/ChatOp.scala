package lolchat.model

import cats.data._
import lolchat._

object ChatOp {
  def apply[A](sess: Session => ChatF[A]): ChatOp[A] = ReaderT[ChatF, Session, A](sess)
}
