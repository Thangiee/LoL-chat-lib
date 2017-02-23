package lolchat

import cats.data.EitherT

import scala.concurrent.{ExecutionContext, Future}

package object data {
  type AsyncResult[A] = EitherT[Future, Error, A]
  type ExeCtx = ExecutionContext
}
