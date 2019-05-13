/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.command.aggregations;

import org.camunda.optimize.dto.optimize.query.report.single.configuration.AggregationType;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;

import static org.camunda.optimize.service.es.report.command.util.ElasticsearchAggregationResultMappingUtil.mapToLong;
import static org.elasticsearch.search.aggregations.AggregationBuilders.avg;

public class AvgAggregation implements AggregationStrategy {
  private static final String AVG_DURATION_AGGREGATION = "avgAggregatedDuration";

  @Override
  public long getValue(final Aggregations aggs) {
    final Avg aggregation = aggs.get(AVG_DURATION_AGGREGATION);
    return mapToLong(aggregation.getValue());
  }

  @Override
  public AggregationBuilder getAggregationBuilder(final String fieldName) {
    return avg(AVG_DURATION_AGGREGATION).field(fieldName);
  }

  @Override
  public AggregationType getAggregationType() {
    return AggregationType.AVERAGE;
  }
}