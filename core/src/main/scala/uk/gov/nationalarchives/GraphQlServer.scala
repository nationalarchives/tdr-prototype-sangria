package uk.gov.nationalarchives

import io.circe.Json
import sangria.ast.Document
import sangria.execution.Executor
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema.{Argument, Field, ListType, ObjectType, Schema, StringType, fields}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object GraphQlServer {

  private val ConsignmentType = deriveObjectType[ConsignmentDao, Consignment]()
  private val ConsignmentNameArg = Argument("name", StringType)

  private val QueryType = ObjectType("Query", fields[ConsignmentDao, Unit](
    Field("getConsignments", ListType(ConsignmentType), resolve = ctx => ctx.ctx.all)
  ))

  private val MutationType = ObjectType("Mutation", fields[ConsignmentDao, Unit](
    Field(
      "createConsignment",
      ConsignmentType,
      arguments = List(ConsignmentNameArg),
      resolve = ctx => ctx.ctx.create(Consignment(ctx.arg(ConsignmentNameArg))))
  ))

  private val schema = Schema(QueryType, Some(MutationType))

  def send(request: GraphQlRequest): Future[Json] = {
    println(s"Got GraphQL request '$request'")

    val query: Try[Document] = QueryParser.parse(request.query)

    query match {
      case Success(doc) =>
        Executor.execute(schema, doc, ConsignmentDao)
      case Failure(e) =>
        Future.failed(e)
    }
  }
}

case class GraphQlRequest(query: String)

trait ConsignmentDao {
  def all: Future[Seq[Consignment]]
  def create(consignment: Consignment): Future[Consignment]
}

object ConsignmentDao extends ConsignmentDao {

  // Load PostgreSQL driver into classpath
  Class.forName("org.postgresql.Driver")

  val db = Database.forURL(
    url = "jdbc:postgresql://localhost/tdrapi",
    user = "postgres",
    password = "devdbpassword",
    driver = "org.postgresql.Driver"
  )

  val consignments = TableQuery[Consignments]

  override def all: Future[Seq[Consignment]] = {
    db.run(consignments.result).map(consignmentNames => {
      consignmentNames.map(name => Consignment(name))
    })
  }

  override def create(consignment: Consignment): Future[Consignment] = {
    val insertAction = consignments += (consignment.name)
    db.run(insertAction).map(_ => consignment)
  }
}

case class Consignment(name: String)

class Consignments(tag: Tag) extends Table[(String)](tag, "consignments") {
  def name = column[String]("name")

  override def * = (name)
}