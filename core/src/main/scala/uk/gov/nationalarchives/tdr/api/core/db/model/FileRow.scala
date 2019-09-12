package uk.gov.nationalarchives.tdr.api.core.db.model

import java.time.Instant
import java.util.UUID

case class FileRow (id: Option[UUID] = None, path: String, consignmentId: Int, fileSize: Int, lastModifiedDate: Instant, fileName: String)
