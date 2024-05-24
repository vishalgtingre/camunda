/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.it.management;

import static io.camunda.zeebe.test.StableValuePredicate.hasStableValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

import feign.FeignException;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.qa.util.actuator.ExportersActuator;
import io.camunda.zeebe.qa.util.cluster.TestCluster;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration;
import io.camunda.zeebe.qa.util.junit.ZeebeIntegration.TestZeebe;
import io.camunda.zeebe.qa.util.topology.ClusterActuatorAssert;
import io.camunda.zeebe.test.util.junit.AutoCloseResources;
import io.camunda.zeebe.test.util.junit.AutoCloseResources.AutoCloseResource;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import java.time.Duration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(2 * 60) // 2 minutes
@ZeebeIntegration
@AutoCloseResources
final class ExportersEndpointIT {
  @TestZeebe
  private final TestCluster cluster =
      TestCluster.builder()
          .useRecordingExporter(true)
          .withBrokersCount(1)
          .withPartitionsCount(1)
          .withReplicationFactor(1)
          .withEmbeddedGateway(true)
          .build();

  @AutoCloseResource private ZeebeClient client;

  @BeforeEach
  void setup() {
    client = cluster.newClientBuilder().build();
  }

  @Test
  void shouldDisableExporter() {
    // given
    // write some events
    client.newPublishMessageCommand().messageName("test").correlationKey("key").send().join();

    // verify recording exporter has seen the records
    final var recordsBeforeDisable =
        Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .during(Duration.ofSeconds(5))
            .until(RecordingExporter.getRecords()::size, hasStableValue());

    assertThat(recordsBeforeDisable).isGreaterThanOrEqualTo(2);

    // when
    final var response =
        ExportersActuator.of(cluster.anyGateway()).disableExporter("recordingExporter");
    Awaitility.await()
        .timeout(Duration.ofSeconds(30))
        .untilAsserted(() -> ClusterActuatorAssert.assertThat(cluster).hasAppliedChanges(response));

    // write more events
    client.newPublishMessageCommand().messageName("test2").correlationKey("key2").send().join();

    // then
    // verify no events are exported to recording exporter
    final var recordsAfterDisable =
        Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .during(Duration.ofSeconds(5))
            .until(RecordingExporter.getRecords()::size, hasStableValue());

    assertThat(recordsBeforeDisable).isEqualTo(recordsAfterDisable);
  }

  @Test
  void shouldFailRequestForNonExistingExporter() {
    // when - then
    assertThatException()
        .isThrownBy(
            () -> ExportersActuator.of(cluster.anyGateway()).disableExporter("nonExistingExporter"))
        .isInstanceOf(FeignException.class)
        .asInstanceOf(InstanceOfAssertFactories.type(FeignException.class))
        .extracting(FeignException::status)
        .isEqualTo(400);
  }
}
