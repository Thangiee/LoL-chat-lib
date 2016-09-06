package lolchat.data

import cats.data.{Xor, XorT}
import cats.instances.future.{catsStdInstancesForFuture => futureInstance}

import scala.concurrent.Future

object AsyncResult {

  def apply[A](futureXor: Future[Xor[Error, A]])(implicit ctx: ExeCtx): AsyncResult[A] = XorT(futureXor)

  def apply[A](xor: => Xor[Error, A])(implicit ctx: ExeCtx): AsyncResult[A] = XorT(Future(xor))

  def right[A](a: A)(implicit ctx: ExeCtx): AsyncResult[A] = XorT.right[Future, Error, A](futureInstance.pure(a))

  def left[A](err: Error)(implicit ctx: ExeCtx): AsyncResult[A] = XorT.left[Future, Error, A](futureInstance.pure(err))

  def pure[A](a: A)(implicit ctx: ExeCtx): AsyncResult[A] = XorT.pure[Future, Error, A](a)

  def catchNonFatal[A](f: => A, g: Throwable => Error)(implicit ctx: ExeCtx): AsyncResult[A] =
    futureInstance.attemptT(Future(f)).leftMap(g)

  def catchNonFatal[A](f: => A)(implicit ctx: ExeCtx): AsyncResult[A] =
    futureInstance.attemptT(Future(f)).leftMap(err => Error(500, err.getMessage, err))
}
