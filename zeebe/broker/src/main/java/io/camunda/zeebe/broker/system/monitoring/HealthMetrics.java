/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.broker.system.monitoring;

import io.prometheus.client.Gauge;

public class HealthMetrics {

  private static final Gauge HEALTH =
      Gauge.build()
          .namespace("zeebe")
          .name("health")
          .help("Shows current health of the partition (1 = healthy, 0 = unhealthy, -1 = dead)")
          .labelNames("partition")
          .register();
  private final String partitionIdLabel;

  public HealthMetrics(final int partitionId) {
    partitionIdLabel = String.valueOf(partitionId);
  }

  public void setHealthy() {
    HEALTH.labels(partitionIdLabel).set(1);
  }

  public void setUnhealthy() {
    HEALTH.labels(partitionIdLabel).set(0);
  }

  public void setDead() {
    HEALTH.labels(partitionIdLabel).set(-1);
  }
}
