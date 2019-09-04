package uk.gov.nationalarchives.tdr.api.core.db.model

import java.util.UUID

case class FileFormat(id: Option[Int], pronomId: String, fileId: UUID)

