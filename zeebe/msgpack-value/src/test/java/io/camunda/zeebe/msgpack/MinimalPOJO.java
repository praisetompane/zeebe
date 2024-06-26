/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.msgpack;

import io.camunda.zeebe.msgpack.property.LongProperty;

public final class MinimalPOJO extends UnpackedObject {

  private final LongProperty longProp = new LongProperty("longProp");

  public MinimalPOJO() {
    super(1);
    declareProperty(longProp);
  }

  public long getLongProp() {
    return longProp.getValue();
  }

  public void setLongProp(final long value) {
    longProp.setValue(value);
  }
}
