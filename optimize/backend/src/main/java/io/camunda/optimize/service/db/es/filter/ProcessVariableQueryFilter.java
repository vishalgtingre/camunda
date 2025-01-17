/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.filter;

import io.camunda.optimize.dto.optimize.query.report.single.filter.data.variable.VariableFilterDataDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class ProcessVariableQueryFilter extends AbstractProcessVariableQueryFilter
    implements QueryFilter<VariableFilterDataDto<?>> {

  @Override
  public void addFilters(
      final BoolQueryBuilder query,
      final List<VariableFilterDataDto<?>> variables,
      final FilterContext filterContext) {
    if (variables != null) {
      List<QueryBuilder> filters = query.filter();
      for (VariableFilterDataDto<?> variable : variables) {
        filters.add(createFilterQueryBuilder(variable, filterContext.getTimezone()));
      }
    }
  }
}
