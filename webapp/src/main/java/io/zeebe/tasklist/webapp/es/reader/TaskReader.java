package io.zeebe.tasklist.webapp.es.reader;

import java.io.IOException;
import java.util.List;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.tasklist.entities.TaskEntity;
import io.zeebe.tasklist.es.schema.templates.TaskTemplate;
import io.zeebe.tasklist.exceptions.TasklistRuntimeException;
import io.zeebe.tasklist.util.ElasticsearchUtil;
import io.zeebe.tasklist.webapp.graphql.entity.TaskDTO;
import io.zeebe.tasklist.webapp.graphql.entity.TaskQueryDTO;
import static io.zeebe.tasklist.util.ElasticsearchUtil.fromSearchHit;
import static io.zeebe.tasklist.util.ElasticsearchUtil.joinWithAnd;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.constantScoreQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

@Component
public class TaskReader {

  private static final Logger logger = LoggerFactory.getLogger(TaskReader.class);

  @Autowired
  private RestHighLevelClient esClient;

  @Autowired
  private TaskTemplate taskTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  public TaskDTO getTask(String taskKey, List<String> fieldNames) {

    //TODO specity sourceFields to fetch
    final GetRequest getRequest = new GetRequest(taskTemplate.getAlias())
        .id(taskKey);

    try {
      final GetResponse response = esClient.get(getRequest, RequestOptions.DEFAULT);
      final TaskEntity taskEntity = fromSearchHit(response.getSourceAsString(), objectMapper, TaskEntity.class);
      return TaskDTO.createFrom(taskEntity);
    } catch (IOException e) {
      final String message = String.format("Exception occurred, while obtaining task: %s", e.getMessage());
      logger.error(message, e);
      throw new TasklistRuntimeException(message, e);
    }
  }

  public List<TaskDTO> getTasks(TaskQueryDTO query, List<String> fieldNames) {

    QueryBuilder esQuery = buildQuery(query);

    //TODO #104 define list of fields

    //TODO we can play around with query type here (2nd parameter), e.g. when we select for only active tasks
    final SearchRequest searchRequest = ElasticsearchUtil.createSearchRequest(taskTemplate)
        .source(
            new SearchSourceBuilder().query(esQuery).sort(TaskTemplate.CREATION_TIME, SortOrder.DESC)
//            .fetchSource(fieldNames.toArray(String[]::new), null)
        );

    try {
      final List<TaskEntity> taskEntities = ElasticsearchUtil.scroll(searchRequest, TaskEntity.class, objectMapper, esClient);
      return TaskDTO.createFrom(taskEntities);
    } catch (IOException e) {
      final String message = String.format("Exception occurred, while obtaining tasks: %s", e.getMessage());
      logger.error(message, e);
      throw new TasklistRuntimeException(message, e);
    }
  }

  private QueryBuilder buildQuery(TaskQueryDTO query) {
    QueryBuilder stateQ = null;
    if (query.getState() != null) {
      stateQ = termQuery(TaskTemplate.STATE, query.getState());
    }
    QueryBuilder assignedQ = null;
    QueryBuilder assigneeQ = null;
    if (query.getAssigned() != null) {
      if (query.getAssigned()) {
        assignedQ = existsQuery(TaskTemplate.ASSIGNEE);
        if (query.getAssignee() != null) {
          assigneeQ = termQuery(TaskTemplate.ASSIGNEE, query.getAssignee());
        }
      } else {
        assignedQ = boolQuery().mustNot(existsQuery(TaskTemplate.ASSIGNEE));
      }
    }
    QueryBuilder jointQ = joinWithAnd(stateQ, assignedQ, assigneeQ);
    if (jointQ == null) {
      jointQ = matchAllQuery();
    }
    return constantScoreQuery(jointQ);
  }

}
