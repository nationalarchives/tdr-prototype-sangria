package uk.gov.nationalarchives.tdr.api.core.db.backgroundtask

import software.amazon.awssdk.services.ecs.EcsClient
import software.amazon.awssdk.services.ecs.model._

object ConsignmentExport {

  private val ecsClient = EcsClient.create

  def startExport(consignmentId: Int): Unit = {
    // In Beta, the API should be decoupled from the export task using SNS as an intermediary. But that requires
    // extra configuration and a Lambda layer to trigger ECS, so in Alpha we start the task in ECS directly.

    val taskNameParam = sys.env.get("EXPORT_TASK_ID")

    taskNameParam match {
      case Some(taskId) => startExportTask(taskId, consignmentId)
      case None => println("No export task configured in EXPORT_TASK_ID, so skipping export")
    }
  }

  private def startExportTask(taskId: String, consignmentId: Int): Unit = {
    val clusterArn = sys.env("EXPORT_CLUSTER_ARN")
    val securityGroupId = sys.env("EXPORT_SECURITY_GROUP_ID")
    val subnetId = sys.env("EXPORT_SUBNET_ID")
    val containerId = sys.env("EXPORT_CONTAINER_ID")

    val awsVpcConfiguration = AwsVpcConfiguration.builder()
      .securityGroups(securityGroupId)
      .subnets(subnetId)
      .build
    val networkConfiguration = NetworkConfiguration.builder()
      .awsvpcConfiguration(awsVpcConfiguration)
      .build
    val consignmentIdParam = KeyValuePair.builder.name("CONSIGNMENT_ID").value(consignmentId.toString).build
    val containerOverride = ContainerOverride.builder
      .name(containerId)
      .environment(consignmentIdParam)
      .build
    val taskOverride = TaskOverride.builder.containerOverrides(containerOverride).build
    val runTaskRequest = RunTaskRequest.builder
      .taskDefinition(taskId)
      .launchType(LaunchType.FARGATE)
      .cluster(clusterArn)
      .networkConfiguration(networkConfiguration)
      .overrides(taskOverride)
      .build
    ecsClient.runTask(runTaskRequest)
  }
}
