/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.importing;

import java.util.concurrent.CompletableFuture;

public interface EngineImportMediator {

  CompletableFuture<Void> runImport();

  long getBackoffTimeInMs();

  void resetBackoff();

  boolean canImport();

  boolean hasPendingImportJobs();

  void shutdown();

}