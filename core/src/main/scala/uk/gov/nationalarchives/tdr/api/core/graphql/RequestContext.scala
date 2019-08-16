package uk.gov.nationalarchives.tdr.api.core.graphql

import uk.gov.nationalarchives.tdr.api.core.graphql.service.{ConsignmentService, SeriesService}

class RequestContext(val series: SeriesService, val consignments: ConsignmentService)
