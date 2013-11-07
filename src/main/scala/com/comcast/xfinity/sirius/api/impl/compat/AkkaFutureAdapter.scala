package com.comcast.xfinity.sirius.api.impl.compat

import scala.concurrent.{Await, Future => AkkaFuture}
import scala.concurrent.duration._
import java.util.concurrent.{TimeoutException, ExecutionException, TimeUnit, Future}
import scala.language.postfixOps

/**
 * Class encapsulating a {@link akka.dispatch.Future} in a
 * {@link java.util.concurrent.Future}
 *
 * @param akkaFuture the {@see akka.dispatch.Future} to wrap
 */
class AkkaFutureAdapter[T](akkaFuture: AkkaFuture[T]) extends Future[T] {

  /**
   * Not implemented, you may not cancel an Akka Future
   */
  def cancel(mayInterrupt: Boolean): Boolean =
    throw new IllegalStateException("Not implemented")

  /**
   * Always returns true, since this is not cancellable
   *
   * @return true always
   */
  def isCancelled: Boolean = false // if not cancellable can't be cancelled

  /**
   * {@inheritDoc}
   */
  def isDone: Boolean = akkaFuture.isCompleted

  /**
   * {@inheritDoc}
   */
  def get: T =
    try {
      // there is still no way in hell this can time out
      Await.result(akkaFuture, 7 days)
    } catch {
      case e: Throwable => throw new ExecutionException(e)
    }

  /**
   * {@inheritDoc}
   */
  @throws(classOf[ExecutionException])
  @throws(classOf[TimeoutException])
  def get(l: Long, timeUnit: TimeUnit) =
    try {
      Await.result(akkaFuture, timeUnit.toMillis(l) milliseconds)
    } catch {
      case te: TimeoutException => throw te
      case e: Throwable => throw new ExecutionException(e)
    }
}