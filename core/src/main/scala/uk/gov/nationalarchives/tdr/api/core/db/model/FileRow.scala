package uk.gov.nationalarchives.tdr.api.core.db.model

case class FileRow (id: Option[Int] = None, path: String, consignmentId: Int, fileSize: Int, lastModifiedDate: String, fileName: String)
