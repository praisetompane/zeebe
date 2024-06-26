/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.msgpack;

import io.camunda.zeebe.msgpack.value.StringValue;

public final class MsgpackPropertyException extends MsgpackException {

  private static final String MESSAGE_FORMAT = "Property '%s' is invalid: %s";
  private final StringValue property;

  public MsgpackPropertyException(final StringValue property, final String message) {
    this(property, message, null);
  }

  public MsgpackPropertyException(final StringValue property, final Throwable cause) {
    this(property, cause.getMessage(), cause);
  }

  public MsgpackPropertyException(
      final StringValue property, final String message, final Throwable cause) {
    super(String.format(MESSAGE_FORMAT, property, message), cause);
    this.property = property;
  }

  public String getProperty() {
    return property.toString();
  }
}
