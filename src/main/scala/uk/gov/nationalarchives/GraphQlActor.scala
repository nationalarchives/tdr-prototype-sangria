package uk.gov.nationalarchives

import akka.actor.{ Actor, ActorLogging, Props }

object GraphQlActor {
  def props: Props = Props[GraphQlActor]
}

class GraphQlActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case _ => sender() ! "hello from actor"
  }
}
