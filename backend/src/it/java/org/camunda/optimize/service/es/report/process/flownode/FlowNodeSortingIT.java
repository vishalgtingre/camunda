/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.process.flownode;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.optimize.dto.optimize.query.report.single.configuration.sorting.SortOrder;
import org.camunda.optimize.dto.optimize.query.report.single.configuration.sorting.SortingDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.ReportMapResult;
import org.camunda.optimize.dto.optimize.query.report.single.result.MapResultEntryDto;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.service.es.report.process.AbstractProcessDefinitionIT;
import org.camunda.optimize.test.util.ProcessReportDataBuilder;
import org.camunda.optimize.test.util.ProcessReportDataType;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.camunda.optimize.dto.optimize.query.report.single.configuration.sorting.SortingDto.SORT_BY_KEY;
import static org.camunda.optimize.dto.optimize.query.report.single.configuration.sorting.SortingDto.SORT_BY_LABEL;
import static org.camunda.optimize.dto.optimize.query.report.single.configuration.sorting.SortingDto.SORT_BY_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;


public class FlowNodeSortingIT extends AbstractProcessDefinitionIT {

  private static final String LABEL_SUFFIX = "_label";
  private static final String TEST_ACTIVITY = "testActivity";
  private static final String TEST_ACTIVITY_2 = "testActivity_2";
  private static final String USER_TASK = "userTask";

  @Test
  public void customOrderOnResultLabelForFrequencyReports() {
    // given
    final ProcessInstanceEngineDto processInstanceDto = deployProcessWithTwoTasksAndLabels();

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    final ProcessReportDataDto reportData = createReport(processInstanceDto);
    reportData.getConfiguration().setSorting(new SortingDto(SORT_BY_LABEL, SortOrder.ASC));
    final ReportMapResult result = evaluateMapReport(reportData).getResult();

    // then
    final List<MapResultEntryDto<Long>> resultData = result.getData();
    assertThat(resultData.size(), is(4));
    assertThat(getExecutedFlowNodeCount(result), is(4L));
    final List<String> resultLabels = resultData.stream()
      .map(MapResultEntryDto::getLabel)
      .collect(Collectors.toList());
    assertThat(
      resultLabels,
      // expect ascending order
      contains(resultLabels.stream().sorted(Comparator.naturalOrder()).toArray())
    );
  }

  private ProcessReportDataDto createReport(ProcessInstanceEngineDto processInstanceDto) {
    return ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(processInstanceDto.getProcessDefinitionKey())
      .setProcessDefinitionVersion(processInstanceDto.getProcessDefinitionVersion())
      .setReportDataType(ProcessReportDataType.COUNT_FLOW_NODE_FREQ_GROUP_BY_FLOW_NODE)
      .build();
  }

  @Test
  public void customOrderOnResultLabelForDurationReports() {
    // given
    final ProcessInstanceEngineDto processDefinition = deployProcessWithTwoTasksAndLabels();

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    final ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeReport(processDefinition);
    reportData.getConfiguration().setSorting(new SortingDto(SORT_BY_LABEL, SortOrder.ASC));
    ReportMapResult result = evaluateMapReport(reportData).getResult();

    // then
    List<MapResultEntryDto<Long>> resultData = result.getData();
    assertThat(resultData.size(), is(4));
    assertThat(getExecutedFlowNodeDuration(result), is(4L));
    final List<String> resultLabels = resultData.stream()
      .map(MapResultEntryDto::getLabel)
      .collect(Collectors.toList());
    assertThat(
      resultLabels,
      // expect ascending order
      contains(resultLabels.stream().sorted(Comparator.naturalOrder()).toArray())
    );
  }

  @Test
  public void ifNameIsNotAvailableKeyIsUsedAsLabel() {
    // given
    // @formatter:off
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess")
      .name("aProcessName")
      .startEvent("startKey")
        .name("startName")
      .serviceTask("task1Key")
        .name("task1Name")
        .camundaExpression("${true}")
      .serviceTask("task2Key")
        .name("")
        .camundaExpression("${true}")
      .endEvent("endKey")
        .name(null)
      .done();
    // @formatter:on
    ProcessInstanceEngineDto processInstanceDto = engineRule.deployAndStartProcess(modelInstance);

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    final ProcessReportDataDto reportData = createReport(processInstanceDto);
    reportData.getConfiguration().setSorting(new SortingDto(SORT_BY_LABEL, SortOrder.ASC));
    final ReportMapResult result = evaluateMapReport(reportData).getResult();

    // then
    final List<MapResultEntryDto<Long>> resultData = result.getData();
    assertThat(resultData.size(), is(4));
    assertThat(getExecutedFlowNodeCount(result), is(4L));
    final List<String> resultLabels = resultData.stream()
      .map(MapResultEntryDto::getLabel)
      .collect(Collectors.toList());
    assertThat(
      resultLabels,
      // expect ascending order
      contains("endKey", "startName", "task1Name", "task2Key")
    );
  }

