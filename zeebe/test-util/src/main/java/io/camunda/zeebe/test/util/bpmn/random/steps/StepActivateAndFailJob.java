/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.test.util.bpmn.random.steps;

import java.time.Duration;
import java.util.Map;

public final class StepActivateAndFailJob extends AbstractExecutionStep {

  private final String jobType;
  private final boolean updateRetries;
  private final String elementId;

  public StepActivateAndFailJob(
      final String jobType, final boolean updateRetries, final String elementId) {
    this.jobType = jobType;
    this.updateRetries = updateRetries;
    this.elementId = elementId;
  }

  public String getJobType() {
    return jobType;
  }

  public boolean isUpdateRetries() {
    return updateRetries;
  }

  public String getElementId() {
    return elementId;
  }

  @Override
  protected Map<String, Object> updateVariables(
      final Map<String, Object> variables, final Duration activationDuration) {
    return variables;
  }

  @Override
  public boolean isAutomatic() {
    return false;
  }

  @Override
  public Duration getDeltaTime() {
    return VIRTUALLY_NO_TIME;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + jobType.hashCode();
    result = 31 * result + (updateRetries ? 1 : 0);
    return result;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    final StepActivateAndFailJob that = (StepActivateAndFailJob) o;

    if (updateRetries != that.updateRetries) {
      return false;
    }
    return jobType.equals(that.jobType);
  }
}
