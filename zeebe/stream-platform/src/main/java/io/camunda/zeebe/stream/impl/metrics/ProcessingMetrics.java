/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.stream.impl.metrics;

import io.camunda.zeebe.stream.impl.ProcessingStateMachine.ErrorHandlingPhase;
import io.prometheus.client.Counter;
import io.prometheus.client.Enumeration;
import io.prometheus.client.Histogram;
import io.prometheus.client.Histogram.Child;
import io.prometheus.client.Histogram.Timer;

public class ProcessingMetrics {

  private static final String NAMESPACE = "zeebe";
  private static final String LABEL_NAME_PARTITION = "partition";

  private static final Histogram BATCH_PROCESSING_DURATION =
      Histogram.build()
          .namespace(NAMESPACE)
          .name("stream_processor_batch_processing_duration")
          .help("Time spent in batch processing (in seconds)")
          .buckets(.0001, .001, .01, 0.1, .250, 0.5, 1, 2)
          .labelNames(LABEL_NAME_PARTITION)
          .register();
  private static final Histogram BATCH_PROCESSING_COMMANDS =
      Histogram.build()
          .namespace(NAMESPACE)
          .name("stream_processor_batch_processing_commands")
          .help("Records the distribution of commands in a batch over time")
          .buckets(1, 2, 4, 8, 16, 32, 64, 128)
          .labelNames(LABEL_NAME_PARTITION)
          .register();

  private static final Histogram BATCH_PROCESSING_POST_COMMIT_TASKS =
      Histogram.build()
          .namespace(NAMESPACE)
          .name("stream_processor_batch_processing_post_commit_tasks")
          .help("Time spent in executing post commit tasks after batch processing (in seconds)")
          .buckets(.0001, .001, .01, 0.1, .250, 0.5, 1, 2)
          .labelNames(LABEL_NAME_PARTITION)
          .register();

  private static final Counter BATCH_PROCESSING_RETRIES =
      Counter.build()
          .namespace(NAMESPACE)
          .name("stream_processor_batch_processing_retry")
          .help(
              "Number of times batch processing failed due to reaching batch limit and was retried")
          .labelNames(LABEL_NAME_PARTITION)
          .register();

  private static final Enumeration ERROR_HANDLING_PHASE =
      Enumeration.build()
          .namespace(NAMESPACE)
          .name("stream_processor_error_handling_phase")
          .help("The phase of error handling")
          .labelNames(LABEL_NAME_PARTITION)
          .states(ErrorHandlingPhase.class)
          .register();

  private final Child batchProcessingDuration;
  private final Child batchProcessingCommands;
  private final Counter.Child batchProcessingRetries;
  private final Child batchProcessingPostCommitTasks;
  private final Enumeration.Child errorHandlingPhase;

  public ProcessingMetrics(final String partitionIdLabel) {
    batchProcessingDuration = BATCH_PROCESSING_DURATION.labels(partitionIdLabel);
    batchProcessingCommands = BATCH_PROCESSING_COMMANDS.labels(partitionIdLabel);
    batchProcessingRetries = BATCH_PROCESSING_RETRIES.labels(partitionIdLabel);
    batchProcessingPostCommitTasks = BATCH_PROCESSING_POST_COMMIT_TASKS.labels(partitionIdLabel);
    errorHandlingPhase = ERROR_HANDLING_PHASE.labels(partitionIdLabel);
  }

  public Timer startBatchProcessingDurationTimer() {
    return batchProcessingDuration.startTimer();
  }

  public void observeCommandCount(final int commandCount) {
    batchProcessingCommands.observe(commandCount);
  }

  public void countRetry() {
    batchProcessingRetries.inc();
  }

  public Timer startBatchProcessingPostCommitTasksTimer() {
    return batchProcessingPostCommitTasks.startTimer();
  }

  public void errorHandlingPhase(final ErrorHandlingPhase phase) {
    errorHandlingPhase.state(phase);
  }
}
