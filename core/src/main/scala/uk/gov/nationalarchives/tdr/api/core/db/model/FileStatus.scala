package uk.gov.nationalarchives.tdr.api.core.db.model

import java.util.UUID

case class FileStatus(id: Option[Int] = None, fileFormatVerified: Boolean, fileId: UUID, clientSideChecksum: String, serverSideChecksum: String, antivirusStatus: String)
