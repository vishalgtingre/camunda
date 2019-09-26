/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.exceptions.evaluation;

import org.camunda.optimize.dto.optimize.rest.AuthorizedReportDefinitionDto;
import org.camunda.optimize.service.exceptions.OptimizeRuntimeException;

public class ReportEvaluationException extends OptimizeRuntimeException {

  protected AuthorizedReportDefinitionDto reportDefinition;

  public ReportEvaluationException() {
    super();
  }

  public ReportEvaluationException(String message) {
    super(message);
  }

  public ReportEvaluationException(String message, Exception e) {
    super(message, e);
  }

  public ReportEvaluationException(AuthorizedReportDefinitionDto reportDefinition, Exception e) {
    super(e.getMessage(), e);
    setReportDefinition(reportDefinition);
  }

  public void setReportDefinition(AuthorizedReportDefinitionDto reportDefinition) {
    this.reportDefinition = reportDefinition;
  }

  public AuthorizedReportDefinitionDto getReportDefinition() {
    return reportDefinition;
  }
}
