package uk.gov.nationalarchives.tdr.api.core.db.model

case class ConsignmentRow(
                           id: Option[Int] = None,
                           name: String,
                           progress: String,
                           seriesId: Int,
                           creator: String,
                           transferringBody: String
                         )
