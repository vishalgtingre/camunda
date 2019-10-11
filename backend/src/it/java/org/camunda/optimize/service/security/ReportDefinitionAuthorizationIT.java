/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.optimize.dto.optimize.query.IdDto;
import org.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.combined.CombinedReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.SingleReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.DecisionReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.decision.SingleDecisionReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.ReportMapResult;
import org.camunda.optimize.dto.optimize.query.report.single.result.MapResultEntryDto;
import org.camunda.optimize.dto.optimize.query.sharing.ReportShareDto;
import org.camunda.optimize.dto.optimize.rest.report.AuthorizedCombinedReportEvaluationResultDto;
import org.camunda.optimize.dto.optimize.rest.report.AuthorizedProcessReportEvaluationResultDto;
import org.camunda.optimize.dto.optimize.rest.report.CombinedProcessReportResultDataDto;
import org.camunda.optimize.test.engine.AuthorizationClient;
import org.camunda.optimize.test.it.rule.ElasticSearchIntegrationTestRule;
import org.camunda.optimize.test.it.rule.EmbeddedOptimizeRule;
import org.camunda.optimize.test.it.rule.EngineIntegrationRule;
import org.camunda.optimize.test.util.ProcessReportDataBuilder;
import org.camunda.optimize.test.util.ProcessReportDataType;
import org.camunda.optimize.test.util.decision.DecisionReportDataBuilder;
import org.camunda.optimize.test.util.decision.DecisionReportDataType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.RESOURCE_TYPE_DECISION_DEFINITION;
import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.RESOURCE_TYPE_PROCESS_DEFINITION;
import static org.camunda.optimize.service.util.configuration.EngineConstantsUtil.RESOURCE_TYPE_TENANT;
import static org.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static org.camunda.optimize.test.it.rule.TestEmbeddedCamundaOptimize.DEFAULT_PASSWORD;
import static org.camunda.optimize.test.it.rule.TestEmbeddedCamundaOptimize.DEFAULT_USERNAME;
import static org.camunda.optimize.test.util.ProcessReportDataBuilderHelper.createCombinedReport;
import static org.camunda.optimize.test.util.decision.DmnHelper.createSimpleDmnModel;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@RunWith(JUnitParamsRunner.class)
public class ReportDefinitionAuthorizationIT {

  private static final String PROCESS_KEY = "aprocess";
  private static final String DECISION_KEY = "aDecision";

  private static final Object[] definitionType() {
    return new Object[]{RESOURCE_TYPE_PROCESS_DEFINITION, RESOURCE_TYPE_DECISION_DEFINITION};
  }

  public EngineIntegrationRule engineRule = new EngineIntegrationRule();
  public ElasticSearchIntegrationTestRule elasticSearchRule = new ElasticSearchIntegrationTestRule();
  public EmbeddedOptimizeRule embeddedOptimizeRule = new EmbeddedOptimizeRule();
  private AuthorizationClient authorizationClient = new AuthorizationClient(engineRule);

  @Rule
  public RuleChain chain = RuleChain
    .outerRule(elasticSearchRule).around(engineRule).around(embeddedOptimizeRule);


  @Test
  @Parameters(method = "definitionType")
  public void evaluateUnauthorizedStoredReport(int definitionResourceType) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    deployStartAndImportDefinition(definitionResourceType);

    String reportId = createReportForDefinition(definitionResourceType);

    // when
    Response response = embeddedOptimizeRule
      .getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildEvaluateSavedReportRequest(reportId)
      .execute();

