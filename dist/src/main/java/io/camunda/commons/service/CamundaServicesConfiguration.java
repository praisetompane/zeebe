/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.commons.service;

import io.camunda.search.clients.CamundaSearchClient;
import io.camunda.service.CamundaServices;
import io.camunda.service.ProcessInstanceServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "zeebe.broker.gateway",
    name = "enable",
    havingValue = "true",
    matchIfMissing = true)
public class CamundaServicesConfiguration {

  private final CamundaSearchClient camundaSearchClient;

  @Autowired
  public CamundaServicesConfiguration(final CamundaSearchClient camundaSearchClient) {
    this.camundaSearchClient = camundaSearchClient;
  }

  @Bean
  public CamundaServices camundaServices() {
    return new CamundaServices(camundaSearchClient);
  }

  @Bean
  public ProcessInstanceServices processInstanceServices(final CamundaServices camundaServices) {
    return camundaServices.processInstanceServices();
  }
}
