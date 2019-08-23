package uk.gov.nationalarchives.tdr.api.core.graphql

import uk.gov.nationalarchives.tdr.api.core.graphql.service.{ConsignmentService, FileService, FileStatusService, SeriesService}

class RequestContext(val series: SeriesService, val consignments: ConsignmentService, val files: FileService, val fileStatuses: FileStatusService)
