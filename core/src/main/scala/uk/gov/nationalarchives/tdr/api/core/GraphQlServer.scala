package uk.gov.nationalarchives.tdr.api.core

import io.circe.Json
import sangria.ast.Document
import sangria.execution.Executor
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import uk.gov.nationalarchives.tdr.api.core.db.dao._
import uk.gov.nationalarchives.tdr.api.core.graphql.service._
import uk.gov.nationalarchives.tdr.api.core.graphql.{DeferredResolver, GraphQlTypes, RequestContext}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object GraphQlServer {

  private val seriesService = new SeriesService(new SeriesDao)
  private val consignmentService = new ConsignmentService(new ConsignmentDao, seriesService)
  private val fileStatusService = new FileStatusService(new FileStatusDao())
  private val fileFormatService = new FileFormatService(new FileFormatDao())
  private val fileService = new FileService(new FileDao, fileStatusService, consignmentService, fileFormatService)
  private val userService = new UserService(new UserDao())
  private val requestContext = new RequestContext(seriesService, consignmentService, fileService, fileStatusService, fileFormatService, userService)

  def send(request: GraphQlRequest): Future[Json] = {
    println(s"Got GraphQL request '$request'")

    val query: Try[Document] = QueryParser.parse(request.query)

    query match {
      case Success(doc) =>
        val variables = request.variables.getOrElse(Json.obj())
        Executor.execute(
          GraphQlTypes.schema,
          doc, requestContext,
          operationName = request.operationName,
          variables = variables,
          deferredResolver = new DeferredResolver
        )
      case Failure(e) =>
        Future.failed(e)
    }
  }
}

case class GraphQlRequest(query: String, operationName: Option[String], variables: Option[Json])


