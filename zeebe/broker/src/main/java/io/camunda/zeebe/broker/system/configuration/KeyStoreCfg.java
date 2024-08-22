/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.broker.system.configuration;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

public final class KeyStoreCfg implements ConfigurationEntry {
  private File filePath;
  private String password;

  @Override
  public void init(final BrokerCfg globalConfig, final String brokerBase) {
    final var brokerBasePath = Path.of(brokerBase);
    if (filePath != null) {
      filePath = brokerBasePath.resolve(filePath.toPath()).toFile();
    }
  }

  public File getFilePath() {
    return filePath;
  }

  public KeyStoreCfg setFilePath(final File filePath) {
    this.filePath = filePath;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public KeyStoreCfg setPassword(final String password) {
    this.password = password;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(filePath, password);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final KeyStoreCfg that = (KeyStoreCfg) o;
    return Objects.equals(filePath, that.filePath) && Objects.equals(password, that.password);
  }

  @Override
  public String toString() {
    final var passStr = password == null ? "" : "*****";
    return "KeyStoreCfg{" + "filePath=" + filePath + ", password='" + passStr + '\'' + '}';
  }
}