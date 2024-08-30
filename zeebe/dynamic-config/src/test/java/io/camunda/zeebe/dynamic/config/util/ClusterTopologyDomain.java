/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.dynamic.config.util;

import io.atomix.cluster.MemberId;
import io.camunda.zeebe.dynamic.config.state.ClusterChangePlan;
import io.camunda.zeebe.dynamic.config.state.ClusterConfiguration;
import io.camunda.zeebe.dynamic.config.state.ClusterConfigurationChangeOperation;
import io.camunda.zeebe.dynamic.config.state.CompletedChange;
import io.camunda.zeebe.dynamic.config.state.MemberState;
import io.camunda.zeebe.dynamic.config.state.RoutingState;
import io.camunda.zeebe.dynamic.config.state.RoutingState.MessageCorrelation;
import io.camunda.zeebe.util.ReflectUtil;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Provide;
import net.jqwik.api.domains.DomainContextBase;

/**
 * Contains all arbitraries needed to generate a {@link ClusterConfiguration}. The topology is not
 * semantically correct (e.g. contains operations for members that don't exist) but all fields
 * should have valid values.
 */
public final class ClusterTopologyDomain extends DomainContextBase {

  @Provide
  Arbitrary<ClusterConfiguration> clusterTopologies() {
    // Combine arbitraries (instead of just using `Arbitraries.forType(ClusterTopology.class)`
    // here so that we have control over the version. Version must be greater than 0 for
    // `ClusterTopology#isUninitialized` to return false.
    final var arbitraryVersion = Arbitraries.integers().greaterOrEqual(0);
    final var arbitraryMembers =
        Arbitraries.maps(memberIds(), Arbitraries.forType(MemberState.class).enableRecursion())
            .ofMaxSize(10);
    final var arbitraryCompletedChange =
        Arbitraries.forType(CompletedChange.class).enableRecursion().optional();
    final var arbitraryChangePlan =
        Arbitraries.forType(ClusterChangePlan.class).enableRecursion().optional();
    final var arbitraryRoutingState = routingStates().optional();
    return Combinators.combine(
            arbitraryVersion,
            arbitraryMembers,
            arbitraryCompletedChange,
            arbitraryChangePlan,
            arbitraryRoutingState)
        .as(ClusterConfiguration::new);
  }

  @Provide
  Arbitrary<RoutingState> routingStates() {
    final var version = Arbitraries.longs().greaterOrEqual(0);
    final var activePartitions = Arbitraries.integers().greaterOrEqual(1).set().ofMaxSize(10);
    return Combinators.combine(version, activePartitions, messageCorrelation())
        .as(RoutingState::new);
  }

  @Provide
  Arbitrary<MessageCorrelation> messageCorrelation() {
    return Arbitraries.of(
            ReflectUtil.implementationsOfSealedInterface(MessageCorrelation.class).toList())
        .flatMap(Arbitraries::forType);
  }

  @Provide
  Arbitrary<ClusterConfigurationChangeOperation> topologyChangeOperations() {
    // jqwik does not support sealed classes yet, so we have to use reflection to get all possible
    // types. See https://github.com/jqwik-team/jqwik/issues/523
    return Arbitraries.of(
            ReflectUtil.implementationsOfSealedInterface(ClusterConfigurationChangeOperation.class)
                .toList())
        .flatMap(Arbitraries::forType);
  }

  @Provide
  Arbitrary<MemberId> memberIds() {
    return Arbitraries.integers().greaterOrEqual(0).map(id -> MemberId.from(id.toString()));
  }
}