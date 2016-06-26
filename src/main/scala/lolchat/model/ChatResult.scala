package lolchat.model

import cats.data.{Xor, XorT}
import lolchat._

import scala.concurrent.Future

object ChatResult {

  def apply[A](xor: Xor[ChatError, A])(implicit ec: ExeCtx): ChatResult[A] = XorT(Future(xor))

  def right[A](a: A)(implicit ec: ExeCtx): ChatResult[A] = XorT.right[Future, ChatError, A](Future.successful(a))

  def left[A](err: ChatError)(implicit ec: ExeCtx): ChatResult[A] = XorT.left[Future, ChatError, A](Future.successful(err))

  def pure[A](a: A)(implicit ec: ExeCtx): ChatResult[A] = XorT.pure[Future, ChatError, A](a)

  def catchNonFatal[A](f: => A, g: Throwable => ChatError)(implicit ec: ExeCtx): ChatResult[A] =
    XorT(Future(Xor.catchNonFatal(f).leftMap(g)))

  def catchNonFatal[A](f: => A)(implicit ec: ExeCtx): ChatResult[A] =
    XorT(Future(Xor.catchNonFatal(f).leftMap(err => ChatError(err.getMessage, err))))
}
