/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.zeebe.tasklist.qa.util;

import static io.zeebe.tasklist.util.ThreadUtil.sleepFor;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.ZeebeFuture;
import io.zeebe.client.api.command.CompleteJobCommandStep1;
import io.zeebe.client.api.command.CreateWorkflowInstanceCommandStep1;
import io.zeebe.client.api.command.DeployWorkflowCommandStep1;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.WorkflowInstanceEvent;
import io.zeebe.client.api.worker.JobWorker;
import io.zeebe.model.bpmn.BpmnModelInstance;
import java.time.Duration;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ZeebeTestUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZeebeTestUtil.class);

  private static Random random = new Random();

  public static String deployWorkflow(
      ZeebeClient client, BpmnModelInstance workflowModel, String resourceName) {
    final DeployWorkflowCommandStep1 deployWorkflowCommandStep1 =
        client.newDeployCommand().addWorkflowModel(workflowModel, resourceName);
    final DeploymentEvent deploymentEvent =
        ((DeployWorkflowCommandStep1.DeployWorkflowCommandBuilderStep2) deployWorkflowCommandStep1)
            .send()
            .join();
    LOGGER.debug("Deployment of resource [{}] was performed", resourceName);
    return String.valueOf(deploymentEvent.getWorkflows().get(0).getWorkflowKey());
  }

  public static ZeebeFuture<WorkflowInstanceEvent> startWorkflowInstanceAsync(
      ZeebeClient client, String bpmnProcessId, String payload) {
    final CreateWorkflowInstanceCommandStep1.CreateWorkflowInstanceCommandStep3
        createWorkflowInstanceCommandStep3 =
            client.newCreateInstanceCommand().bpmnProcessId(bpmnProcessId).latestVersion();
    if (payload != null) {
      createWorkflowInstanceCommandStep3.variables(payload);
    }
    return createWorkflowInstanceCommandStep3.send();
  }

  public static long startWorkflowInstance(
      ZeebeClient client, String bpmnProcessId, String payload) {
    final CreateWorkflowInstanceCommandStep1.CreateWorkflowInstanceCommandStep3
        createWorkflowInstanceCommandStep3 =
            client.newCreateInstanceCommand().bpmnProcessId(bpmnProcessId).latestVersion();
    if (payload != null) {
      createWorkflowInstanceCommandStep3.variables(payload);
    }
    final WorkflowInstanceEvent workflowInstanceEvent =
        createWorkflowInstanceCommandStep3.send().join();
    LOGGER.debug("Workflow instance created for workflow [{}]", bpmnProcessId);
    return workflowInstanceEvent.getWorkflowInstanceKey();
  }

  public static void completeTask(
      ZeebeClient client, String jobType, String workerName, String payload, int count) {
    final int[] countCompleted = {0};
    final JobWorker jobWorker =
        client
            .newWorker()
            .jobType(jobType)
            .handler(
                (jobClient, job) -> {
                  if (countCompleted[0] < count) {
                    CompleteJobCommandStep1 completeJobCommandStep1 =
                        jobClient.newCompleteCommand(job.getKey());
                    if (payload != null) {
                      completeJobCommandStep1 = completeJobCommandStep1.variables(payload);
                    }
                    completeJobCommandStep1.send().join();
                    LOGGER.debug("Task completed jobKey [{}]", job.getKey());
                    countCompleted[0]++;
                    if (countCompleted[0] % 1000 == 0) {
                      LOGGER.info("{} jobs completed ", countCompleted[0]);
                    }
                  }
                })
            .name(workerName)
            .timeout(Duration.ofSeconds(2))
            .open();
    // wait till all requested tasks are completed
    while (countCompleted[0] < count) {
      sleepFor(1000);
    }
    jobWorker.close();
  }

  public static void failTask(
      ZeebeClient client, String jobType, String workerName, int incidentCount) {
    failTask(client, jobType, workerName, null, incidentCount);
  }

  public static void failTask(
      ZeebeClient client,
      String jobType,
      String workerName,
      String errorMessage,
      int incidentCount) {
    final int[] countFailed = {0};
    final JobWorker jobWorker =
        client
            .newWorker()
            .jobType(jobType)
            .handler(
                (jobClient, activatedJob) -> {
                  final String error =
                      errorMessage == null ? "Error " + random.nextInt(50) : errorMessage;
                  if (countFailed[0] < incidentCount) {
                    client
                        .newFailCommand(activatedJob.getKey())
                        .retries(0)
                        .errorMessage(error)
                        .send()
                        .join();
                    countFailed[0]++;
                    if (countFailed[0] % 1000 == 0) {
                      LOGGER.info("{} jobs failed ", countFailed[0]);
                    }
                  }
                })
            .name(workerName)
            .timeout(Duration.ofSeconds(2))
            .open();
    // wait till all incidents are created
    while (countFailed[0] < incidentCount) {
      sleepFor(200);
    }
    jobWorker.close();
  }
}
