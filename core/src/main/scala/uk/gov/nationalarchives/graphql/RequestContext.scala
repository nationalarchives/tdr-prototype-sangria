package uk.gov.nationalarchives.graphql

import uk.gov.nationalarchives.graphql.service.{ConsignmentService, SeriesService}

class RequestContext(val series: SeriesService, val consignments: ConsignmentService)
