/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Loggers {

  public static final Logger CONFIG_LOGGER =
      LoggerFactory.getLogger("io.camunda.zeebe.util.config");
  public static final Logger ACTOR_LOGGER = LoggerFactory.getLogger("io.camunda.zeebe.util.actor");
  public static final Logger IO_LOGGER = LoggerFactory.getLogger("io.camunda.zeebe.util.buffer");
  public static final Logger FILE_LOGGER = LoggerFactory.getLogger("io.camunda.zeebe.util.fs");
}
