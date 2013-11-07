package com.comcast.xfinity.sirius.api.impl.paxos

import akka.actor.{Props, ReceiveTimeout, ActorRef, Actor}
import scala.concurrent.duration._
import com.comcast.xfinity.sirius.api.impl.paxos.LeaderWatcher.{LeaderPong, DifferentLeader, LeaderGone}
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.language.postfixOps

object LeaderPinger {
  case object Ping
  case class Pong(leaderBallot: Option[Ballot])

  /**
   * Create Props for a LeaderPinger actor.
   *
   * @param leaderToWatch ref for currently elected leader to keep an eye on
   * @param expectedBallot last-known "elected" leader ballot
   * @param replyTo actor to inform about elected leader state
   * @param timeoutMs how long to wait for a response before declaring leader dead
   * @return  Props for creating this actor, which can then be further configured
   *         (e.g. calling `.withDispatcher()` on it)
   */
  def props(leaderToWatch: ActorRef, expectedBallot: Ballot, replyTo: ActorRef, timeoutMs: Int): Props = {
    Props(classOf[LeaderPinger], leaderToWatch, expectedBallot, replyTo, timeoutMs)
  }
}

private[paxos] class LeaderPinger(leaderToWatch: ActorRef, expectedBallot: Ballot, replyTo: ActorRef, timeoutMs: Int) extends Actor {
    import LeaderPinger._

  context.setReceiveTimeout(timeoutMs milliseconds)

  leaderToWatch ! Ping
  val pingSent = System.currentTimeMillis()

  def receive = {
    case Pong(Some(leaderBallot)) if leaderBallot != expectedBallot =>
      replyTo ! DifferentLeader(leaderBallot)
      context.stop(self)

    case Pong(Some(_)) =>
      replyTo ! LeaderPong(System.currentTimeMillis() - pingSent)
      context.stop(self)

    case Pong(None) =>
      replyTo ! LeaderGone
      context.stop(self)

    case ReceiveTimeout =>
      replyTo ! LeaderGone
      context.stop(self)
  }
}
