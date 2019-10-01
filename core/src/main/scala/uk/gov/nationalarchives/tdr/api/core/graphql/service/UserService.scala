package uk.gov.nationalarchives.tdr.api.core.graphql.service

import uk.gov.nationalarchives.tdr.api.core.db.dao.UserDao
import uk.gov.nationalarchives.tdr.api.core.db.model.{Password, User}
import uk.gov.nationalarchives.tdr.api.core.graphql
import uk.gov.nationalarchives.tdr.api.core.graphql.{PasswordInfo, PasswordInput, PasswordResetToken, UserInput}

import scala.concurrent.{ExecutionContext, Future}

class UserService(userDao: UserDao)(implicit val executionContext: ExecutionContext) {


  def get(providerKey: String, providerId: String) = {
    val user = userDao.get(providerKey, providerId)
    user.map(_.map(u =>graphql.User(u.id.get, u.firstName, u.lastName, u.email, u.email, u.providerId)))
  }

  def create(userData: UserInput) = {
    val user = User(None, userData.firstName, userData.lastName, userData.email, userData.providerId, userData.email)
    userDao.create(user)
      .map(u =>graphql.User(u.id.get, u.firstName, u.lastName, u.email, u.providerKey, u.providerId))
  }

  def findPassword(providerKey: String) = {
    userDao.findPassword(providerKey).map(_.map(p => PasswordInfo(p.hasher, p.hash, p.salt)))
  }

  def addPassword(passwordInput: PasswordInput) = {
    val password = Password(passwordInput.providerKey, passwordInput.hasher, passwordInput.password, passwordInput.salt)
    userDao.addPassword(password)
      .map(p => PasswordInfo(password.hasher, password.hash, password.salt))
  }

  def updatePassword(passwordInput: PasswordInput) = {
    userDao.updatePassword(passwordInput)
  }

  def deletePassword(providerKey: String) = {
    userDao.deletePassword(providerKey)
  }

  def createResetPasswordToken(email: String) = {
    userDao.createOrUpdatePasswordResetToken(email).map(a => a.map(t => PasswordResetToken(email, t.token)))
  }

  def isPasswordResetTokenValid(email: String, token: String) = {
    userDao.getPasswordResetToken(email, token).map(t => t.nonEmpty)
  }
}