  @Test
  public void labelSortingIsCaseInsensitive() {
    // given
    // @formatter:off
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess")
      .name("aProcessName")
      .startEvent("start")
        .name("ax")
      .serviceTask(TEST_ACTIVITY)
        .name("fooBar1")
        .camundaExpression("${true}")
      .serviceTask(TEST_ACTIVITY_2)
        .name("Ac")
        .camundaExpression("${true}")
      .endEvent("end")
        .name("foobar2")
      .done();
    // @formatter:on
    ProcessInstanceEngineDto processInstanceDto = engineRule.deployAndStartProcess(modelInstance);

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    final ProcessReportDataDto reportData = createReport(processInstanceDto);
    reportData.getConfiguration().setSorting(new SortingDto(SORT_BY_LABEL, SortOrder.ASC));
    final ReportMapResult result = evaluateMapReport(reportData).getResult();

    // then
    final List<MapResultEntryDto<Long>> resultData = result.getData();
    assertThat(resultData.size(), is(4));
    assertThat(getExecutedFlowNodeCount(result), is(4L));
    final List<String> resultLabels = resultData.stream()
      .map(MapResultEntryDto::getLabel)
      .collect(Collectors.toList());
    assertThat(
      resultLabels,
      // expect ascending order
      contains("Ac", "ax", "fooBar1", "foobar2")
    );
  }

  @Test
  public void keySortingIsCaseInsensitive() {
    // given
    // @formatter:off
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess")
      .name("aProcessName")
      .startEvent("ax")
      .serviceTask("fooBar1")
        .camundaExpression("${true}")
      .serviceTask("Ac")
        .camundaExpression("${true}")
      .endEvent("foobar2")
      .done();
    // @formatter:on
    ProcessInstanceEngineDto processInstanceDto = engineRule.deployAndStartProcess(modelInstance);

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    final ProcessReportDataDto reportData = createReport(processInstanceDto);
    reportData.getConfiguration().setSorting(new SortingDto(SORT_BY_KEY, SortOrder.ASC));
    final ReportMapResult result = evaluateMapReport(reportData).getResult();

    // then
    final List<MapResultEntryDto<Long>> resultData = result.getData();
    assertThat(resultData.size(), is(4));
    final List<String> resultLabels = resultData.stream()
      .map(MapResultEntryDto::getLabel)
      .collect(Collectors.toList());
    assertThat(
      resultLabels,
      // expect ascending order
      contains("Ac", "ax", "fooBar1", "foobar2")
    );
  }

  @Test
  public void descendingOrderOnResultValueForDurationReportsWithNullsLast() {
    testExpectedResultValueOrderForDurationReports(Comparator.nullsLast(Comparator.reverseOrder()), SortOrder.DESC);
  }

  @Test
  public void ascendingOnResultValueForDurationReportsWithNullsLast() {
    testExpectedResultValueOrderForDurationReports(Comparator.nullsLast(Comparator.naturalOrder()), SortOrder.ASC);
  }

  private void testExpectedResultValueOrderForDurationReports(final Comparator<Long> expectedOrderComparator,
                                                              final SortOrder sortOrder) {
    // given
    final ProcessInstanceEngineDto processDefinition = deployProcessWithServiceAndUserTask();

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    // when
    final ProcessReportDataDto reportData = getAverageFlowNodeDurationGroupByFlowNodeReport(processDefinition);
    reportData.getConfiguration().setSorting(new SortingDto(SORT_BY_VALUE, sortOrder));
    ReportMapResult result = evaluateMapReport(reportData).getResult();

    // then
    List<MapResultEntryDto<Long>> resultData = result.getData();
    assertThat(resultData.size(), is(4));
    // end activity not executed due running userTask
    assertThat(getExecutedFlowNodeDuration(result), is(3L));
    final List<Long> resultLabels = resultData.stream()
      .map(MapResultEntryDto::getValue)
      .collect(Collectors.toList());
    assertThat(
      resultLabels,
      // expect ascending order, nulls last
      contains(resultLabels.stream().sorted(expectedOrderComparator).toArray())
    );
  }

  private ProcessInstanceEngineDto deployProcessWithTwoTasksAndLabels() {
    // @formatter:off
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess")
      .name("aProcessName")
      .startEvent("start")
        .name("start" + LABEL_SUFFIX)
      .serviceTask(TEST_ACTIVITY)
        .name(TEST_ACTIVITY + LABEL_SUFFIX)
        .camundaExpression("${true}")
      .serviceTask(TEST_ACTIVITY_2)
        .name(TEST_ACTIVITY_2 + LABEL_SUFFIX)
        .camundaExpression("${true}")
      .endEvent("end")
        .name(null)
      .done();
    // @formatter:on
    return engineRule.deployAndStartProcess(modelInstance);
  }

  private ProcessInstanceEngineDto deployProcessWithServiceAndUserTask() {
    // @formatter:off
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess")
      .name("aProcessName")
      .startEvent("start")
        .name("start" + LABEL_SUFFIX)
      .serviceTask(TEST_ACTIVITY)
        .name(TEST_ACTIVITY + LABEL_SUFFIX)
        .camundaExpression("${true}")
      .userTask(USER_TASK)
        .name(USER_TASK + LABEL_SUFFIX)
      .endEvent("end")
        .name(null)
      .done();
    // @formatter:on
    return engineRule.deployAndStartProcess(modelInstance);
  }

  private ProcessReportDataDto getAverageFlowNodeDurationGroupByFlowNodeReport(ProcessInstanceEngineDto processDefinition) {
    return ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(processDefinition.getProcessDefinitionKey())
      .setProcessDefinitionVersion(processDefinition.getProcessDefinitionVersion())
      .setReportDataType(ProcessReportDataType.FLOW_NODE_DUR_GROUP_BY_FLOW_NODE)
      .build();
  }

  private long getExecutedFlowNodeCount(ReportMapResult resultList) {
    return resultList.getData().stream().filter(result -> result.getValue() > 0).count();
  }

  private long getExecutedFlowNodeDuration(ReportMapResult resultList) {
    return resultList.getData()
      .stream()
      .map(MapResultEntryDto::getValue)
      .filter(Objects::nonNull)
      .count();
  }
}
