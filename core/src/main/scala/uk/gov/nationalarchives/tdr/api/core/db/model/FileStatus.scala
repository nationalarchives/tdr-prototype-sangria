package uk.gov.nationalarchives.tdr.api.core.db.model

case class FileStatus(id: Option[Int] = None, fileFormatVerified: Boolean, fileId: Int, clientSideChecksum: String, serverSideChecksum: String, antivirusStatus: String)
