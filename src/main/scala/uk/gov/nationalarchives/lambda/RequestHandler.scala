package uk.gov.nationalarchives.lambda

import java.io.{ InputStream, OutputStream }

import com.amazonaws.services.lambda.runtime.{ Context, RequestStreamHandler }

import scala.io.Source

class RequestHandler extends RequestStreamHandler {
  override def handleRequest(inputStream: InputStream, outputStream: OutputStream, context: Context): Unit = {
    val input = Source.fromInputStream(inputStream, "UTF-8").mkString
    outputStream.write(s"Hello world - $input".getBytes)
  }
}
