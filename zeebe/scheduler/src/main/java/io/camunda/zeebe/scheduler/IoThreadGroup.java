/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.scheduler;

import io.camunda.zeebe.scheduler.ActorScheduler.ActorSchedulerBuilder;

public final class IoThreadGroup extends ActorThreadGroup {

  public IoThreadGroup(final ActorSchedulerBuilder builder) {
    super(
        "zb-fs-workers", builder.getIoBoundActorThreadCount(), builder, builder.getSchedulerName());
  }
}
