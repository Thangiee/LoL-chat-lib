package lolchat

import cats.data.XorT

import scala.concurrent.{ExecutionContext, Future}

package object data {
  type AsyncResult[A] = XorT[Future, Error, A]
  type ExeCtx = ExecutionContext
}
