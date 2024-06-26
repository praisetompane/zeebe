/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.variable.mapping;

import static io.camunda.zeebe.engine.processing.variable.mapping.VariableValue.variable;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.builder.IntermediateCatchEventBuilder;
import io.camunda.zeebe.model.bpmn.builder.ZeebeVariablesMappingBuilder;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.intent.VariableIntent;
import io.camunda.zeebe.test.util.MsgPackUtil;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public final class MessageOutputMappingTest {

  @ClassRule public static final EngineRule ENGINE_RULE = EngineRule.singlePartition();
  private static final String PROCESS_ID = "process";
  private static final String MESSAGE_NAME = "message";
  private static final String CORRELATION_VARIABLE = "key";

  @Rule
  public final RecordingExporterTestWatcher recordingExporterTestWatcher =
      new RecordingExporterTestWatcher();

  @Parameter(0)
  public String messageVariables;

  @Parameter(1)
  public Consumer<IntermediateCatchEventBuilder> mappings;

  @Parameter(2)
  public List<VariableValue> expectedActivityVariables;

  @Parameter(3)
  public List<VariableValue> expectedScopeVariables;

  private String correlationKey;

  @Parameters(name = "from {0} to activity: {2} and scope: {3}")
  public static Object[][] parameters() {
    return new Object[][] {
      // create variable
      {"{'x': 1}", mapping(b -> {}), activityVariables(), scopeVariables(variable("x", "1"))},
      {
        "{'x': 1}",
        mapping(b -> b.zeebeOutputExpression("x", "x")),
        activityVariables(variable("x", "1")),
        scopeVariables(variable("x", "1"))
      },
      {
        "{'x': 1}",
        mapping(b -> b.zeebeOutputExpression("x", "y")),
        activityVariables(variable("x", "1")),
        scopeVariables(variable("y", "1"))
      },
      {
        "{'_x': 1}",
        mapping(b -> b.zeebeOutputExpression("_x", "_y")),
        activityVariables(variable("_x", "1")),
        scopeVariables(variable("_y", "1"))
      },
      {
        "{'x': 1, 'y': 2}",
        mapping(b -> b.zeebeOutputExpression("y", "z")),
        activityVariables(variable("x", "1"), variable("y", "2")),
        scopeVariables(variable("z", "2"))
      },
      {
        "{'x': {'y': 2}}",
        mapping(b -> {}),
        activityVariables(),
        scopeVariables(variable("x", "{\"y\":2}"))
      },
      {
        "{'x': {'y': 2}}",
        mapping(b -> b.zeebeOutputExpression("x", "y")),
        activityVariables(variable("x", "{\"y\":2}")),
        scopeVariables(variable("y", "{\"y\":2}"))
      },
      {
        "{'x': {'y': 2}}",
        mapping(b -> b.zeebeOutputExpression("x.y", "y")),
        activityVariables(variable("x", "{\"y\":2}")),
        scopeVariables(variable("y", "2"))
      },
      {
        "{'x': 1, 'y': 2}",
        mapping(b -> b.zeebeOutputExpression("x", "z.x").zeebeOutputExpression("y", "z.y")),
        activityVariables(variable("x", "1"), variable("y", "2")),
        scopeVariables(variable("z", "{\"x\":1,\"y\":2}"))
      },
      // update variable
      {"{'i': 1}", mapping(b -> {}), activityVariables(), scopeVariables(variable("i", "1"))},
      {
        "{'x': 1}",
        mapping(b -> b.zeebeOutputExpression("x", "i")),
        activityVariables(variable("x", "1")),
        scopeVariables(variable("i", "1"))
      },
      {
        "{'i': 2, 'x': 1}",
        mapping(b -> b.zeebeInputExpression("i", "x")),
        activityVariables(variable("x", "0")),
        scopeVariables(variable("i", "2"))
      },
    };
  }

  @Before
  public void init() {
    correlationKey = UUID.randomUUID().toString();
  }

  @Test
  public void shouldApplyOutputMappings() {
    // given
    ENGINE_RULE
        .deployment()
        .withXmlResource(
            Bpmn.createExecutableProcess(PROCESS_ID)
                .startEvent()
                .intermediateCatchEvent(
                    "catch-event",
                    b -> {
                      b.message(
                          m ->
                              m.name(MESSAGE_NAME)
                                  .zeebeCorrelationKeyExpression(CORRELATION_VARIABLE));

                      mappings.accept(b);
                    })
                .endEvent()
                .done())
        .deploy()
        .getValue()
        .getProcessesMetadata()
        .get(0)
        .getProcessDefinitionKey();

    final Map<String, Object> variables = new HashMap<>();
    variables.put("i", 0);
    variables.put(CORRELATION_VARIABLE, correlationKey);

    // when
    final long processInstanceKey =
        ENGINE_RULE
            .processInstance()
            .ofBpmnProcessId(PROCESS_ID)
            .withVariables("{'i':0,'key':'" + correlationKey + "'}")
            .create();
    ENGINE_RULE
        .message()
        .withName(MESSAGE_NAME)
        .withCorrelationKey(correlationKey)
        .withVariables(MsgPackUtil.asMsgPack(messageVariables))
        .publish();

    // then
    final long elementInstanceKey =
        RecordingExporter.processInstanceRecords(ProcessInstanceIntent.ELEMENT_COMPLETED)
            .withProcessInstanceKey(processInstanceKey)
            .withElementId("catch-event")
            .getFirst()
            .getKey();

    final long latestPayloadVariablePosition =
        RecordingExporter.variableRecords(VariableIntent.CREATED)
            .withProcessInstanceKey(processInstanceKey)
            .filter(r -> variables.containsKey(r.getValue().getName()))
            .limit(variables.size())
            .map(Record::getPosition)
            .max(Comparator.naturalOrder())
            .orElseThrow();

    assertThat(
            RecordingExporter.variableRecords()
                .withProcessInstanceKey(processInstanceKey)
                .skipUntil(r -> r.getPosition() > latestPayloadVariablePosition)
                .withScopeKey(elementInstanceKey)
                .limit(expectedActivityVariables.size()))
        .extracting(Record::getValue)
        .extracting(v -> variable(v.getName(), v.getValue()))
        .hasSameSizeAs(expectedActivityVariables)
        .containsAll(expectedActivityVariables);

    assertThat(
            RecordingExporter.variableRecords()
                .withProcessInstanceKey(processInstanceKey)
                .skipUntil(r -> r.getPosition() > latestPayloadVariablePosition)
                .withScopeKey(processInstanceKey)
                .limit(expectedScopeVariables.size()))
        .extracting(Record::getValue)
        .extracting(v -> variable(v.getName(), v.getValue()))
        .hasSameSizeAs(expectedScopeVariables)
        .containsAll(expectedScopeVariables);
  }

  private static Consumer<ZeebeVariablesMappingBuilder<IntermediateCatchEventBuilder>> mapping(
      final Consumer<ZeebeVariablesMappingBuilder<IntermediateCatchEventBuilder>> mappingBuilder) {
    return mappingBuilder;
  }

  private static List<VariableValue> activityVariables(final VariableValue... variables) {
    return Arrays.asList(variables);
  }

  private static List<VariableValue> scopeVariables(final VariableValue... variables) {
    return Arrays.asList(variables);
  }
}
