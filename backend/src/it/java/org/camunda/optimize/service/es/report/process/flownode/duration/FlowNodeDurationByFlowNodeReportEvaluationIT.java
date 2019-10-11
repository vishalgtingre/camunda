/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.process.flownode.duration;

import com.google.common.collect.ImmutableList;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.optimize.dto.engine.ProcessDefinitionEngineDto;
import org.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType;
import org.camunda.optimize.dto.optimize.query.report.single.configuration.FlowNodeExecutionState;
import org.camunda.optimize.dto.optimize.query.report.single.configuration.sorting.SortOrder;
import org.camunda.optimize.dto.optimize.query.report.single.configuration.sorting.SortingDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.ProcessFilterDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.util.ProcessFilterBuilder;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.ReportMapResult;
import org.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewEntity;
import org.camunda.optimize.dto.optimize.query.report.single.process.view.ProcessViewProperty;
import org.camunda.optimize.dto.optimize.query.report.single.result.MapResultEntryDto;
import org.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResultDto;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.service.es.report.process.AbstractProcessDefinitionIT;
import org.camunda.optimize.service.security.util.LocalDateUtil;
import org.camunda.optimize.test.util.ProcessReportDataBuilder;
import org.camunda.optimize.test.util.ProcessReportDataType;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static org.camunda.optimize.dto.optimize.ReportConstants.ALL_VERSIONS;
import static org.camunda.optimize.dto.optimize.query.report.single.configuration.sorting.SortingDto.SORT_BY_KEY;
import static org.camunda.optimize.dto.optimize.query.report.single.configuration.sorting.SortingDto.SORT_BY_VALUE;
import static org.camunda.optimize.test.util.DurationAggregationUtil.calculateExpectedValueGivenDurations;
import static org.camunda.optimize.test.util.DurationAggregationUtil.calculateExpectedValueGivenDurationsDefaultAggr;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

public class FlowNodeDurationByFlowNodeReportEvaluationIT extends AbstractProcessDefinitionIT {

  private static final String PROCESS_DEFINITION_KEY = "123";
  private static final String START_EVENT = "startEvent";
  private static final String END_EVENT = "endEvent";
  private static final String SERVICE_TASK_ID = "aSimpleServiceTask";
  private static final String SERVICE_TASK_ID_2 = "aSimpleServiceTask2";
  private static final String USER_TASK = "userTask";

  private final List<AggregationType> aggregationTypes = Arrays.asList(AggregationType.values());

