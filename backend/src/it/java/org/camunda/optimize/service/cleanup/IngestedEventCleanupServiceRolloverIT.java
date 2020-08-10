/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.cleanup;

import org.camunda.optimize.dto.optimize.query.event.EventDto;
import org.camunda.optimize.dto.optimize.rest.CloudEventDto;
import org.camunda.optimize.service.util.configuration.cleanup.IngestedEventCleanupConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class IngestedEventCleanupServiceRolloverIT extends AbstractEngineDataCleanupIT {

  @BeforeEach
  @AfterEach
  public void beforeAndAfter() {
    cleanUpEventIndices();
  }

  @Test
  public void testIngestedEventCleanup_afterRollover() {
    // given
    getIngestedEventCleanupConfiguration().setEnabled(true);
    final Instant timestampLessThanTtl = getTimestampLessThanIngestedEventsTtl();
    final List<CloudEventDto> eventsToCleanupIngestedBeforeRollover =
      eventClient.ingestEventBatchWithTimestamp(timestampLessThanTtl, 10);
    final List<CloudEventDto> eventsToKeepIngestedBeforeRollover =
      eventClient.ingestEventBatchWithTimestamp(Instant.now().minusSeconds(10L), 10);

    embeddedOptimizeExtension.getConfigurationService().getEventIndexRolloverConfiguration().setMaxIndexSizeGB(0);
    embeddedOptimizeExtension.getEventIndexRolloverService().triggerRollover();

    final List<CloudEventDto> eventsToCleanupIngestedAfterRollover =
      eventClient.ingestEventBatchWithTimestamp(timestampLessThanTtl, 10);
    final List<CloudEventDto> eventsToKeepIngestedAfterRollover =
      eventClient.ingestEventBatchWithTimestamp(Instant.now().minusSeconds(10L), 10);
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    embeddedOptimizeExtension.getCleanupScheduler().runCleanup();
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // then
    assertThat(elasticSearchIntegrationTestExtension.getAllStoredExternalEvents())
      .extracting(EventDto::getId)
      .containsExactlyInAnyOrderElementsOf(
        Stream.concat(eventsToKeepIngestedBeforeRollover.stream(), eventsToKeepIngestedAfterRollover.stream())
          .map(CloudEventDto::getId).collect(Collectors.toSet())
      );
  }

  private IngestedEventCleanupConfiguration getIngestedEventCleanupConfiguration() {
    return embeddedOptimizeExtension.getConfigurationService()
      .getCleanupServiceConfiguration()
      .getIngestedEventCleanupConfiguration();
  }

}
