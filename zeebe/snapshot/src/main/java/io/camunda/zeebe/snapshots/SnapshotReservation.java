/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.snapshots;

import io.camunda.zeebe.scheduler.future.ActorFuture;

/** Returned when a snapshot is reserved * */
public interface SnapshotReservation {

  /**
   * Releases the reservation of a snapshot.
   *
   * @return
   */
  ActorFuture<Void> release();
}
