/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.broker.system.partitions;

import io.camunda.zeebe.engine.processing.streamprocessor.TypedRecordProcessorContext;
import io.camunda.zeebe.engine.processing.streamprocessor.TypedRecordProcessors;

@FunctionalInterface
public interface TypedRecordProcessorsFactory {

  TypedRecordProcessors createTypedStreamProcessor(
      TypedRecordProcessorContext typedRecordProcessorContext);
}
