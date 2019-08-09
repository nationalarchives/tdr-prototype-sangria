package uk.gov.nationalarchives.lambda

import java.io.{ InputStream, OutputStream }

import com.amazonaws.services.lambda.runtime.{ Context, RequestStreamHandler }
import io.circe.generic.auto._
import io.circe.parser.decode
import uk.gov.nationalarchives.{ GraphQlRequest, GraphQlServer }

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.Source

class RequestHandler extends RequestStreamHandler {
  override def handleRequest(inputStream: InputStream, outputStream: OutputStream, context: Context): Unit = {
    val inputString = Source.fromInputStream(inputStream, "UTF-8").mkString

    println("Input event:")
    println(inputString)

    val parsedRequest = decode[GraphQlRequest](inputString)

    val futureResponse = parsedRequest match {
      case Left(failure) => throw new RuntimeException(failure)
      case Right(graphQlRequest) =>
        val response = GraphQlServer.send(graphQlRequest)
        response.map { json =>
          println("Returning:")
          println(json.toString)
          outputStream.write(json.toString.getBytes)
        }
    }

    println("End of handleRequest method")

    Await.result(futureResponse, 5.seconds)
  }
}
