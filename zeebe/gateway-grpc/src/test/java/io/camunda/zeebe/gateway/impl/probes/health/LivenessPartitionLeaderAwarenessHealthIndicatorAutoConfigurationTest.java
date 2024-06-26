/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.impl.probes.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.zeebe.broker.client.api.BrokerClusterState;
import io.camunda.zeebe.gateway.impl.SpringGatewayBridge;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

public class LivenessPartitionLeaderAwarenessHealthIndicatorAutoConfigurationTest {

  private SpringGatewayBridge helperGatewayBridge;

  private PartitionLeaderAwarenessHealthIndicatorAutoConfiguration sutAutoConfig;

  @Before
  public void setUp() {
    helperGatewayBridge = new SpringGatewayBridge();
    sutAutoConfig = new PartitionLeaderAwarenessHealthIndicatorAutoConfiguration();
  }

  @Test
  public void shouldCreateHealthIndicatorEvenBeforeClusterStateSupplierIsRegistered() {
    // when
    final var actual =
        sutAutoConfig.gatewayPartitionLeaderAwarenessHealthIndicator(helperGatewayBridge);

    // then
    assertThat(actual).isNotNull();
  }

  @Test
  public void
      shouldCreateHealthIndicatorThatReportsHealthBasedOnResultOfRegisteredClusterStateSupplier() {
    // given
    final BrokerClusterState mockClusterState = mock(BrokerClusterState.class);
    when(mockClusterState.getPartitions()).thenReturn(List.of(1));
    when(mockClusterState.getLeaderForPartition(1)).thenReturn(42);

    final Supplier<Optional<BrokerClusterState>> stateSupplier =
        () -> Optional.of(mockClusterState);
    final var healthIndicator =
        sutAutoConfig.gatewayPartitionLeaderAwarenessHealthIndicator(helperGatewayBridge);

    // when
    helperGatewayBridge.registerClusterStateSupplier(stateSupplier);
    final Health actualHealth = healthIndicator.health();

    // then
    assertThat(actualHealth).isNotNull();
    assertThat(actualHealth.getStatus()).isSameAs(Status.UP);
  }
}
