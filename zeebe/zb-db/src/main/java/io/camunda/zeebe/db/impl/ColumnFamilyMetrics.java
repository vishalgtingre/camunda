/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.db.impl;

import io.camunda.zeebe.protocol.EnumValue;
import io.prometheus.client.Histogram;
import io.prometheus.client.Histogram.Child;
import io.prometheus.client.Histogram.Timer;

public final class ColumnFamilyMetrics {

  private static final Histogram LATENCY =
      Histogram.build()
          .namespace("zeebe")
          .name("rocksdb_latency")
          .labelNames("partition", "columnFamily", "operation")
          .help("Latency of RocksDB operations per column family")
          .register();

  private final Child getLatency;
  private final Child putLatency;
  private final Child deleteLatency;
  private final Child iterateLatency;

  public <ColumnFamilyNames extends Enum<? extends EnumValue> & EnumValue> ColumnFamilyMetrics(
      final int partitionId, final ColumnFamilyNames columnFamily) {
    final var partitionLabel = String.valueOf(partitionId);
    final var columnFamilyLabel = columnFamily.name();
    getLatency = LATENCY.labels(partitionLabel, columnFamilyLabel, "get");
    putLatency = LATENCY.labels(partitionLabel, columnFamilyLabel, "put");
    deleteLatency = LATENCY.labels(partitionLabel, columnFamilyLabel, "delete");
    iterateLatency = LATENCY.labels(partitionLabel, columnFamilyLabel, "iterate");
  }

  public Timer measureGetLatency() {
    return getLatency.startTimer();
  }

  public Timer measurePutLatency() {
    return putLatency.startTimer();
  }

  public Timer measureDeleteLatency() {
    return deleteLatency.startTimer();
  }

  public Timer measureIterateLatency() {
    return iterateLatency.startTimer();
  }
}
