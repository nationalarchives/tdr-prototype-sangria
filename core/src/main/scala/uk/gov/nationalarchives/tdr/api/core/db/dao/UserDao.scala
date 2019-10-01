package uk.gov.nationalarchives.tdr.api.core.db.dao

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.{Date, UUID}

import shapeless.Succ
import slick.dbio.Effect

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, QueryBase}
import slick.lifted.ProvenShape.proveShapeOf
import slick.sql.FixedSqlStreamingAction
import uk.gov.nationalarchives.tdr.api.core.db.{DbConnection, model}
import uk.gov.nationalarchives.tdr.api.core.db.model.{Password, PasswordResetToken, User}
import uk.gov.nationalarchives.tdr.api.core.db.dao.UserDao.{passwordResetTokenTable, passwordTable, userTable}
import uk.gov.nationalarchives.tdr.api.core.graphql.PasswordInput

import scala.util.Success

class UserDao(implicit val executionContext: ExecutionContext) {

  private val db = DbConnection.db

  private val insertQuery = userTable returning userTable.map(_.id) into ((user, id) => user.copy(id = Some(id)))
  private val passwordInsertQuery = passwordTable returning passwordTable.map(_.key) into ((password, key) => password.copy(key = key))
  private val passwordResetInsertQuery = passwordResetTokenTable returning passwordResetTokenTable.map(_.email) into ((tokenReset, email) => tokenReset.copy(email = email))

  def get(providerKey: String, providerId: String): Future[Option[User]] = {
    val result: FixedSqlStreamingAction[Seq[User], User, Effect.Read] = userTable.filter(u => u.providerKey === providerKey && u.providerId === providerId).result
    db.run(result).map(_.headOption)
  }

  def create(user: User): Future[User] = {
    db.run(insertQuery += user)
  }

  def findPassword(providerKey: String): Future[Option[Password]] = {
    db.run(passwordTable.filter(p => p.key === providerKey).result).map(_.headOption)
  }

  def addPassword(password: Password): Future[Password] = {
    db.run(passwordInsertQuery += password)
  }

  def updatePassword(passwordInput: PasswordInput) = {
    val q = for {
      password <- passwordTable if password.key === passwordInput.providerKey
    } yield (password.hasher, password.hash, password.salt)

    db.run(q.update(passwordInput.hasher, passwordInput.password, passwordInput.salt))
  }

  def deletePassword(providerKey: String) = {
    db.run(passwordTable.filter(password => password.key === providerKey).delete)
  }

  def createOrUpdatePasswordResetToken(email: String): Future[Option[PasswordResetToken]] = {
    val token = UUID.randomUUID().toString
    val time = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now().plusHours(4))
    val passwordResetToken = PasswordResetToken(email, token, time)
    db.run(passwordResetInsertQuery.insertOrUpdate(passwordResetToken))
        .map(e => Option.apply(PasswordResetToken(email, token, time)) )
  }

  def getPasswordResetToken(email: String, token: String): Future[Seq[PasswordResetToken]] = {
    val now = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())
    db.run(passwordResetTokenTable.filter(t => t.email === email && t.token === token && t.expiry >= now).result)
  }
}

class Users(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Int]("user_id", O.PrimaryKey, O.AutoInc)

  def firstName = column[String]("first_name")

  def lastName = column[String]("last_name")

  def email = column[String]("email")

  def providerId = column[String]("provider_id")

  def providerKey = column[String]("provider_key")

  override def * : ProvenShape[User] = (id.?, firstName, lastName, email, providerId, providerKey).mapTo[User]
}

class Passwords(tag: Tag) extends Table[Password](tag, "password") {
  def key = column[String]("provider_key", O.PrimaryKey)

  def hasher = column[String]("hasher")

  def hash = column[String]("hash")

  def salt = column[Option[String]]("salt")

  override def * = (key, hasher, hash, salt).mapTo[Password]
}

class PasswordResetTokens(tag: Tag) extends Table[PasswordResetToken](tag, "password_reset_token") {
  def email = column[String]("email", O.PrimaryKey)

  def token = column[String]("token")

  def expiry = column[String]("expiry")

  override def * = (email, token, expiry).mapTo[PasswordResetToken]
}

object UserDao {
  val userTable = TableQuery[Users]
  val passwordTable = TableQuery[Passwords]
  val passwordResetTokenTable = TableQuery[PasswordResetTokens]

}
