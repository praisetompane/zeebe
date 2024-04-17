/*
 * Copyright Camunda Services GmbH
 *
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING, OR DISTRIBUTING THE SOFTWARE (“USE”), YOU INDICATE YOUR ACCEPTANCE TO AND ARE ENTERING INTO A CONTRACT WITH, THE LICENSOR ON THE TERMS SET OUT IN THIS AGREEMENT. IF YOU DO NOT AGREE TO THESE TERMS, YOU MUST NOT USE THE SOFTWARE. IF YOU ARE RECEIVING THE SOFTWARE ON BEHALF OF A LEGAL ENTITY, YOU REPRESENT AND WARRANT THAT YOU HAVE THE ACTUAL AUTHORITY TO AGREE TO THE TERMS AND CONDITIONS OF THIS AGREEMENT ON BEHALF OF SUCH ENTITY.
 * “Licensee” means you, an individual, or the entity on whose behalf you receive the Software.
 *
 * Permission is hereby granted, free of charge, to the Licensee obtaining a copy of this Software and associated documentation files to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject in each case to the following conditions:
 * Condition 1: If the Licensee distributes the Software or any derivative works of the Software, the Licensee must attach this Agreement.
 * Condition 2: Without limiting other conditions in this Agreement, the grant of rights is solely for non-production use as defined below.
 * "Non-production use" means any use of the Software that is not directly related to creating products, services, or systems that generate revenue or other direct or indirect economic benefits.  Examples of permitted non-production use include personal use, educational use, research, and development. Examples of prohibited production use include, without limitation, use for commercial, for-profit, or publicly accessible systems or use for commercial or revenue-generating purposes.
 *
 * If the Licensee is in breach of the Conditions, this Agreement, including the rights granted under it, will automatically terminate with immediate effect.
 *
 * SUBJECT AS SET OUT BELOW, THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * NOTHING IN THIS AGREEMENT EXCLUDES OR RESTRICTS A PARTY’S LIABILITY FOR (A) DEATH OR PERSONAL INJURY CAUSED BY THAT PARTY’S NEGLIGENCE, (B) FRAUD, OR (C) ANY OTHER LIABILITY TO THE EXTENT THAT IT CANNOT BE LAWFULLY EXCLUDED OR RESTRICTED.
 */
package io.camunda.tasklist.zeebeimport;

import static io.camunda.tasklist.util.ConversionUtils.toStringOrNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.tasklist.entities.FormEntity;
import io.camunda.tasklist.entities.ProcessEntity;
import io.camunda.tasklist.exceptions.PersistenceException;
import io.camunda.tasklist.schema.indices.FormIndex;
import io.camunda.tasklist.schema.indices.ProcessIndex;
import io.camunda.tasklist.util.ConversionUtils;
import io.camunda.tasklist.util.ElasticsearchUtil;
import io.camunda.tasklist.zeebeimport.util.XMLUtil;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

// TODO This class was used on the recover of aliases (I'm keeping it here for one release just in
// case - This should be removed on 8.5 -- notice that is not enabled with @Component and is not
// being used anywhere)
public class CUSTOMCopyProcessesFromOptimize {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(CUSTOMCopyProcessesFromOptimize.class);
  private static final String OPTIMIZE_PROCESS_INDEX = "optimize-process-definition_v6";

  @Autowired private RestHighLevelClient esClient;

  @Autowired private XMLUtil xmlUtil;

  @Autowired private ProcessIndex processIndex;

  @Autowired private FormIndex formIndex;

  @Autowired private ObjectMapper objectMapper;

