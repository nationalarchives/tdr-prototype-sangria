package uk.gov.nationalarchives.tdr.api.core.db.model

import java.util.{Date, UUID}

case class FileRow (id: Option[UUID] = None, path: String, consignmentId: Int, fileSize: Int, lastModifiedDate: Date, fileName: String)
