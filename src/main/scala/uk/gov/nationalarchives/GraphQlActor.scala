package uk.gov.nationalarchives

import akka.actor.{ Actor, ActorLogging, Props }
import akka.pattern.pipe

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure

object GraphQlActor {
  def props: Props = Props[GraphQlActor]
}

class GraphQlActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case request: GraphQlRequest =>
      GraphQlServer.send(request) pipeTo sender()
    case other =>
      println(s"Got other '$other'")
      sender() ! Failure(new RuntimeException(s"Could not parse message $other"))
  }
}
