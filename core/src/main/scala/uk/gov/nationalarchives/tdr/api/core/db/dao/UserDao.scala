package uk.gov.nationalarchives.tdr.api.core.db.dao

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import slick.dbio.Effect

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, QueryBase}
import slick.lifted.ProvenShape.proveShapeOf
import slick.sql.FixedSqlStreamingAction

import uk.gov.nationalarchives.tdr.api.core.db.{DbConnection}
import uk.gov.nationalarchives.tdr.api.core.db.model.{Password, PasswordResetToken, TotpInfo, TotpScratchCode, User}
import uk.gov.nationalarchives.tdr.api.core.db.dao.UserDao.{passwordResetTokenTable, passwordTable, totpInfoTable, totpScratchCodes, userTable}
import uk.gov.nationalarchives.tdr.api.core.graphql.{PasswordInput, TotpInfoOutput, TotpScratchCodesOuput}


class UserDao(implicit val executionContext: ExecutionContext) {

  private val db = DbConnection.db

  private val insertQuery = userTable returning userTable.map(_.id) into ((user, id) => user.copy(id = Some(id)))
  private val passwordInsertQuery = passwordTable returning passwordTable.map(_.key) into ((password, key) => password.copy(key = key))
  private val passwordResetInsertQuery = passwordResetTokenTable returning passwordResetTokenTable.map(_.email) into ((tokenReset, email) => tokenReset.copy(email = email))
  private val totpInfoQuery = totpInfoTable returning totpInfoTable.map(_.id) into ((totpInfo, id) => totpInfo.copy(id = id))
  private val totpScratchCodeQuery = totpScratchCodes returning totpScratchCodes.map(_.id) into ((totpScratchCodes, id) => totpScratchCodes.copy(id = id))

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

  def findTotpInfo(providerKey: String) = {
    db.run(totpInfoTable.filter(t => t.providerKey === providerKey).result.headOption)
  }

  def findTotpScratchCodes(infoId: Int) = {
    db.run(totpScratchCodes.filter(t => t.totpInfoId === infoId).result)
  }

  def addTotpInfo(totpInfo: TotpInfo): Future[TotpInfo] = {
    db.run(totpInfoQuery += totpInfo)
  }

  def addTotpScratchCodes(scratchCodes: Seq[TotpScratchCode]): Future[Seq[TotpScratchCode]] = {
    db.run(totpScratchCodeQuery ++= scratchCodes)
  }

  def deleteTotpScratchCodes(providerKey: String) = {
    db.run(sqlu"delete from totp_scratch_codes c using totp_info i where i.id = c.totp_info_id and i.provider_key = $providerKey;")
  }

  def deleteTotpInfo(providerKey: String) = {
    db.run(totpInfoTable.filter(t => t.providerKey === providerKey).delete)
  }

  def createOrUpdatePasswordResetToken(email: String): Future[Option[PasswordResetToken]] = {
    val token = UUID.randomUUID().toString
    val time = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now().plusHours(4))
    val passwordResetToken = PasswordResetToken(email, token, time)
    db.run(passwordResetInsertQuery.insertOrUpdate(passwordResetToken))
      .map(e => Option.apply(PasswordResetToken(email, token, time)))
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

class TotpInfos(tag: Tag) extends Table[TotpInfo](tag, "totp_info") {
  def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)

  def providerKey = column[String]("provider_key")

  def sharedKey = column[String]("shared_key")

  override def * = (id, providerKey, sharedKey).mapTo[TotpInfo]
}

class TotpScratchCodes(tag: Tag) extends Table[TotpScratchCode](tag, "totp_scratch_codes") {
  def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)

  def hasher = column[String]("hasher")

  def password = column[String]("password")

  def salt = column[Option[String]]("salt")

  def totpInfoId = column[Int]("totp_info_id")

  def totpInfo = foreignKey("totp_info_fk", totpInfoId, totpInfoTable)(_.id.get)

  override def * = (id, hasher, password, salt, totpInfoId).mapTo[TotpScratchCode]
}

object UserDao {
  val userTable = TableQuery[Users]
  val passwordTable = TableQuery[Passwords]
  val passwordResetTokenTable = TableQuery[PasswordResetTokens]
  val totpInfoTable = TableQuery[TotpInfos]
  val totpScratchCodes = TableQuery[TotpScratchCodes]

}
