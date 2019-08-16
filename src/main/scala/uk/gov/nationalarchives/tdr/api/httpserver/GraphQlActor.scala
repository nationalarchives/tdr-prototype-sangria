package uk.gov.nationalarchives.tdr.api.httpserver

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import uk.gov.nationalarchives.tdr.api.core.{GraphQlRequest, GraphQlServer}

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
