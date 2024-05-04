/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.util;

import io.micrometer.core.instrument.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FutureHelper {
  public static <R> CompletableFuture<R> withTimer(
      Timer timer, Supplier<CompletableFuture<R>> supplier) {
    final var t = Timer.start();
    return supplier.get().whenComplete((response, e) -> t.stop(timer));
  }
}
