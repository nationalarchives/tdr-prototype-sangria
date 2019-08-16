package uk.gov.nationalarchives

import io.circe.Json
import sangria.ast.Document
import sangria.execution.Executor
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema.{Argument, Field, IntType, ListType, ObjectType, OptionType, Schema, StringType, fields}
import uk.gov.nationalarchives.db.{ConsignmentDao, SeriesDao}
import uk.gov.nationalarchives.model.ConsignmentDbData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object GraphQlServer {

  implicit private val SeriesType: ObjectType[Unit, SeriesResponse] = deriveObjectType[Unit, SeriesResponse]()
  implicit private val ConsignmentType: ObjectType[Unit, ConsignmentResponse] = deriveObjectType[Unit, ConsignmentResponse]()

  private val ConsignmentNameArg = Argument("name", StringType)
  private val ConsignmentIdArg = Argument("id", IntType)
  private val SeriesIdArg = Argument("seriesId", IntType)

  private val QueryType = ObjectType("Query", fields[RequestContext, Unit](
    Field("getConsignments", ListType(ConsignmentType), resolve = ctx => ctx.ctx.consignments.all),
    Field(
      "getConsignment",
      OptionType(ConsignmentType),
      arguments = List(ConsignmentIdArg),
      resolve = ctx => ctx.ctx.consignments.get(ctx.arg(ConsignmentIdArg))
    )
  ))

  private val MutationType = ObjectType("Mutation", fields[RequestContext, Unit](
    Field(
      "createConsignment",
      ConsignmentType,
      arguments = List(ConsignmentNameArg, SeriesIdArg),
      resolve = ctx => ctx.ctx.consignments.create(ctx.arg(ConsignmentNameArg), ctx.arg(SeriesIdArg)))
  ))

  private val schema = Schema(QueryType, Some(MutationType))

  // TODO: Inject dependencies
  private val seriesService = new SeriesService(new SeriesDao)
  private val consignmentService = new ConsignmentService(new ConsignmentDao, seriesService)
  private val requestContext = new RequestContext(seriesService, consignmentService)

  def send(request: GraphQlRequest): Future[Json] = {
    println(s"Got GraphQL request '$request'")

    val query: Try[Document] = QueryParser.parse(request.query)

    query match {
      case Success(doc) =>
        Executor.execute(schema, doc, requestContext)
      case Failure(e) =>
        Future.failed(e)
    }
  }
}

case class GraphQlRequest(query: String)

case class SeriesResponse(id: Int, name: String, description: String)
case class ConsignmentResponse(id: Int, name: String, series: SeriesResponse)

class RequestContext(val series: SeriesService, val consignments: ConsignmentService)

class ConsignmentService(consignmentDao: ConsignmentDao, seriesService: SeriesService) {

  def all: Future[Seq[ConsignmentResponse]] = {
    consignmentDao.all.flatMap(consignments => {
      val consignmentResponses = consignments.map(consignment =>
        seriesService.get(consignment.seriesId).map(series =>
          ConsignmentResponse(consignment.id.get, consignment.name, series.get)
        )
      )
      Future.sequence(consignmentResponses)
    })
  }

  def get(id: Int): Future[Option[ConsignmentResponse]] = {
    consignmentDao.get(id).flatMap(_.map(consignment =>
      seriesService.get(consignment.seriesId).map(series =>
        ConsignmentResponse(consignment.id.get, consignment.name, series.get)
      )
    ) match {
      case Some(f) => f.map(Some(_))
      case None => Future.successful(None)
    })
  }

  def create(name: String, seriesId: Int): Future[ConsignmentResponse] = {
    val newConsignment = ConsignmentDbData(None, name, seriesId)
    val creationResult = consignmentDao.create(newConsignment)

    creationResult.flatMap(persistedConsignment =>
      seriesService.get(persistedConsignment.seriesId).map(series =>
        ConsignmentResponse(persistedConsignment.id.get, persistedConsignment.name, series.get)
      )
    )
  }
}

class SeriesService(seriesDao: SeriesDao) {
  // TODO: This is a repetitive way of getting the series, and it suffers from the N+1 problem. Replace it with GraphQL
  // deferred resolvers.
  def get(id: Int): Future[Option[SeriesResponse]] = {
    seriesDao.get(id).map(_.map(series =>
      SeriesResponse(series.id.get, series.name, series.description)
    ))
  }
}