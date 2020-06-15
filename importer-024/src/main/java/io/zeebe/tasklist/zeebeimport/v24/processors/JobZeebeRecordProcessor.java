/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.zeebe.tasklist.zeebeimport.v24.processors;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.protocol.record.Record;
import io.zeebe.tasklist.entities.TaskEntity;
import io.zeebe.tasklist.entities.TaskState;
import io.zeebe.tasklist.es.schema.templates.TaskTemplate;
import io.zeebe.tasklist.exceptions.PersistenceException;
import io.zeebe.tasklist.property.TasklistProperties;
import io.zeebe.tasklist.util.DateUtil;
import io.zeebe.tasklist.util.ElasticsearchUtil;
import io.zeebe.tasklist.zeebeimport.v24.record.value.JobRecordValueImpl;
import static io.zeebe.tasklist.util.ElasticsearchUtil.UPDATE_RETRY_COUNT;
import static io.zeebe.tasklist.zeebeimport.v24.record.Intent.COMPLETED;
import static io.zeebe.tasklist.zeebeimport.v24.record.Intent.CREATED;

@Component
public class JobZeebeRecordProcessor {

  private static final Logger logger = LoggerFactory.getLogger(JobZeebeRecordProcessor.class);

  private static final Set<String> TASK_START_STATES = new HashSet<>();
  private static final Set<String> TASK_FINISH_STATES = new HashSet<>();

  static {
    TASK_START_STATES.add(CREATED.name());
    TASK_FINISH_STATES.add(COMPLETED.name());
  }

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private TaskTemplate taskTemplate;

  @Autowired
  private TasklistProperties tasklistProperties;

  public void processJobRecord(Record record, BulkRequest bulkRequest) throws PersistenceException {
    JobRecordValueImpl recordValue = (JobRecordValueImpl)record.getValue();
    if (recordValue.getType().equals(tasklistProperties.getImporter().getJobType())) {
      //update variable
      bulkRequest.add(persistTask(record, recordValue));
    }
    // else skip task
  }

  private UpdateRequest persistTask(Record record, JobRecordValueImpl recordValue) throws PersistenceException {
    TaskEntity entity = new TaskEntity();
    entity.setId(String.valueOf(record.getKey()));
    entity.setKey(record.getKey());
    entity.setPartitionId(record.getPartitionId());
    entity.setElementId(recordValue.getElementId());
    entity.setWorkflowInstanceKey(String.valueOf(recordValue.getWorkflowInstanceKey()));
    entity.setBpmnProcessId(recordValue.getBpmnProcessId());
    if (TASK_FINISH_STATES.contains(record.getIntent().name())) {
      entity.setState(TaskState.COMPLETED);
      entity.setCompletionTime(DateUtil.toOffsetDateTime(Instant.ofEpochMilli(record.getTimestamp())));
    } else {
      entity.setState(TaskState.CREATED);
      entity.setCreationTime(DateUtil.toOffsetDateTime(Instant.ofEpochMilli(record.getTimestamp())));
    }
    return getTaskQuery(entity);
  }

  private UpdateRequest getTaskQuery(TaskEntity entity) throws PersistenceException {
    try {
      logger.debug("Task instance: id {}", entity.getId());
      Map<String, Object> updateFields = new HashMap<>();
      updateFields.put(TaskTemplate.STATE, entity.getState());
      updateFields.put(TaskTemplate.COMPLETION_TIME, entity.getCompletionTime());

      //format date fields properly
      Map<String, Object> jsonMap = objectMapper.readValue(objectMapper.writeValueAsString(updateFields), HashMap.class);

      return new UpdateRequest(taskTemplate.getMainIndexName(), ElasticsearchUtil.ES_INDEX_TYPE, entity.getId())
          .upsert(objectMapper.writeValueAsString(entity), XContentType.JSON)
          .doc(jsonMap)
          .retryOnConflict(UPDATE_RETRY_COUNT);

    } catch (IOException e) {
      logger.error(String.format("Error preparing the query to upsert task instance [%s]", entity.getId()), e);
      throw new PersistenceException(String.format("Error preparing the query to upsert task instance [%s]", entity.getId()), e);
    }
  }
}
