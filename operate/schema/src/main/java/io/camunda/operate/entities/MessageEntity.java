/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.entities;

import static io.camunda.operate.schema.indices.IndexDescriptor.DEFAULT_TENANT_ID;

import java.time.OffsetDateTime;
import java.util.Objects;

public class MessageEntity extends OperateZeebeEntity<MessageEntity> {

  private String messageName;
  private String correlationKey;
  private OffsetDateTime publishDate;
  private OffsetDateTime expireDate;
  private OffsetDateTime deadline;
  private Long timeToLive;
  private String messageId;
  private String variables;
  private String tenantId = DEFAULT_TENANT_ID;

  public String getMessageName() {
    return messageName;
  }

  public MessageEntity setMessageName(String messageName) {
    this.messageName = messageName;
    return this;
  }

  public String getCorrelationKey() {
    return correlationKey;
  }

  public MessageEntity setCorrelationKey(String correlationKey) {
    this.correlationKey = correlationKey;
    return this;
  }

  public OffsetDateTime getPublishDate() {
    return publishDate;
  }

  public MessageEntity setPublishDate(OffsetDateTime publishDate) {
    this.publishDate = publishDate;
    return this;
  }

  public OffsetDateTime getExpireDate() {
    return expireDate;
  }

  public MessageEntity setExpireDate(OffsetDateTime expireDate) {
    this.expireDate = expireDate;
    return this;
  }

  public OffsetDateTime getDeadline() {
    return deadline;
  }

  public MessageEntity setDeadline(OffsetDateTime deadline) {
    this.deadline = deadline;
    return this;
  }

  public Long getTimeToLive() {
    return timeToLive;
  }

  public MessageEntity setTimeToLive(Long timeToLive) {
    this.timeToLive = timeToLive;
    return this;
  }

  public String getMessageId() {
    return messageId;
  }

  public MessageEntity setMessageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  public String getVariables() {
    return variables;
  }

  public MessageEntity setVariables(String variables) {
    this.variables = variables;
    return this;
  }

  public String getTenantId() {
    return tenantId;
  }

  public MessageEntity setTenantId(String tenantId) {
    this.tenantId = tenantId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final MessageEntity that = (MessageEntity) o;
    return Objects.equals(messageName, that.messageName)
        && Objects.equals(correlationKey, that.correlationKey)
        && Objects.equals(publishDate, that.publishDate)
        && Objects.equals(expireDate, that.expireDate)
        && Objects.equals(deadline, that.deadline)
        && Objects.equals(timeToLive, that.timeToLive)
        && Objects.equals(messageId, that.messageId)
        && Objects.equals(variables, that.variables)
        && Objects.equals(tenantId, that.tenantId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        messageName,
        correlationKey,
        publishDate,
        expireDate,
        deadline,
        timeToLive,
        messageId,
        variables,
        tenantId);
  }
}
