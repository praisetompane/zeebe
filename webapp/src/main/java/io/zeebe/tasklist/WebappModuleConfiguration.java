/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.zeebe.tasklist;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@Configuration
@ComponentScan(
    basePackages = "io.zeebe.tasklist.webapp",
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@ConditionalOnProperty(
    name = "zeebe.tasklist.webappEnabled",
    havingValue = "true",
    matchIfMissing = true)
public class WebappModuleConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebappModuleConfiguration.class);

  @PostConstruct
  public void logModule() {
    LOGGER.info("Starting module: webapp");
  }
}
