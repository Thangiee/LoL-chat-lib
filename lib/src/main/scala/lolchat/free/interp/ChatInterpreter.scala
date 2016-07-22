package lolchat.free.interp

import cats._
import lolchat._
import lolchat.free.ChatF

import scala.language.higherKinds

trait ChatInterpreter[M[_]] {
  type Interpreter = (ChatF ~> M)
  def interpreter: Interpreter
  def run[A](chat: Chat[A])(implicit M: Monad[M]): M[A] = chat.foldMap(interpreter)
}
