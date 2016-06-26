package lolchat.model

import cats.data._
import lolchat._

object ChatOp {
  def apply[A](sess: Session => Chat[A]): ChatOp[A] = ReaderT[Chat, Session, A](sess)
}
