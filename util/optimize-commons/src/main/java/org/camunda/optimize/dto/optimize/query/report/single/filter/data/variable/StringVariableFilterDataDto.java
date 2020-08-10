/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.report.single.filter.data.variable;

import org.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterOperator;
import org.camunda.optimize.dto.optimize.query.report.single.filter.data.variable.data.OperatorMultipleValuesVariableFilterSubDataDto;
import org.camunda.optimize.dto.optimize.query.variable.VariableType;

import java.util.List;
import java.util.Optional;

public class StringVariableFilterDataDto extends OperatorMultipleValuesVariableFilterDataDto {
  protected StringVariableFilterDataDto() {
    this(null, null, null);
  }

  public StringVariableFilterDataDto(final String name, final FilterOperator operator, final List<String> values) {
    super(name, VariableType.STRING, new OperatorMultipleValuesVariableFilterSubDataDto(operator, values));
  }

  public boolean hasContainsOperation() {
    return Optional.ofNullable(this.data.getOperator())
      .map(FilterOperator::isContainsOperation)
      .orElse(false);
  }

  public boolean hasEqualsOperation() {
    return Optional.ofNullable(this.data.getOperator())
      .map(FilterOperator::isEqualsOperation)
      .orElse(false);
  }
}
