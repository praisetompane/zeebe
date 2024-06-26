/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */

import {deployProcess, createInstances} from '../setup-utils';

const setup = async () => {
  const deploymentV1 = await deployProcess([
    'ProcessInstanceMigration/orderProcessMigration_v_1.bpmn',
  ]);
  if (deploymentV1.processes[0] === undefined) {
    throw new Error('Error deploying process');
  }

  const deploymentV2 = await deployProcess([
    'ProcessInstanceMigration/orderProcessMigration_v_2.bpmn',
  ]);
  if (deploymentV2.processes[0] === undefined) {
    throw new Error('Error deploying process');
  }

  const deploymentV3 = await deployProcess([
    'ProcessInstanceMigration/orderProcessMigration_v_3.bpmn',
  ]);
  if (deploymentV3.processes[0] === undefined) {
    throw new Error('Error deploying process');
  }

  return {
    processV1Instances: await createInstances(
      'orderProcessMigration',
      deploymentV1.processes[0].version,
      10,
    ),
    processV1: deploymentV1.processes[0],
    processV2: deploymentV2.processes[0],
    processV3: deploymentV3.processes[0],
  };
};

export {setup};
