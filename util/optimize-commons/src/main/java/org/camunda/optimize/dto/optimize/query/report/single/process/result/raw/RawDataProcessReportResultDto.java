/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.report.single.process.result.raw;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.camunda.optimize.dto.optimize.query.report.SingleReportResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.LimitedResultDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.ResultType;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class RawDataProcessReportResultDto extends SingleReportResultDto implements LimitedResultDto {

  protected List<RawDataProcessInstanceDto> data;
  private Boolean isComplete = true;

  @Override
  public ResultType getType() {
    return ResultType.RAW;
  }

}
