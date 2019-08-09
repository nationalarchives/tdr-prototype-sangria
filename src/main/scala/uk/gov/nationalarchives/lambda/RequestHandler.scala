package uk.gov.nationalarchives.lambda

import java.io.{ InputStream, OutputStream }

import com.amazonaws.services.lambda.runtime.{ Context, RequestStreamHandler }
import io.circe.generic.auto._
import io.circe.parser.decode
import uk.gov.nationalarchives.{ GraphQlRequest, GraphQlServer }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Success }

class RequestHandler extends RequestStreamHandler {
  override def handleRequest(inputStream: InputStream, outputStream: OutputStream, context: Context): Unit = {
    val inputString = Source.fromInputStream(inputStream, "UTF-8").mkString

    println("Input event:")
    println(inputString)

    val parsedRequest = decode[GraphQlRequest](inputString)

    parsedRequest match {
      case Left(failure) => throw new RuntimeException(failure)
      case Right(graphQlRequest) =>
        val response = GraphQlServer.send(graphQlRequest)
        response.onComplete {
          case Success(json) => outputStream.write(json.toString.getBytes)
          case Failure(e) => throw e
        }
    }
  }
}
