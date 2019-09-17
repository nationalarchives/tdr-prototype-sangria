package uk.gov.nationalarchives.tdr.api.httpserver

import akka.actor.{Actor, Props}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, OverflowStrategy}
import uk.gov.nationalarchives.tdr.api.core.WsServer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

case object GetWebsocketFlow

object WsActor {
  def props: Props = Props[WsActor]
}

class WsActor extends Actor {
  implicit val am = ActorMaterializer()

  val (down, publisher) = Source
    .actorRef[String](1000, OverflowStrategy.fail)
    .toMat(Sink.asPublisher(fanout = false))(Keep.both)
    .run()

  override def receive = {
    case GetWebsocketFlow =>

      val flow = Flow.fromGraph(GraphDSL.create() { implicit b =>

        // only works with TextMessage. Extract the body and sends it to self
        val textMsgFlow = b.add(Flow[Message]
          .mapAsync(1) {
            case tm: TextMessage => tm.toStrict(3.seconds).map(_.text)
            case bm: BinaryMessage =>
              // consume the stream
              bm.dataStream.runWith(Sink.ignore)
              Future.failed(new Exception("yuck"))
          })

        val pubSrc = b.add(Source.fromPublisher(publisher).map(TextMessage(_)))

        textMsgFlow ~> Sink.foreach[String](self ! _)
        FlowShape(textMsgFlow.in, pubSrc.out)
      })

      sender ! flow

    case id: String =>
      down ! Await.result(WsServer.send(id.toInt), Duration.Inf).toString

    case id: Int => Await.result(WsServer.send(id.toInt), Duration.Inf).toString
  }
}