  @Test
  public void reportEvaluationForOneProcess() throws Exception {

    // given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 20L);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    // then
    final ReportMapResult result = evaluationResponse.getResult();
    final ProcessReportDataDto resultReportDataDto = evaluationResponse.getReportDefinition().getData();
    assertThat(result.getInstanceCount(), is(1L));
    assertThat(resultReportDataDto.getProcessDefinitionKey(), is(processDefinition.getKey()));
    assertThat(resultReportDataDto.getDefinitionVersions(), contains(processDefinition.getVersionAsString()));
    assertThat(resultReportDataDto.getView(), is(notNullValue()));
    assertThat(resultReportDataDto.getView().getEntity(), is(ProcessViewEntity.FLOW_NODE));
    assertThat(resultReportDataDto.getView().getProperty(), is(ProcessViewProperty.DURATION));
    assertThat(result.getData(), is(notNullValue()));
    assertThat(result.getData().size(), is(3));
    assertThat(result.getDataEntryForKey(SERVICE_TASK_ID).isPresent(), is(true));
    assertThat(
      result.getDataEntryForKey(SERVICE_TASK_ID).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(20L))
    );
    assertThat(result.getDataEntryForKey(START_EVENT).isPresent(), is(true));
    assertThat(
      result.getDataEntryForKey(START_EVENT).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(20L))
    );
    assertThat(result.getDataEntryForKey(END_EVENT).isPresent(), is(true));
    assertThat(
      result.getDataEntryForKey(END_EVENT).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(20L))
    );
  }


  @Test
  public void reportEvaluationForSeveralProcesses() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 10L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 30L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 20L);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    // then
    final ReportMapResult result = evaluationResponse.getResult();
    assertThat(result.getData().size(), is(3));
    assertThat(result.getDataEntryForKey(SERVICE_TASK_ID).isPresent(), is(true));
    assertThat(
      result.getDataEntryForKey(SERVICE_TASK_ID).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(10L, 30L, 20L))
    );
  }

  @Test
  public void evaluateReportForMultipleEvents() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deployProcessWithTwoTasks();

    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 100L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 20L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 200L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 10L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 900L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 90L);

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData =
      getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    // then
    final ReportMapResult result = evaluationResponse.getResult();
    assertThat(result.getIsComplete(), is(true));
    assertThat(result.getData().size(), is(4));
    assertThat(result.getDataEntryForKey(SERVICE_TASK_ID).isPresent(), is(true));
    assertThat(
      result.getDataEntryForKey(SERVICE_TASK_ID).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(100L, 200L, 900L))
    );
    assertThat(
      result.getDataEntryForKey(SERVICE_TASK_ID_2).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(20L, 10L, 90L))
    );
  }

  @Test
  public void evaluateReportForMultipleEvents_resultLimitedByConfig() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deployProcessWithTwoTasks();

    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 100L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 20L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 200L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 10L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 900L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 90L);

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    embeddedOptimizeRule.getConfigurationService().setEsAggregationBucketLimit(1);

    // when
    ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    // then
    final ReportMapResult resultDto = evaluationResponse.getResult();
    assertThat(resultDto.getInstanceCount(), is(3L));
    assertThat(resultDto.getData(), is(notNullValue()));
    assertThat(resultDto.getData().size(), is(4));
    assertThat(getExecutedFlowNodeCount(resultDto), is(1L));
    assertThat(resultDto.getIsComplete(), is(false));
  }

  @Test
  public void testCustomOrderOnResultKeyIsApplied() throws SQLException {
    // given
    final ProcessDefinitionEngineDto processDefinition = deployProcessWithTwoTasks();

    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 100L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 20L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 200L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 10L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 900L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 90L);


    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    final ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    reportData.getConfiguration().setSorting(new SortingDto(SORT_BY_KEY, SortOrder.ASC));
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    // then
    List<MapResultEntryDto<Long>> resultData = evaluationResponse.getResult().getData();
    assertThat(resultData.size(), is(4));
    final List<String> resultKeys = resultData.stream().map(MapResultEntryDto::getKey).collect(Collectors.toList());
    assertThat(
      resultKeys,
      // expect ascending order
      contains(resultKeys.stream().sorted(Comparator.naturalOrder()).toArray())
    );
  }


  @Test
  public void testEvaluationResultForAllAggregationTypes() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 10L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 30L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 20L);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    final Map<AggregationType, ReportMapResult> results = evaluateMapReportForAllAggTypes(
      reportData);

    // then
    assertDurationMapReportResults(results, new Long[]{10L, 30L, 20L});
  }

  private void assertDurationMapReportResults(final Map<AggregationType, ReportMapResult> results,
                                              Long[] expectedDurations) {
    aggregationTypes.forEach((AggregationType aggType) -> {
      final ReportMapResult result = results.get(aggType);
      assertThat(result.getDataEntryForKey(SERVICE_TASK_ID).isPresent(), is(true));
      assertThat(
        result.getDataEntryForKey(SERVICE_TASK_ID).get().getValue(),
        is(calculateExpectedValueGivenDurations(expectedDurations).get(aggType))
      );
    });
  }

  @Test
  public void testCustomOrderOnResultValueIsApplied() throws SQLException {
    // given
    ProcessDefinitionEngineDto processDefinition = deployProcessWithTwoTasks();

    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 100L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 20L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 200L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 10L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID, 900L);
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), SERVICE_TASK_ID_2, 90L);

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    aggregationTypes.forEach((AggregationType aggType) -> {
      // when
      final ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
      reportData.getConfiguration().setAggregationType(aggType);
      reportData.getConfiguration().setSorting(new SortingDto(SORT_BY_VALUE, SortOrder.ASC));
      AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse =
        evaluateMapReport(
          reportData);

      // then
      final List<MapResultEntryDto<Long>> resultData = evaluationResponse.getResult().getData();
      assertThat(resultData.size(), is(4));
      final List<Long> bucketValues = resultData.stream()
        .map(MapResultEntryDto::getValue)
        .collect(Collectors.toList());
      assertThat(
        bucketValues,
        contains(bucketValues.stream().sorted(Comparator.naturalOrder()).toArray())
      );
    });
  }

  @Test
  public void allVersionsRespectLatestNodesOnlyWhereLatestHasMoreNodes() throws Exception {
    //given
    ProcessDefinitionEngineDto firstDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessDefinitionEngineDto latestDefinition = deployProcessWithTwoTasks();
    assertThat(latestDefinition.getVersion(), is(2));

    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(firstDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 20L);
    processInstanceDto = engineRule.startProcessInstance(firstDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 30L);
    processInstanceDto = engineRule.startProcessInstance(latestDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 50L);
    processInstanceDto = engineRule.startProcessInstance(latestDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 120L);
    processInstanceDto = engineRule.startProcessInstance(latestDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 100L);

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    //when
    ProcessReportDataDto reportData = createReport(
      latestDefinition.getKey(),
      ALL_VERSIONS
    );
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    //then
    final ReportMapResult result = evaluationResponse.getResult();
    assertThat(result.getData().size(), is(4));
    assertThat(
      result.getDataEntryForKey(SERVICE_TASK_ID).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(20L, 30L, 50L, 120L, 100L))
    );
    assertThat(
      result.getDataEntryForKey(SERVICE_TASK_ID_2).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(50L, 120L, 100L))
    );
  }

  @Test
  public void multipleVersionsRespectLatestNodesOnlyWhereLatestHasMoreNodes() throws Exception {
    //given
    ProcessDefinitionEngineDto firstDefinition = deploySimpleServiceTaskProcessDefinition();
    deploySimpleServiceTaskProcessDefinition();
    ProcessDefinitionEngineDto latestDefinition = deployProcessWithTwoTasks();
    assertThat(latestDefinition.getVersion(), is(3));

    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(firstDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 20L);
    processInstanceDto = engineRule.startProcessInstance(firstDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 30L);
    processInstanceDto = engineRule.startProcessInstance(latestDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 50L);
    processInstanceDto = engineRule.startProcessInstance(latestDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 120L);
    processInstanceDto = engineRule.startProcessInstance(latestDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 100L);

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    //when
    ProcessReportDataDto reportData = createReport(
      latestDefinition.getKey(),
      ImmutableList.of(firstDefinition.getVersionAsString(), latestDefinition.getVersionAsString())
    );
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    //then
    final ReportMapResult result = evaluationResponse.getResult();
    assertThat(result.getData().size(), is(4));
    assertThat(
      result.getDataEntryForKey(SERVICE_TASK_ID).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(20L, 30L, 50L, 120L, 100L))
    );
    assertThat(
      result.getDataEntryForKey(SERVICE_TASK_ID_2).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(50L, 120L, 100L))
    );
  }

  @Test
  public void allVersionsRespectLatestNodesOnlyWhereLatestHasLessNodes() throws Exception {
    //given
    ProcessDefinitionEngineDto firstDefinition = deployProcessWithTwoTasks();
    ProcessDefinitionEngineDto latestDefinition = deploySimpleServiceTaskProcessDefinition();
    assertThat(latestDefinition.getVersion(), is(2));

    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(firstDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 20L);
    processInstanceDto = engineRule.startProcessInstance(firstDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 30L);
    processInstanceDto = engineRule.startProcessInstance(firstDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 50L);
    processInstanceDto = engineRule.startProcessInstance(latestDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 120L);
    processInstanceDto = engineRule.startProcessInstance(latestDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 100L);

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    //when
    ProcessReportDataDto reportData = createReport(latestDefinition.getKey(),
                                                   ALL_VERSIONS);
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    //then
    final ReportMapResult result = evaluationResponse.getResult();
    assertThat(result.getData().size(), is(3));
    assertThat(
      result.getDataEntryForKey(SERVICE_TASK_ID).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(20L, 30L, 50L, 120L, 100L))
    );
  }

  @Test
  public void multipleVersionsRespectLatestNodesOnlyWhereLatestHasLessNodes() throws Exception {
    //given
    ProcessDefinitionEngineDto firstDefinition = deployProcessWithTwoTasks();
    deployProcessWithTwoTasks();
    ProcessDefinitionEngineDto latestDefinition = deploySimpleServiceTaskProcessDefinition();
    assertThat(latestDefinition.getVersion(), is(3));

    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(firstDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 20L);
    processInstanceDto = engineRule.startProcessInstance(firstDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 30L);
    processInstanceDto = engineRule.startProcessInstance(firstDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 50L);
    processInstanceDto = engineRule.startProcessInstance(latestDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 120L);
    processInstanceDto = engineRule.startProcessInstance(latestDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 100L);

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    //when
    ProcessReportDataDto reportData =
      createReport(
        latestDefinition.getKey(),
        ImmutableList.of(firstDefinition.getVersionAsString(), latestDefinition.getVersionAsString())
      );
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    //then
    final ReportMapResult result = evaluationResponse.getResult();
    assertThat(result.getData().size(), is(3));
    assertThat(
      result.getDataEntryForKey(SERVICE_TASK_ID).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(20L, 30L, 50L, 120L, 100L))
    );
  }

  @Test
  public void reportEvaluationSingleBucketFilteredBySingleTenant() {
    // given
    final String tenantId1 = "tenantId1";
    final String tenantId2 = "tenantId2";
    final List<String> selectedTenants = newArrayList(tenantId1);
    final String processKey = deployAndStartMultiTenantSimpleServiceTaskProcess(
      newArrayList(null, tenantId1, tenantId2)
    );

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData = createReport(processKey, ALL_VERSIONS);
    reportData.setTenantIds(selectedTenants);
    ReportMapResult result = evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getInstanceCount(), CoreMatchers.is((long) selectedTenants.size()));
  }

  @Test
  public void otherProcessDefinitionsDoNotInfluenceResult() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessDefinitionEngineDto processDefinition2 = deploySimpleServiceTaskProcessDefinition();
    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 80L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 40L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 120L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition2.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 20L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition2.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 100L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition2.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 1000L);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData1 = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse1 =
      evaluateMapReport(reportData1);
    ProcessReportDataDto reportData2 = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition2);
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse2 =
      evaluateMapReport(reportData2);

    // then
    final ReportMapResult result1 = evaluationResponse1.getResult();
    assertThat(result1.getData().size(), is(3));
    assertThat(
      result1.getDataEntryForKey(SERVICE_TASK_ID).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(80L, 40L, 120L))
    );
    final ReportMapResult result2 = evaluationResponse2.getResult();
    assertThat(result2.getData().size(), is(3));
    assertThat(
      result2.getDataEntryForKey(SERVICE_TASK_ID).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(20L, 100L, 1000L))
    );
  }

  @Test
  public void evaluateReportWithIrrationalAverageNumberAsResult() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 100L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 300L);
    processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 600L);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    final Map<AggregationType, ReportMapResult> results = evaluateMapReportForAllAggTypes(
      reportData);


    // then
    assertDurationMapReportResults(results, new Long[]{100L, 300L, 600L});
  }

  @Test
  public void noEventMatchesReturnsEmptyResult() {

    // when
    ProcessReportDataDto reportData =
      createReport("nonExistingProcessDefinitionId", "1");
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    // then
    assertThat(evaluationResponse.getResult().getData().size(), is(0));
  }

  @Test
  public void evaluateReportWithExecutionStateRunning() throws SQLException {
    // given
    OffsetDateTime now = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(now);

    ProcessDefinitionEngineDto processDefinition = deploySimpleUserTaskDefinition();
    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), START_EVENT, 100L);
    engineDatabaseRule.changeActivityInstanceStartDate(
      processInstanceDto.getId(),
      USER_TASK,
      now.minus(200L, ChronoUnit.MILLIS)
    );

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData =
      createReport(processDefinition.getKey(),
                   processDefinition.getVersionAsString());
    reportData.getConfiguration().setFlowNodeExecutionState(FlowNodeExecutionState.RUNNING);
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    // then
    final ReportMapResult result = evaluationResponse.getResult();
    assertThat(result.getData().size(), is(3));
    assertThat(getExecutedFlowNodeCount(result), is(1L));
    assertThat(
      result.getDataEntryForKey(START_EVENT).get().getValue(),
      is(nullValue())
    );
    assertThat(
      result.getDataEntryForKey(USER_TASK).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(200L))
    );
  }

  @Test
  public void evaluateReportWithExecutionStateCompleted() throws SQLException {
    // given
    OffsetDateTime now = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(now);

    ProcessDefinitionEngineDto processDefinition = deploySimpleUserTaskDefinition();
    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), START_EVENT, 100L);
    engineDatabaseRule.changeActivityInstanceStartDate(
      processInstanceDto.getId(),
      USER_TASK,
      now.minus(200L, ChronoUnit.MILLIS)
    );

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData =
      createReport(processDefinition.getKey(),
                   processDefinition.getVersionAsString());
    reportData.getConfiguration().setFlowNodeExecutionState(FlowNodeExecutionState.COMPLETED);
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    // then
    final ReportMapResult result = evaluationResponse.getResult();
    assertThat(result.getData().size(), is(3));
    assertThat(getExecutedFlowNodeCount(result), is(1L));
    assertThat(
      result.getDataEntryForKey(START_EVENT).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(100L))
    );
    assertThat(
      result.getDataEntryForKey(USER_TASK).get().getValue(),
      is(nullValue())
    );
  }

  @Test
  public void evaluateReportWithExecutionStateAll() throws SQLException {
    // given
    OffsetDateTime now = OffsetDateTime.now();
    LocalDateUtil.setCurrentTime(now);

    ProcessDefinitionEngineDto processDefinition = deploySimpleUserTaskDefinition();
    ProcessInstanceEngineDto processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), START_EVENT, 100L);
    engineDatabaseRule.changeActivityInstanceStartDate(
      processInstanceDto.getId(),
      USER_TASK,
      now.minus(200L, ChronoUnit.MILLIS)
    );

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData =
      createReport(processDefinition.getKey(),
                   processDefinition.getVersionAsString());
    reportData.getConfiguration().setFlowNodeExecutionState(FlowNodeExecutionState.ALL);
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    // then
    final ReportMapResult result = evaluationResponse.getResult();
    assertThat(result.getData().size(), is(3));
    assertThat(getExecutedFlowNodeCount(result), is(2L));
    assertThat(
      result.getDataEntryForKey(START_EVENT).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(100L))
    );
    assertThat(
      result.getDataEntryForKey(USER_TASK).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(200L))
    );
  }

  @Test
  public void processDefinitionContainsMultiInstanceBody() throws Exception {
    // given
    // @formatter:off
    BpmnModelInstance subProcess = Bpmn.createExecutableProcess("subProcess")
        .startEvent()
          .serviceTask(SERVICE_TASK_ID)
            .camundaExpression("${true}")
        .endEvent()
        .done();

    BpmnModelInstance miProcess = Bpmn.createExecutableProcess("miProcess")
        .name("MultiInstance")
          .startEvent("miStart")
          .callActivity("callActivity")
            .calledElement("subProcess")
            .camundaIn("activityDuration", "activityDuration")
            .multiInstance()
              .cardinality("2")
            .multiInstanceDone()
          .endEvent("miEnd")
        .done();
    // @formatter:on
    ProcessDefinitionEngineDto subProcessDefinition = engineRule.deployProcessAndGetProcessDefinition(subProcess);
    String processDefinitionId = engineRule.deployProcessAndGetId(miProcess);
    engineRule.startProcessInstance(processDefinitionId);
    engineDatabaseRule.changeActivityDurationForProcessDefinition(subProcessDefinition.getId(), 10L);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData =
      createReport(subProcessDefinition.getKey(), subProcessDefinition.getVersionAsString());
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    // then
    final ReportMapResult result = evaluationResponse.getResult();
    assertThat(result.getData().size(), is(3));
    assertThat(getExecutedFlowNodeCount(result), is(3L));
    assertThat(
      result.getDataEntryForKey(SERVICE_TASK_ID).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(10L))
    );
  }

  @Test
  public void evaluateReportForMoreThenTenEvents() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();

    ProcessInstanceEngineDto processInstanceDto;
    for (int i = 0; i < 11; i++) {
      processInstanceDto = engineRule.startProcessInstance(processDefinition.getId());
      engineDatabaseRule.changeActivityDuration(processInstanceDto.getId(), 10L);
    }
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    // then
    final ReportMapResult result = evaluationResponse.getResult();
    assertThat(result.getData().size(), is(3));
    Long[] durationSet = new Long[11];
    Arrays.fill(durationSet, 10L);
    assertThat(
      result.getDataEntryForKey(SERVICE_TASK_ID).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(durationSet))
    );
  }

  @Test
  public void resultContainsNonExecutedFlowNodes() {
    // given
    BpmnModelInstance subProcess = Bpmn.createExecutableProcess()
      .startEvent("startEvent")
      .userTask("userTask")
      .endEvent("endEvent")
      .done();
    ProcessInstanceEngineDto engineDto = engineRule.deployAndStartProcess(subProcess);

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData = createReport(engineDto.getProcessDefinitionKey(),
                                                   engineDto.getProcessDefinitionVersion());
    ReportMapResult result = evaluateMapReport(reportData).getResult();

    // then
    assertThat(result.getData().size(), is(3));
    Long notExecutedFlowNodeResult = result.getData()
      .stream()
      .filter(r -> r.getKey().equals("endEvent"))
      .findFirst().get().getValue();
    assertThat(notExecutedFlowNodeResult, nullValue());
  }

  @Test
  public void filterInReport() throws Exception {
    // given
    ProcessDefinitionEngineDto processDefinition = deploySimpleServiceTaskProcessDefinition();
    ProcessInstanceEngineDto processInstance = engineRule.startProcessInstance(processDefinition.getId());
    engineDatabaseRule.changeActivityDuration(processInstance.getId(), 10L);
    OffsetDateTime past = engineRule.getHistoricProcessInstance(processInstance.getId()).getStartTime();
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    reportData.setFilter(createStartDateFilter(null, past.minusSeconds(1L)));
    AuthorizedProcessReportEvaluationResultDto<ReportMapResult> evaluationResponse = evaluateMapReport(
      reportData);

    // then
    ReportMapResult result = evaluationResponse.getResult();
    assertThat(result.getData(), is(notNullValue()));
    assertThat(result.getData().size(), is(3));
    assertThat(getExecutedFlowNodeCount(result), is(0L));

    // when
    reportData = getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(processDefinition);
    reportData.setFilter(createStartDateFilter(past, null));
    evaluationResponse = evaluateMapReport(reportData);

    // then
    result = evaluationResponse.getResult();
    assertThat(result.getData(), is(notNullValue()));
    assertThat(result.getData().size(), is(3));
    assertThat(
      result.getDataEntryForKey(SERVICE_TASK_ID).get().getValue(),
      is(calculateExpectedValueGivenDurationsDefaultAggr(10L))
    );
  }

  private List<ProcessFilterDto> createStartDateFilter(OffsetDateTime startDate, OffsetDateTime endDate) {
    return ProcessFilterBuilder.filter().fixedStartDate().start(startDate).end(endDate).add().buildList();
  }

  @Test
  public void optimizeExceptionOnViewEntityIsNull() {
    // given
    ProcessReportDataDto dataDto =
      createReport(PROCESS_DEFINITION_KEY, "1");
    dataDto.getView().setEntity(null);

    //when
    Response response = evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus(), is(500));
  }

  @Test
  public void optimizeExceptionOnViewPropertyIsNull() {
    // given
    ProcessReportDataDto dataDto =
      createReport(PROCESS_DEFINITION_KEY, "1");
    dataDto.getView().setProperty(null);

    //when
    Response response = evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus(), is(500));
  }

  @Test
  public void optimizeExceptionOnGroupByTypeIsNull() {
    // given
    ProcessReportDataDto dataDto =
      createReport(PROCESS_DEFINITION_KEY, "1");
    dataDto.getGroupBy().setType(null);

    //when
    Response response = evaluateReportAndReturnResponse(dataDto);

    // then
    assertThat(response.getStatus(), is(400));
  }

  private ProcessDefinitionEngineDto deploySimpleServiceTaskProcessDefinition() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess")
      .startEvent(START_EVENT)
      .serviceTask(SERVICE_TASK_ID)
      .camundaExpression("${true}")
      .endEvent(END_EVENT)
      .done();
    return engineRule.deployProcessAndGetProcessDefinition(modelInstance);
  }

  private ProcessDefinitionEngineDto deploySimpleUserTaskDefinition() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess")
      .startEvent(START_EVENT)
      .userTask(USER_TASK)
      .endEvent(END_EVENT)
      .done();
    return engineRule.deployProcessAndGetProcessDefinition(modelInstance);
  }

  private ProcessReportDataDto getAverageFlowNodeDurationGroupByFlowNodeHeatmapReport(ProcessDefinitionEngineDto processDefinition) {
    return createReport(processDefinition.getKey(), processDefinition.getVersionAsString());
  }


  private long getExecutedFlowNodeCount(ReportMapResult resultList) {
    return resultList.getData()
      .stream()
      .map(MapResultEntryDto::getValue)
      .filter(Objects::nonNull)
      .count();
  }

  private ProcessDefinitionEngineDto deployProcessWithTwoTasks() {
    // @formatter:off
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess")
      .startEvent(START_EVENT)
      .serviceTask(SERVICE_TASK_ID)
        .camundaExpression("${true}")
      .serviceTask(SERVICE_TASK_ID_2)
        .camundaExpression("${true}")
      .endEvent(END_EVENT)
      .done();
    // @formatter:on
    return engineRule.deployProcessAndGetProcessDefinition(modelInstance);
  }

  private Map<AggregationType, ReportMapResult> evaluateMapReportForAllAggTypes(final ProcessReportDataDto reportData) {

    Map<AggregationType, ReportMapResult> resultsMap =
      new HashMap<>();
    aggregationTypes.forEach((AggregationType aggType) -> {
      reportData.getConfiguration().setAggregationType(aggType);
      resultsMap.put(aggType, evaluateMapReport(reportData).getResult());
    });
    return resultsMap;
  }

  private ProcessReportDataDto createReport(String definitionKey, String definitionVersion) {
    return createReport(definitionKey, ImmutableList.of(definitionVersion));
  }

  private ProcessReportDataDto createReport(String definitionKey, List<String> definitionVersions) {
    return ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(definitionKey)
      .setProcessDefinitionVersions(definitionVersions)
      .setReportDataType(ProcessReportDataType.FLOW_NODE_DUR_GROUP_BY_FLOW_NODE)
      .build();
  }
}
