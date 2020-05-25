/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.security.authorization;

import com.google.common.collect.Lists;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.optimize.AbstractIT;
import org.camunda.optimize.dto.engine.definition.DecisionDefinitionEngineDto;
import org.camunda.optimize.dto.optimize.query.variable.DecisionVariableNameRequestDto;
import org.camunda.optimize.dto.optimize.query.variable.DecisionVariableValueRequestDto;
import org.camunda.optimize.dto.optimize.query.variable.VariableType;
import org.camunda.optimize.test.engine.AuthorizationClient;
import org.camunda.optimize.test.util.decision.DecisionTypeRef;
import org.camunda.optimize.test.util.decision.DmnModelGenerator;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.optimize.service.util.configuration.EngineConstants.RESOURCE_TYPE_DECISION_DEFINITION;
import static org.camunda.optimize.service.util.configuration.EngineConstants.RESOURCE_TYPE_TENANT;
import static org.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DecisionVariableAuthorizationIT extends AbstractIT {

  private static final String PROCESS_DEFINITION_KEY = "aProcessDefinitionKey";
  private static final String VARIABLE_NAME = "input";
  private static final String VARIABLE_VALUE = "input";

  private AuthorizationClient authorizationClient = new AuthorizationClient(engineIntegrationExtension);

  @Test
  public void variableRequest_authorized() {
    // given
    final DecisionDefinitionEngineDto decisionDefinition = deploySimpleDecisionDefinition();
    evaluateSimpleDecision(decisionDefinition);
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_DECISION_DEFINITION);

    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    //when
    List<Response> responses = executeVariableRequestsAsKermit(decisionDefinition);

    //then
    responses.forEach(response -> assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()))
    );
  }

  @Test
  public void variableRequest_noneTenantAuthorized() {
    // given
    final DecisionDefinitionEngineDto decisionDefinition = deploySimpleDecisionDefinition();
    evaluateSimpleDecision(decisionDefinition);
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_DECISION_DEFINITION);

    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    //when
    List<Response> responses = executeVariableRequestsAsKermit(decisionDefinition, Collections.singletonList(null));

    //then
    responses.forEach(response -> assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()))
    );
  }

  @Test
  public void variableRequestWithoutAuthorization() {
    // given
    final DecisionDefinitionEngineDto decisionDefinition = deploySimpleDecisionDefinition();
    evaluateSimpleDecision(decisionDefinition);
    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    Response inputVariableNameResponse = embeddedOptimizeExtension
      .getRequestExecutor()
      .withoutAuthentication()
      .buildDecisionInputVariableNamesRequest(
        createInputVariableNameRequest("", "", Collections.emptyList())
      )
      .execute();
    Response outputVariableNameResponse = embeddedOptimizeExtension
      .getRequestExecutor()
      .withoutAuthentication()
      .buildDecisionOutputVariableNamesRequest(
        createInputVariableNameRequest("", "", Collections.emptyList())
      )
      .execute();
    Response inputVariableValueResponse = embeddedOptimizeExtension
      .getRequestExecutor()
      .withoutAuthentication()
      .buildDecisionInputVariableValuesRequest(
        createVariableValueRequest("", "", Collections.emptyList())
      )
      .execute();
    Response outputVariableValueResponse = embeddedOptimizeExtension
      .getRequestExecutor()
      .withoutAuthentication()
      .buildDecisionOutputVariableValuesRequest(
        createVariableValueRequest("", "", Collections.emptyList())
      )
      .execute();

    // then
    Arrays.asList(
      inputVariableNameResponse,
      inputVariableValueResponse,
      outputVariableNameResponse,
      outputVariableValueResponse
    ).forEach(response ->
                assertThat(
                  response.getStatus(),
                  is(Response.Status.UNAUTHORIZED.getStatusCode())
                )
    );
  }

  @Test
  public void variableRequest_authorizedTenant() {
    // given
    final String tenantId = "tenantId";
    final DecisionDefinitionEngineDto decisionDefinition = deploySimpleDecisionDefinition(tenantId);
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_DECISION_DEFINITION);
    authorizationClient.grantSingleResourceAuthorizationsForUser(KERMIT_USER, tenantId, RESOURCE_TYPE_TENANT);
    evaluateSimpleDecision(decisionDefinition);

    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    //when
    List<Response> responses = executeVariableRequestsAsKermit(decisionDefinition);

    //then
    responses.forEach(response -> assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()))
    );
  }

  @Test
  public void variableRequest_unauthorizedTenant() {
    // given
    final String tenantId = "tenantId";
    final DecisionDefinitionEngineDto decisionDefinition = deploySimpleDecisionDefinition(tenantId);
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_DECISION_DEFINITION);
    evaluateSimpleDecision(decisionDefinition);

    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    //when
    List<Response> responses = executeVariableRequestsAsKermit(decisionDefinition);

    //then
    responses.forEach(response -> assertThat(response.getStatus(), is(Response.Status.FORBIDDEN.getStatusCode()))
    );
  }

  @Test
  public void variableRequest_partiallyUnauthorizedTenants() {
    // given
    final String tenantId1 = "tenantId1";
    final String tenantId2 = "tenantId2";
    final DecisionDefinitionEngineDto decisionDefinition1 = deploySimpleDecisionDefinition(tenantId1);
    final DecisionDefinitionEngineDto decisionDefinition2 = deploySimpleDecisionDefinition(tenantId2);
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_DECISION_DEFINITION);
    authorizationClient.grantSingleResourceAuthorizationsForUser(KERMIT_USER, tenantId1, RESOURCE_TYPE_TENANT);
    evaluateSimpleDecision(decisionDefinition1);
    evaluateSimpleDecision(decisionDefinition2);

    embeddedOptimizeExtension.importAllEngineEntitiesFromScratch();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    //when
    List<Response> responses = executeVariableRequestsAsKermit(
      decisionDefinition1,
      Lists.newArrayList(tenantId1, tenantId2)
    );

    //then
    responses.forEach(response -> assertThat(response.getStatus(), is(Response.Status.FORBIDDEN.getStatusCode()))
    );
  }

  private List<Response> executeVariableRequestsAsKermit(final DecisionDefinitionEngineDto decisionDefinition) {
    return executeVariableRequestsAsKermit(
      decisionDefinition,
      decisionDefinition.getTenantId().map(Collections::singletonList).orElse(Collections.emptyList())
    );
  }

  private List<Response> executeVariableRequestsAsKermit(final DecisionDefinitionEngineDto decisionDefinition,
                                                         List<String> tenantIds) {
    Response inputVariableNameResponse = embeddedOptimizeExtension
      .getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildDecisionInputVariableNamesRequest(createInputVariableNameRequest(
        decisionDefinition.getKey(), String.valueOf(decisionDefinition.getVersion()), tenantIds
      ))
      .execute();
    Response outputVariableNameResponse = embeddedOptimizeExtension
      .getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildDecisionOutputVariableNamesRequest(createInputVariableNameRequest(
        decisionDefinition.getKey(), String.valueOf(decisionDefinition.getVersion()), tenantIds
      ))
      .execute();

    Response inputVariableValueResponse = embeddedOptimizeExtension
      .getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildDecisionInputVariableValuesRequest(createVariableValueRequest(
        decisionDefinition.getKey(), String.valueOf(decisionDefinition.getVersion()), tenantIds
      ))
      .execute();

    Response outputVariableValueResponse = embeddedOptimizeExtension
      .getRequestExecutor()
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .buildDecisionOutputVariableValuesRequest(createVariableValueRequest(
        decisionDefinition.getKey(), String.valueOf(decisionDefinition.getVersion()), tenantIds
      ))
      .execute();
    return Lists.newArrayList(inputVariableNameResponse, inputVariableValueResponse);
  }

  private DecisionVariableNameRequestDto createInputVariableNameRequest(final String decisionDefinitionKey,
                                                                        final String decisionDefinitionVersion,
                                                                        final List<String> tenantIds) {
    DecisionVariableNameRequestDto dto = new DecisionVariableNameRequestDto();
    dto.setDecisionDefinitionKey(decisionDefinitionKey);
    dto.setDecisionDefinitionVersion(decisionDefinitionVersion);
    dto.setTenantIds(tenantIds);
    return dto;
  }

  private DecisionVariableValueRequestDto createVariableValueRequest(final String processDefinitionKey,
                                                                     final String processDefinitionVersion,
                                                                     final List<String> tenantIds) {
    DecisionVariableValueRequestDto dto = new DecisionVariableValueRequestDto();
    dto.setDecisionDefinitionKey(processDefinitionKey);
    dto.setDecisionDefinitionVersion(processDefinitionVersion);
    dto.setTenantIds(tenantIds);
    dto.setVariableId(VARIABLE_NAME);
    dto.setVariableType(VariableType.STRING);
    return dto;
  }

  private DecisionDefinitionEngineDto deploySimpleDecisionDefinition() {
    return deploySimpleDecisionDefinition(null);
  }

  private DecisionDefinitionEngineDto deploySimpleDecisionDefinition(String tenantId) {
    // @formatter:off
    DmnModelInstance modelInstance = DmnModelGenerator
      .create()
        .decision()
          .addInput("input", "input", DecisionTypeRef.STRING)
          .addOutput("output", "output", DecisionTypeRef.STRING)
        .buildDecision()
      .build();
    // @formatter:on
    return engineIntegrationExtension.deployDecisionDefinition(modelInstance, tenantId);
  }

  private void evaluateSimpleDecision(DecisionDefinitionEngineDto decisionDefinition) {
    Map<String, Object> variables = new HashMap<>();
    variables.put(VARIABLE_NAME, VARIABLE_VALUE);
    engineIntegrationExtension.startDecisionInstance(decisionDefinition.getId(), variables);
  }
}
