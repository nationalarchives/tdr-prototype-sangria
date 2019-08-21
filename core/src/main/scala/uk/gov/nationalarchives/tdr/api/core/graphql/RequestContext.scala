package uk.gov.nationalarchives.tdr.api.core.graphql

import uk.gov.nationalarchives.tdr.api.core.graphql.service.{ConsignmentService, FileService, SeriesService}

class RequestContext(val series: SeriesService, val consignments: ConsignmentService, val files: FileService)
