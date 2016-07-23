package lolchat.data

import cats.data.{Xor, XorT}
import cats.std.future._

import scala.concurrent.Future

object ChatResult {

  def apply[A](futureXor: Future[Xor[ChatError, A]])(implicit ctx: ExeCtx): ChatResult[A] = XorT(futureXor)

  def apply[A](xor: => Xor[ChatError, A])(implicit ctx: ExeCtx): ChatResult[A] = XorT(futureInstance.pure(xor))

  def right[A](a: A)(implicit ctx: ExeCtx): ChatResult[A] = XorT.right[Future, ChatError, A](futureInstance.pure(a))

  def left[A](err: ChatError)(implicit ctx: ExeCtx): ChatResult[A] = XorT.left[Future, ChatError, A](futureInstance.pure(err))

  def pure[A](a: A)(implicit ctx: ExeCtx): ChatResult[A] = XorT.pure[Future, ChatError, A](a)

  def catchNonFatal[A](f: => A, g: Throwable => ChatError)(implicit ctx: ExeCtx): ChatResult[A] =
    futureInstance.attemptT(Future(f)).leftMap(g)

  def catchNonFatal[A](f: => A)(implicit ctx: ExeCtx): ChatResult[A] =
    futureInstance.attemptT(Future(f)).leftMap(err => ChatError(err.getMessage, err))
}