    // then
    assertThat(response.getStatus(), is(403));
  }

  @Test
  @Parameters(method = "definitionType")
  public void evaluateUnauthorizedTenantsStoredReport(int definitionResourceType) {
    // given
    final String tenantId = "tenant1";
    engineRule.createTenant(tenantId);

    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.addGlobalAuthorizationForResource(definitionResourceType);
    deployStartAndImportDefinition(definitionResourceType);

    String reportId = createReportForDefinition(definitionResourceType, ImmutableList.of(tenantId));

    // when
    Response response = embeddedOptimizeRule
      .getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildEvaluateSavedReportRequest(reportId)
      .execute();

    // then
    assertThat(response.getStatus(), is(403));
  }

  @Test
  @Parameters(method = "definitionType")
  public void evaluatePartiallyUnauthorizedTenantsStoredReport(int definitionResourceType) {
    // given
    final String tenantId1 = "tenant1";
    engineRule.createTenant(tenantId1);
    final String tenantId2 = "tenant2";
    engineRule.createTenant(tenantId2);

    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.addGlobalAuthorizationForResource(definitionResourceType);
    authorizationClient.grantSingleResourceAuthorizationsForUser(KERMIT_USER, tenantId1, RESOURCE_TYPE_TENANT);
    deployStartAndImportDefinition(definitionResourceType);

    String reportId = createReportForDefinition(definitionResourceType, ImmutableList.of(tenantId1, tenantId2));

    // when
    Response response = embeddedOptimizeRule
      .getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildEvaluateSavedReportRequest(reportId)
      .execute();

    // then
    assertThat(response.getStatus(), is(403));
  }

  @Test
  @Parameters(method = "definitionType")
  public void evaluateAllTenantsAuthorizedStoredReport(int definitionResourceType) {
    // given
    final String tenantId1 = "tenant1";
    engineRule.createTenant(tenantId1);
    final String tenantId2 = "tenant2";
    engineRule.createTenant(tenantId2);

    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.addGlobalAuthorizationForResource(definitionResourceType);
    authorizationClient.grantSingleResourceAuthorizationsForUser(KERMIT_USER, tenantId1, RESOURCE_TYPE_TENANT);
    authorizationClient.grantSingleResourceAuthorizationsForUser(KERMIT_USER, tenantId2, RESOURCE_TYPE_TENANT);
    deployStartAndImportDefinition(definitionResourceType);

    String reportId = createReportForDefinitionAsUser(
      definitionResourceType, ImmutableList.of(tenantId1, tenantId2), KERMIT_USER, KERMIT_USER
    );

    // when
    Response response = embeddedOptimizeRule
      .getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildEvaluateSavedReportRequest(reportId)
      .execute();

    // then
    assertThat(response.getStatus(), is(200));
  }

  @Test
  @Parameters(method = "definitionType")
  public void deleteUnauthorizedStoredReport(int definitionResourceType) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    deployStartAndImportDefinition(definitionResourceType);

    String reportId = createReportForDefinition(definitionResourceType);

    // when
    Response response = embeddedOptimizeRule
      .getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildDeleteReportRequest(reportId)
      .execute();

    // then
    assertThat(response.getStatus(), is(403));
  }

  @Test
  @Parameters(method = "definitionType")
  public void evaluateUnauthorizedOnTheFlyReport(int definitionResourceType) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    deployStartAndImportDefinition(definitionResourceType);

    // when
    ReportDefinitionDto<SingleReportDataDto> definition = constructReportWithDefinition(definitionResourceType);
    Response response = embeddedOptimizeRule
      .getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildEvaluateSingleUnsavedReportRequest(definition.getData())
      .execute();

    // then
    assertThat(response.getStatus(), is(403));
  }

  @Test
  @Parameters(method = "definitionType")
  public void updateUnauthorizedReport(int definitionResourceType) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    deployStartAndImportDefinition(definitionResourceType);

    String reportId = createReportForDefinition(definitionResourceType);

    ReportDefinitionDto updatedReport = createReportUpdate(definitionResourceType);

    // when
    Response response = embeddedOptimizeRule
      .getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildUpdateSingleReportRequest(reportId, updatedReport)
      .execute();

    // then
    assertThat(response.getStatus(), is(403));
  }

  @Test
  @Parameters(method = "definitionType")
  public void getUnauthorizedReport(int definitionResourceType) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    deployStartAndImportDefinition(definitionResourceType);

    String reportId = createReportForDefinition(definitionResourceType);

    // when
    Response response = embeddedOptimizeRule
      .getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildGetReportRequest(reportId)
      .execute();

    // then
    assertThat(response.getStatus(), is(403));
  }

  @Test
  @Parameters(method = "definitionType")
  public void shareUnauthorizedReport(int definitionResourceType) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    deployStartAndImportDefinition(definitionResourceType);

    String reportId = createReportForDefinition(definitionResourceType);
    ReportShareDto reportShareDto = new ReportShareDto();
    reportShareDto.setReportId(reportId);

    // when
    Response response = embeddedOptimizeRule
      .getRequestExecutor()
      .buildShareReportRequest(reportShareDto)
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .execute();

    // then
    assertThat(response.getStatus(), is(403));
  }

  @Test
  @Parameters(method = "definitionType")
  public void newPrivateReportsCanOnlyBeAccessedByOwner(int definitionResourceType) {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    deployStartAndImportDefinition(definitionResourceType);

    String reportId = createNewReport(definitionResourceType);

    // when
    Response response = embeddedOptimizeRule
      .getRequestExecutor()
      .buildGetReportRequest(reportId)
      .execute();

    // then
    assertThat(response.getStatus(), is(200));

    // when
    Response otherUserResponse = embeddedOptimizeRule
      .getRequestExecutor()
      .buildGetReportRequest(reportId)
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .execute();

    // then
    assertThat(otherUserResponse.getStatus(), is(403));
  }

  @Test
  public void unauthorizedReportInCombinedIsNotEvaluated() {
    // given
    final String authorizedProcessDefinitionKey = "aprocess";
    final String notAuthorizedProcessDefinitionKey = "notAuthorizedProcess";
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    deployAndStartSimpleProcessDefinition(authorizedProcessDefinitionKey);
    authorizationClient.grantSingleResourceAuthorizationForKermit(
      authorizedProcessDefinitionKey,
      RESOURCE_TYPE_PROCESS_DEFINITION
    );
    deployAndStartSimpleProcessDefinition(notAuthorizedProcessDefinitionKey);
    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();

    String authorizedReportId = createNewSingleMapReportAsUser(
      authorizedProcessDefinitionKey, KERMIT_USER, KERMIT_USER
    );
    String notAuthorizedReportId = createNewSingleMapReportAsUser(
      notAuthorizedProcessDefinitionKey, KERMIT_USER, KERMIT_USER
    );

    // when
    CombinedReportDataDto combinedReport = createCombinedReport(authorizedReportId, notAuthorizedReportId);

    CombinedProcessReportResultDataDto<ReportMapResult> result = embeddedOptimizeRule
      .getRequestExecutor()
      .buildEvaluateCombinedUnsavedReportRequest(combinedReport)
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .execute(new TypeReference<AuthorizedCombinedReportEvaluationResultDto<ReportMapResult>>() {
      })
      .getResult();

    // then
    Map<String, AuthorizedProcessReportEvaluationResultDto<ReportMapResult>> resultMap = result.getData();
    assertThat(resultMap.size(), is(1));
    assertThat(resultMap.containsKey(notAuthorizedReportId), is(false));
    List<MapResultEntryDto<Long>> flowNodeToCount = resultMap.get(authorizedReportId).getResult().getData();
    assertThat(flowNodeToCount.size(), is(2));
  }

  private String getDefinitionKey(final int definitionResourceType) {
    return definitionResourceType == RESOURCE_TYPE_PROCESS_DEFINITION ? PROCESS_KEY : DECISION_KEY;
  }

  private String createNewSingleMapReportAsUser(final String processDefinitionKey,
                                                final String user,
                                                final String password) {
    String singleReportId = createNewReportAsUser(RESOURCE_TYPE_PROCESS_DEFINITION, user, password);
    ProcessReportDataDto countFlowNodeFrequencyGroupByFlowNode = ProcessReportDataBuilder
      .createReportData()
      .setProcessDefinitionKey(processDefinitionKey)
      .setProcessDefinitionVersion("1")
      .setReportDataType(ProcessReportDataType.COUNT_FLOW_NODE_FREQ_GROUP_BY_FLOW_NODE)
      .build();
    SingleProcessReportDefinitionDto definitionDto = new SingleProcessReportDefinitionDto();
    definitionDto.setData(countFlowNodeFrequencyGroupByFlowNode);
    updateReportAsUser(singleReportId, definitionDto, user, password);
    return singleReportId;
  }

  private void deployStartAndImportDefinition(int definitionResourceType) {
    switch (definitionResourceType) {
      case RESOURCE_TYPE_PROCESS_DEFINITION:
        deployAndStartSimpleProcessDefinition(PROCESS_KEY);
        break;
      case RESOURCE_TYPE_DECISION_DEFINITION:
        deployAndStartSimpleDecisionDefinition(DECISION_KEY);
        break;
      default:
        throw new IllegalStateException("Uncovered definitionResourceType: " + definitionResourceType);
    }

    embeddedOptimizeRule.importAllEngineEntitiesFromScratch();
    elasticSearchRule.refreshAllOptimizeIndices();
  }

  private void deployAndStartSimpleProcessDefinition(String processKey) {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(processKey)
      .startEvent()
      .endEvent()
      .done();
    engineRule.deployAndStartProcess(modelInstance);
  }

  private void deployAndStartSimpleDecisionDefinition(String decisionKey) {
    final DmnModelInstance modelInstance = createSimpleDmnModel(decisionKey);
    engineRule.deployAndStartDecisionDefinition(modelInstance);
  }

  private ReportDefinitionDto createReportUpdate(int definitionResourceType) {
    switch (definitionResourceType) {
      default:
      case RESOURCE_TYPE_PROCESS_DEFINITION:
        ProcessReportDataDto processReportData = new ProcessReportDataDto();
        processReportData.setProcessDefinitionKey("procdef");
        processReportData.setProcessDefinitionVersion("123");
        processReportData.setFilter(Collections.emptyList());
        SingleProcessReportDefinitionDto processReport = new SingleProcessReportDefinitionDto();
        processReport.setData(processReportData);
        processReport.setName("MyReport");
        return processReport;
      case RESOURCE_TYPE_DECISION_DEFINITION:
        DecisionReportDataDto decisionReportData = new DecisionReportDataDto();
        decisionReportData.setDecisionDefinitionKey("Decisionef");
        decisionReportData.setDecisionDefinitionVersion("123");
        decisionReportData.setFilter(Collections.emptyList());
        SingleDecisionReportDefinitionDto decisionReport = new SingleDecisionReportDefinitionDto();
        decisionReport.setData(decisionReportData);
        decisionReport.setName("MyReport");
        return decisionReport;
    }
  }

  private String createReportForDefinition(final int resourceType) {
    return createReportForDefinition(resourceType, Collections.emptyList());
  }

  private String createReportForDefinition(final int resourceType, final List<String> tenantIds) {
    return createReportForDefinitionAsUser(resourceType, tenantIds, DEFAULT_USERNAME, DEFAULT_PASSWORD);
  }

  private String createReportForDefinitionAsUser(final int resourceType,
                                                 final List<String> tenantIds,
                                                 final String user,
                                                 final String password) {
    String id = createNewReportAsUser(resourceType, user, password);
    ReportDefinitionDto definition = constructReportWithDefinition(resourceType, tenantIds);
    updateReportAsUser(id, definition, user, password);
    return id;
  }

  private String createNewReport(int resourceType) {
    return createNewReportAsUser(resourceType, DEFAULT_USERNAME, DEFAULT_PASSWORD);
  }

  private String createNewReportAsUser(int resourceType, final String user, final String password) {
    switch (resourceType) {
      default:
      case RESOURCE_TYPE_PROCESS_DEFINITION:
        return embeddedOptimizeRule
          .getRequestExecutor()
          .withUserAuthentication(user, password)
          .buildCreateSingleProcessReportRequest()
          .execute(IdDto.class, 200)
          .getId();
      case RESOURCE_TYPE_DECISION_DEFINITION:
        return embeddedOptimizeRule
          .getRequestExecutor()
          .withUserAuthentication(user, password)
          .buildCreateSingleDecisionReportRequest()
          .execute(IdDto.class, 200)
          .getId();
    }
  }

  private void updateReport(String id, ReportDefinitionDto updatedReport) {
    updateReportAsUser(id, updatedReport, DEFAULT_USERNAME, DEFAULT_PASSWORD);
  }

  private void updateReportAsUser(String id, ReportDefinitionDto updatedReport, final String user,
                                  final String password) {
    Response response = getUpdateReportResponse(id, updatedReport, user, password);
    assertThat(response.getStatus(), is(204));
  }

  private ReportDefinitionDto constructReportWithDefinition(int resourceType) {
    return constructReportWithDefinition(resourceType, Collections.emptyList());
  }

  private ReportDefinitionDto constructReportWithDefinition(int resourceType, List<String> tenantIds) {
    switch (resourceType) {
      default:
      case RESOURCE_TYPE_PROCESS_DEFINITION:
        SingleProcessReportDefinitionDto processReportDefinitionDto = new SingleProcessReportDefinitionDto();
        ProcessReportDataDto processReportDataDto = ProcessReportDataBuilder
          .createReportData()
          .setProcessDefinitionKey(getDefinitionKey(resourceType))
          .setProcessDefinitionVersion("1")
          .setReportDataType(ProcessReportDataType.RAW_DATA)
          .build();
        processReportDataDto.setTenantIds(tenantIds);
        processReportDefinitionDto.setData(processReportDataDto);
        return processReportDefinitionDto;
      case RESOURCE_TYPE_DECISION_DEFINITION:
        SingleDecisionReportDefinitionDto decisionReportDefinitionDto = new SingleDecisionReportDefinitionDto();
        DecisionReportDataDto decisionReportDataDto = DecisionReportDataBuilder.create()
          .setDecisionDefinitionKey(getDefinitionKey(resourceType))
          .setDecisionDefinitionVersion("1")
          .setReportDataType(DecisionReportDataType.RAW_DATA)
          .build();
        decisionReportDataDto.setTenantIds(tenantIds);
        decisionReportDefinitionDto.setData(decisionReportDataDto);
        return decisionReportDefinitionDto;
    }
  }

  private Response getUpdateReportResponse(final String id,
                                           final ReportDefinitionDto updatedReport,
                                           final String user,
                                           final String password) {
    switch (updatedReport.getReportType()) {
      default:
      case PROCESS:
        return embeddedOptimizeRule
          .getRequestExecutor()
          .withUserAuthentication(user, password)
          .buildUpdateSingleProcessReportRequest(id, (SingleProcessReportDefinitionDto) updatedReport)
          .execute();
      case DECISION:
        return embeddedOptimizeRule
          .getRequestExecutor()
          .withUserAuthentication(user, password)
          .buildUpdateSingleDecisionReportRequest(id, (SingleDecisionReportDefinitionDto) updatedReport)
          .execute();
    }
  }

}
