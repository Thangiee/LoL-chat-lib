package lolchat

import cats.data.XorT

import scala.concurrent.{ExecutionContext, Future}

package object data {
  type ChatResult[A] = XorT[Future, ChatError, A]
  type ExeCtx = ExecutionContext
}
