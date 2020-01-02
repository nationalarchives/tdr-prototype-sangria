package uk.gov.nationalarchives.tdr.api.core.monitoring

import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest}

object Metrics {
  private val metricNamespace = sys.env.getOrElse("CLOUDWATCH_METRIC_NAMESPACE", "TdrPrototypeDev")

  def recordConsignmentCreation(transferringBody: String): Unit = {
    val cloudWatchClient = CloudWatchAsyncClient.create()
    val transferringBodyDimension = Dimension.builder()
      .name("TransferringBody")
      .value(transferringBody)
      .build()
    val metricDatum = MetricDatum.builder()
      .metricName("ConsignmentCreated")
      .value(1d)
      .dimensions(transferringBodyDimension)
      .build()
    val metricRequest = PutMetricDataRequest.builder()
      .namespace(metricNamespace)
      .metricData(metricDatum)
      .build()
    cloudWatchClient.putMetricData(metricRequest)
  }
}
