package uk.gov.nationalarchives.tdr.api.core.monitoring

import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest}
import uk.gov.nationalarchives.tdr.api.core.db.dao.ConsignmentDao

import scala.concurrent.ExecutionContext

class Metrics(consignmentDao: ConsignmentDao)(implicit val executionContext: ExecutionContext) {
  private val metricNamespace = sys.env.getOrElse("CLOUDWATCH_METRIC_NAMESPACE", "TdrPrototypeDev")

  def recordConsignmentCreation(transferringBody: String): Unit = {
    recordConsignmentMetric("ConsignmentCreated", transferringBody)
  }

  def recordConsignmentTransferConfirmation(consignmentId: Int): Unit = {
    consignmentDao.get(consignmentId).foreach(_.foreach(consignment => {
      recordConsignmentMetric("ConsignmentTransferConfirmed", consignment.transferringBody)
    }))
  }

  private def recordConsignmentMetric(metricName: String, transferringBody: String): Unit = {
    val cloudWatchClient = CloudWatchAsyncClient.create()
    val transferringBodyDimension = Dimension.builder()
      .name("TransferringBody")
      .value(transferringBody)
      .build()
    val metricDatum = MetricDatum.builder()
      .metricName(metricName)
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
