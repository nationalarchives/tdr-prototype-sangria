package uk.gov.nationalarchives.model

case class SeriesDbData(id: Option[Int] = None, name: String, description: String)

case class ConsignmentDbData(id: Option[Int] = None, name: String, seriesId: Int)
