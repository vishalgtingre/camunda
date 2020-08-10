/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.test.query.performance;

import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.IdentityDto;
import org.camunda.optimize.dto.optimize.IdentityType;
import org.camunda.optimize.dto.optimize.query.event.EventProcessRoleDto;
import org.camunda.optimize.dto.optimize.query.event.IndexableEventProcessMappingDto;
import org.camunda.optimize.dto.optimize.rest.event.EventProcessMappingResponseDto;
import org.camunda.optimize.service.es.schema.index.events.EventProcessMappingIndex;
import org.camunda.optimize.service.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class EventBasedProcessQueryPerformanceTest extends AbstractQueryPerformanceTest {

  @BeforeEach
  public void init() {
    embeddedOptimizeExtension.getConfigurationService()
      .getEventBasedProcessConfiguration()
      .setAuthorizedUserIds(Collections.singletonList(DEFAULT_USER));
  }

  @Test
  public void testQueryPerformance_getEventBasedProcesses() {
    // given
    final int numberOfEntities = getNumberOfEntities();

    addEventProcessMappingsToOptimize(numberOfEntities);

    // when
    Instant start = Instant.now();
    final List<EventProcessMappingResponseDto> eventBasedProcesses = embeddedOptimizeExtension.getRequestExecutor()
      .buildGetAllEventProcessMappingsRequests()
      .executeAndReturnList(EventProcessMappingResponseDto.class, Response.Status.OK.getStatusCode());
    Instant finish = Instant.now();
    long responseTimeMs = Duration.between(start, finish).toMillis();
    log.info("{} query response time: {}", getTestDisplayName(), responseTimeMs);

    // then
    assertThat(eventBasedProcesses).hasSize(numberOfEntities);
    assertThat(responseTimeMs).isLessThanOrEqualTo(getMaxAllowedQueryTime());
  }

  private void addEventProcessMappingsToOptimize(final int numberOfDifferentEvents) {
    final Map<String, Object> mappingsById = IntStream.range(0, numberOfDifferentEvents)
      .mapToObj(index -> {
        final String mappingId = IdGenerator.getNextId();
        return IndexableEventProcessMappingDto.builder()
          .id(mappingId)
          .name("event based process name")
          .xml("some xml")
          .mappings(Collections.emptyList())
          .eventSources(Collections.emptyList())
          .lastModifier(DEFAULT_USER)
          .lastModified(OffsetDateTime.now())
          .roles(Collections.singletonList(new EventProcessRoleDto(new IdentityDto(DEFAULT_USER, IdentityType.USER))))
          .build();
      })
      .collect(Collectors.toMap(IndexableEventProcessMappingDto::getId, mapping -> mapping));
    addToElasticsearch(
      new EventProcessMappingIndex().getIndexName(),
      mappingsById
    );
  }

}
