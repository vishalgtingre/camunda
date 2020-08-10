/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.query.report.AdditionalProcessReportEvaluationFilterDto;
import org.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import org.camunda.optimize.dto.optimize.rest.AuthorizedReportEvaluationResult;
import org.camunda.optimize.service.es.report.AuthorizationCheckReportEvaluationHandler;
import org.camunda.optimize.service.es.report.ReportEvaluationInfo;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@RequiredArgsConstructor
@Component
@Slf4j
public class ReportEvaluationService {

  private final AuthorizationCheckReportEvaluationHandler reportEvaluator;

  public AuthorizedReportEvaluationResult evaluateSavedReportWithAdditionalFilters(final String userId,
                                                                                   final ZoneId timezone,
                                                                                   final String reportId,
                                                                                   final AdditionalProcessReportEvaluationFilterDto filterDto) {
    ReportEvaluationInfo evaluationInfo = ReportEvaluationInfo.builder(reportId)
      .userId(userId)
      .timezone(timezone)
      .additionalFilters(filterDto)
      .build();
    // auth is handled in evaluator as it also handles single reports of a combined report
    return reportEvaluator.evaluateReport(evaluationInfo);
  }

  public AuthorizedReportEvaluationResult evaluateUnsavedReport(final String userId,
                                                                final ZoneId timezone,
                                                                final ReportDefinitionDto reportDefinition) {
    // reset owner, it's not relevant for authorization given a full unsaved report definition is provided
    final String originalOwner = reportDefinition.getOwner();
    reportDefinition.setOwner(null);
    ReportEvaluationInfo evaluationInfo = ReportEvaluationInfo.builder(reportDefinition)
      .userId(userId)
      .timezone(timezone)
      .build();
    // auth is handled in evaluator as it also handles single reports of a combined report
    final AuthorizedReportEvaluationResult authorizedReportEvaluationResult =
      reportEvaluator.evaluateReport(evaluationInfo);
    // reflect back original owner value as provided in the request into the response
    authorizedReportEvaluationResult.getEvaluationResult().getReportDefinition().setOwner(originalOwner);
    return authorizedReportEvaluationResult;
  }
}