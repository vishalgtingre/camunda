/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.es.filter;

import io.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterDataDto;
import java.util.List;
import org.elasticsearch.index.query.BoolQueryBuilder;

public interface QueryFilter<FILTER extends FilterDataDto> {
  void addFilters(BoolQueryBuilder query, List<FILTER> filter, FilterContext filterContext);
}