  public void copyProcesses() {
    String scrollId = null;
    try {
      final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().size(10);

      final SearchRequest searchRequest =
          new SearchRequest(OPTIMIZE_PROCESS_INDEX)
              .source(searchSourceBuilder)
              .requestCache(false)
              .scroll(TimeValue.timeValueMinutes(1));

      var searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);

      while (searchResponse.getHits().getHits().length > 0) {
        final BulkRequest bulkRequest = new BulkRequest();

        for (SearchHit hit : searchResponse.getHits().getHits()) {

          final Map<String, Object> processFromOptimize = hit.getSourceAsMap();

          if (!processFromOptimize.get("key").equals("customer_onboarding_en")) {
            final long processDefinitionKey = Long.valueOf((String) processFromOptimize.get("id"));

            if (!processDefinitionExists(processDefinitionKey, esClient)) {
              final Map<String, String> userTaskForms = new HashMap<>();
              final ProcessEntity processToTasklist =
                  createEntity(processFromOptimize, userTaskForms::put);
              try {
                bulkRequest.add(
                    new IndexRequest()
                        .index(processIndex.getFullQualifiedName())
                        .id(toStringOrNull(processToTasklist.getKey()))
                        .source(
                            objectMapper.writeValueAsString(processToTasklist), XContentType.JSON));

                userTaskForms.forEach(
                    (formKey, schema) -> {
                      try {
                        final FormEntity formEntity =
                            new FormEntity(
                                String.valueOf(processDefinitionKey),
                                formKey,
                                schema,
                                processToTasklist.getTenantId());
                        if (!formExists(formEntity.getId(), esClient)) {
                          persistForm(formEntity, bulkRequest);
                        }
                      } catch (Exception ex) {
                        LOGGER.warn("Unable to copy forms from Optimize: " + ex.getMessage(), ex);
                      }
                    });
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e.getMessage(), e);
              }
            }
          }
        }

        if (bulkRequest.requests().size() > 0) {
          esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        }

        scrollId = searchResponse.getScrollId();
        final SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll(TimeValue.timeValueMinutes(1));

        searchResponse = esClient.scroll(scrollRequest, RequestOptions.DEFAULT);

        LOGGER.info("Processes were successfully copied from Optimize.");
      }
    } catch (Exception ex) {
      LOGGER.warn("Unable to copy processes from Optimize: " + ex.getMessage(), ex);
    } finally {
      if (scrollId != null) {
        ElasticsearchUtil.clearScroll(scrollId, esClient);
      }
    }
  }

  private ProcessEntity createEntity(
      Map<String, Object> processFromOptimize, BiConsumer<String, String> userTaskFormCollector) {
    final ProcessEntity processEntity = new ProcessEntity();
    processEntity.setId(String.valueOf(processFromOptimize.get("id")));
    processEntity.setTenantId("<default>");
    processEntity.setKey(Long.valueOf(String.valueOf(processFromOptimize.get("id"))));
    processEntity.setBpmnProcessId(String.valueOf(processFromOptimize.get("key")));
    processEntity.setVersion(Integer.valueOf(String.valueOf(processFromOptimize.get("version"))));
    processEntity.setName(String.valueOf(processFromOptimize.get("name")));
    final String bpmXml = String.valueOf(processFromOptimize.get("bpmn20Xml"));

    xmlUtil.extractDiagramData(
        bpmXml.getBytes(),
        processEntity.getBpmnProcessId()::equals,
        processEntity::setName,
        flowNode -> processEntity.getFlowNodes().add(flowNode),
        userTaskFormCollector,
        processEntity::setFormKey,
        formId -> processEntity.setFormId(formId),
        processEntity::setStartedByForm);

    return processEntity;
  }

  private boolean processDefinitionExists(
      final long processDefinitionKey, final RestHighLevelClient elasticsearchClient)
      throws IOException {
    return elasticsearchClient.exists(
        new GetRequest(processIndex.getFullQualifiedName(), String.valueOf(processDefinitionKey)),
        RequestOptions.DEFAULT);
  }

  private boolean formExists(final String formId, final RestHighLevelClient elasticsearchClient)
      throws IOException {
    return elasticsearchClient.exists(
        new GetRequest(formIndex.getFullQualifiedName(), formId), RequestOptions.DEFAULT);
  }

  private void persistForm(FormEntity formEntity, BulkRequest bulkRequest)
      throws PersistenceException {

    LOGGER.debug("Form: key {}", formEntity.getId());
    try {
      bulkRequest.add(
          new IndexRequest()
              .index(formIndex.getFullQualifiedName())
              .id(ConversionUtils.toStringOrNull(formEntity.getId()))
              .source(objectMapper.writeValueAsString(formEntity), XContentType.JSON));

    } catch (JsonProcessingException e) {
      throw new PersistenceException(
          String.format("Error preparing the query to insert task form [%s]", formEntity.getId()),
          e);
    }
  }
}