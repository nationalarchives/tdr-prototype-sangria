package uk.gov.nationalarchives.tdr.api.core

import uk.gov.nationalarchives.tdr.api.core.db.dao.FileStatusDao
import uk.gov.nationalarchives.tdr.api.core.graphql.service.FileStatusService
import scala.concurrent.ExecutionContext.Implicits.global

object WsServer {

    private val fileStatusService = new FileStatusService(new FileStatusDao())

    def send(id: Int) = {
        println(s"Getting status for consignment $id")
        fileStatusService.getFileCheckStatus(id)
    }
}
