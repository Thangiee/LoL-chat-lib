import java.util.concurrent.Executors

import cats.data._
import cats.free.Free
import lolchat.free.ChatF
import lolchat.model.{ChatError, Session}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

package object lolchat extends AnyRef with ops {
  type Chat[A] = Free[ChatF, A]
  type ChatOp[A] = ReaderT[Chat, Session, A]
  type ChatResult[A] = XorT[Future, ChatError, A]
  type ExeCtx = ExecutionContext
  type ErrMsg = String
}
