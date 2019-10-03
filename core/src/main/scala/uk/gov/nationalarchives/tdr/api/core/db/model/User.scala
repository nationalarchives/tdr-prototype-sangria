package uk.gov.nationalarchives.tdr.api.core.db.model

case class User(id: Option[Int], firstName: String, lastName: String, email: String, providerId: String, providerKey: String)
case class Password(key: String, hasher: String, hash: String, salt: Option[String])
case class PasswordResetToken(email: String, token: String, expiry: String)
case class TotpInfo(id: Option[Int], providerKey: String, sharedKey: String)
case class TotpScratchCode(id: Option[Int], hasher: String, password: String, salt: Option[String], totpInfoId: Int)