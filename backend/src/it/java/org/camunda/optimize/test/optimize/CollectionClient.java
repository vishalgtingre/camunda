/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.test.optimize;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.camunda.optimize.OptimizeRequestExecutor;
import org.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import org.camunda.optimize.dto.optimize.DefinitionType;
import org.camunda.optimize.dto.optimize.query.IdDto;
import org.camunda.optimize.dto.optimize.query.alert.AlertDefinitionDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionDefinitionRestDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionRoleDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionRoleRestDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionRoleUpdateDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionScopeEntryDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionScopeEntryUpdateDto;
import org.camunda.optimize.dto.optimize.query.collection.PartialCollectionDefinitionDto;
import org.camunda.optimize.dto.optimize.query.entity.EntityDto;
import org.camunda.optimize.dto.optimize.rest.AuthorizedCollectionDefinitionRestDto;
import org.camunda.optimize.dto.optimize.rest.AuthorizedReportDefinitionDto;
import org.camunda.optimize.dto.optimize.rest.ConflictResponseDto;
import org.camunda.optimize.dto.optimize.rest.collection.CollectionScopeEntryRestDto;
import org.camunda.optimize.dto.optimize.rest.sorting.EntitySorter;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.optimize.dto.optimize.DefinitionType.DECISION;
import static org.camunda.optimize.dto.optimize.DefinitionType.PROCESS;
import static org.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static org.camunda.optimize.test.it.extension.TestEmbeddedCamundaOptimize.DEFAULT_USERNAME;

@AllArgsConstructor
@Builder
public class CollectionClient {

  public static final String DEFAULT_DEFINITION_KEY = "defaultScopeDefinitionKey";
  public static final String DEFAULT_TENANT = null;
  public static final List<String> DEFAULT_TENANTS = singletonList(DEFAULT_TENANT);
  public static final String PRIVATE_COLLECTION_ID = null;

  private final Supplier<OptimizeRequestExecutor> requestExecutorSupplier;

  public String createNewCollectionForAllDefinitionTypes() {
    final String collectionId = createNewCollectionWithDefaultScope(PROCESS);
    createScopeWithTenants(collectionId, DEFAULT_DEFINITION_KEY, DEFAULT_TENANTS, DECISION);
    return collectionId;
  }

  public String createNewCollectionWithDefaultProcessScope() {
    return createNewCollectionWithDefaultScope(PROCESS);
  }

  public String createNewCollectionWithDefaultDecisionScope() {
    return createNewCollectionWithDefaultScope(DECISION);
  }

  public String createNewCollectionWithDefaultScope(final DefinitionType definitionType) {
    final String collectionId = createNewCollection();
    createScopeWithTenants(collectionId, DEFAULT_DEFINITION_KEY, DEFAULT_TENANTS, definitionType);
    return collectionId;
  }

  public String createNewCollectionWithProcessScope(final ProcessInstanceEngineDto instanceEngineDto) {
    final String collectionId = createNewCollection();
    createScopeWithTenants(
      collectionId,
      instanceEngineDto.getProcessDefinitionKey(),
      singletonList(instanceEngineDto.getTenantId()),
      PROCESS
    );
    return collectionId;
  }

  public String createNewCollectionWithProcessScope(final ProcessDefinitionEngineDto definitionEngineDto) {
    final String collectionId = createNewCollection();
    createScopeWithTenants(
      collectionId,
      definitionEngineDto.getKey(),
      singletonList(definitionEngineDto.getTenantId().orElse(null)),
      PROCESS
    );
    return collectionId;
  }

  public String createNewCollection() {
    return getRequestExecutor()
      .buildCreateCollectionRequest()
      .execute(IdDto.class, Response.Status.OK.getStatusCode())
      .getId();
  }

  public String createNewCollection(final String user, final String password) {
    return getRequestExecutor()
      .withUserAuthentication(user, password)
      .buildCreateCollectionRequest()
      .execute(IdDto.class, Response.Status.OK.getStatusCode())
      .getId();
  }

  public void updateCollection(final String collectionId, final PartialCollectionDefinitionDto updatedCollection) {
    updateCollectionAsUser(collectionId, updatedCollection, DEFAULT_USERNAME, DEFAULT_USERNAME);
  }

  public void updateCollectionAsUser(final String collectionId, final PartialCollectionDefinitionDto updatedCollection,
                                     final String username, final String password) {
    getRequestExecutor()
      .buildUpdatePartialCollectionRequest(collectionId, updatedCollection)
      .withUserAuthentication(username, password)
      .execute(Response.Status.NO_CONTENT.getStatusCode());
  }

  public CollectionDefinitionRestDto getCollectionById(final String collectionId) {
    return getRequestExecutor()
      .buildGetCollectionRequest(collectionId)
      .execute(CollectionDefinitionRestDto.class, Response.Status.OK.getStatusCode());
  }

  public AuthorizedCollectionDefinitionRestDto getAuthorizedCollectionById(final String collectionId) {
    return getAuthorizedCollectionById(collectionId, DEFAULT_USERNAME, DEFAULT_USERNAME);
  }


  public AuthorizedCollectionDefinitionRestDto getAuthorizedCollectionById(final String collectionId,
                                                                           final String username,
                                                                           final String password) {
    return getRequestExecutor()
      .buildGetCollectionRequest(collectionId)
      .withUserAuthentication(username, password)
      .execute(AuthorizedCollectionDefinitionRestDto.class, Response.Status.OK.getStatusCode());
  }

  public List<AuthorizedReportDefinitionDto> getReportsForCollection(final String collectionId) {
    return getRequestExecutor()
      .buildGetReportsForCollectionRequest(collectionId)
      .executeAndReturnList(
        AuthorizedReportDefinitionDto.class,
        200
      );
  }

  public List<AuthorizedReportDefinitionDto> getReportsForCollectionAsUser(final String collectionId,
                                                                           final String username,
                                                                           final String password) {
    return getRequestExecutor()
      .buildGetReportsForCollectionRequest(collectionId)
      .withUserAuthentication(username, password)
      .executeAndReturnList(
        AuthorizedReportDefinitionDto.class,
        200
      );
  }

  public List<AlertDefinitionDto> getAlertsForCollection(final String collectionId) {
    return getRequestExecutor()
      .buildGetAlertsForCollectionRequest(collectionId)
      .executeAndReturnList(AlertDefinitionDto.class, 200);
  }

  public List<EntityDto> getEntitiesForCollection(final String collectionId) {
    return getEntitiesForCollection(collectionId, null);
  }

  public List<EntityDto> getEntitiesForCollection(final String collectionId, final String user, final String pass) {
    return getEntitiesForCollection(collectionId, null, user, pass);
  }

  public List<EntityDto> getEntitiesForCollection(final String collectionId, final EntitySorter entitySorter) {
    return getEntitiesForCollection(collectionId, entitySorter, DEFAULT_USERNAME, DEFAULT_USERNAME);
  }

  public List<EntityDto> getEntitiesForCollection(final String collectionId, final EntitySorter entitySorter,
                                                  final String username, final String password) {
    return getRequestExecutor()
      .buildGetCollectionEntitiesRequest(collectionId, entitySorter)
      .withUserAuthentication(username, password)
      .executeAndReturnList(EntityDto.class, Response.Status.OK.getStatusCode());
  }

  public void updateCollectionScopeAsKermit(final String collectionId,
                                            final String scopeEntryId,
                                            final List<String> tenants) {
    getRequestExecutor()
      .buildUpdateCollectionScopeEntryRequest(
        collectionId,
        scopeEntryId,
        new CollectionScopeEntryUpdateDto(tenants)
      )
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .execute(Response.Status.NO_CONTENT.getStatusCode());
  }

  public void createScopeForCollection(final String collectionId,
                                       final String definitionKey,
                                       final DefinitionType definitionType) {
    addScopeEntryToCollection(collectionId, createSimpleScopeEntry(definitionKey, definitionType));
  }

  public List<CollectionScopeEntryRestDto> getCollectionScope(final String collectionId) {
    return getRequestExecutor()
      .buildGetScopeForCollectionRequest(collectionId)
      .execute(new TypeReference<List<CollectionScopeEntryRestDto>>() {
      });
  }

  public List<CollectionScopeEntryRestDto> getCollectionScopeForKermit(final String collectionId) {
    return getRequestExecutor()
      .buildGetScopeForCollectionRequest(collectionId)
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .execute(new TypeReference<List<CollectionScopeEntryRestDto>>() {
      });
  }

  public void createScopeWithTenants(final String collectionId, final String definitionKey,
                                     final List<String> tenants, final DefinitionType definitionType) {
    final CollectionScopeEntryDto scopeEntry = new CollectionScopeEntryDto(definitionType, definitionKey, tenants);
    addScopeEntryToCollection(collectionId, scopeEntry);
  }

  public Response deleteCollection(String id) {
    return getRequestExecutor()
      .buildDeleteCollectionRequest(id, true)
      .execute();
  }

  public CollectionScopeEntryDto createSimpleScopeEntry(final String definitionKey,
                                                        final DefinitionType definitionType) {
    return new CollectionScopeEntryDto(definitionType, definitionKey, DEFAULT_TENANTS);
  }

  public CollectionScopeEntryDto createSimpleScopeEntry(final String definitionKey,
                                                        final DefinitionType definitionType,
                                                        final List<String> tenantIds) {
    return new CollectionScopeEntryDto(definitionType, definitionKey, tenantIds);
  }

  public void addScopeEntryToCollection(final String collectionId, final String definitionKey,
                                        final DefinitionType definitionType,
                                        final List<String> tenantIds) {
    addScopeEntryToCollection(collectionId, createSimpleScopeEntry(definitionKey, definitionType, tenantIds));
  }

  public void addScopeEntryToCollection(final String collectionId, final CollectionScopeEntryDto entry) {
    addScopeEntriesToCollection(collectionId, singletonList(entry));
  }

  public void addScopeEntriesToCollection(final String collectionId, final List<CollectionScopeEntryDto> entries) {
    getRequestExecutor()
      .buildAddScopeEntriesToCollectionRequest(collectionId, entries)
      .execute(Response.Status.NO_CONTENT.getStatusCode());
  }

  public void addScopeEntryToCollectionAsUser(final String collectionId, final CollectionScopeEntryDto entry,
                                              final String user, final String password) {
    getRequestExecutor()
      .buildAddScopeEntriesToCollectionRequest(collectionId, singletonList(entry))
      .withUserAuthentication(user, password)
      .execute(IdDto.class, Response.Status.NO_CONTENT.getStatusCode());
  }

  public void updateCollectionScopeEntry(final String collectionId,
                                         final CollectionScopeEntryUpdateDto scopeEntryUpdate,
                                         final String scopeEntryId) {
    Response response = getRequestExecutor()
      .buildUpdateCollectionScopeEntryRequest(
        collectionId,
        scopeEntryId,
        scopeEntryUpdate,
        true
      )
      .execute();
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
  }

  public void deleteScopeEntry(final String collectionId, final CollectionScopeEntryDto scopeEntry,
                               final Boolean force) {
    Response response = getRequestExecutor()
      .buildDeleteScopeEntryFromCollectionRequest(collectionId, scopeEntry.getId(), force)
      .execute();
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
  }

  public IdDto addRoleToCollection(final String collectionId, final CollectionRoleDto roleDto) {
    return addRoleToCollectionAsUser(collectionId, roleDto, DEFAULT_USERNAME, DEFAULT_USERNAME);
  }

  public IdDto addRoleToCollectionAsUser(final String collectionId,
                                         final CollectionRoleDto roleDto,
                                         final String username,
                                         final String password) {
    return getRequestExecutor()
      .buildAddRoleToCollectionRequest(collectionId, roleDto)
      .withUserAuthentication(username, password)
      .execute(IdDto.class, Response.Status.OK.getStatusCode());
  }

  public void updateCollectionRoleAsUser(final String collectionId,
                                         final String roleId,
                                         final CollectionRoleUpdateDto updateDto,
                                         final String username,
                                         final String password) {
    getRequestExecutor()
      .buildUpdateRoleToCollectionRequest(collectionId, roleId, updateDto)
      .withUserAuthentication(username, password)
      .execute(Response.Status.NO_CONTENT.getStatusCode());
  }

  public void deleteCollectionRoleAsUser(final String collectionId, final String roleId, final String username,
                                         final String password) {
    getRequestExecutor()
      .buildDeleteRoleToCollectionRequest(collectionId, roleId)
      .withUserAuthentication(username, password)
      .execute(Response.Status.NO_CONTENT.getStatusCode());
  }

  public OptimizeRequestExecutor getAlertsRequest(final String userId,
                                                  final String password,
                                                  final String collectionId) {
    return getRequestExecutor()
      .buildGetAlertsForCollectionRequest(collectionId)
      .withUserAuthentication(userId, password);
  }

  public List<CollectionRoleRestDto> getCollectionRolesAsUser(final String collectionId,
                                                              final String username,
                                                              final String password) {
    return getRequestExecutor()
      .buildGetRolesToCollectionRequest(collectionId)
      .withUserAuthentication(username, password)
      .executeAndReturnList(CollectionRoleRestDto.class, Response.Status.OK.getStatusCode());
  }

  public List<CollectionRoleRestDto> getCollectionRoles(final String collectionId) {
    return getRequestExecutor()
      .buildGetRolesToCollectionRequest(collectionId)
      .executeAndReturnList(CollectionRoleRestDto.class, Response.Status.OK.getStatusCode());
  }

  public List<IdDto> getCollectionRoleIdDtos(final String collectionId) {
    return getRequestExecutor()
      .buildGetRolesToCollectionRequest(collectionId)
      .executeAndReturnList(IdDto.class, Response.Status.OK.getStatusCode());
  }

  public CollectionDefinitionRestDto copyCollection(final String collectionId) {
    return copyCollection(collectionId, null);
  }

  public CollectionDefinitionRestDto copyCollection(final String collectionId, final String newName) {
    OptimizeRequestExecutor executor = getRequestExecutor()
      .buildCopyCollectionRequest(collectionId);
    if (newName != null) {
      executor.addSingleQueryParam("name", newName);
    }
    return executor.execute(CollectionDefinitionRestDto.class, Response.Status.OK.getStatusCode());
  }

  public ConflictResponseDto getDeleteCollectionConflicts(final String collectionId) {
    return getRequestExecutor()
      .buildGetCollectionDeleteConflictsRequest(collectionId)
      .execute(ConflictResponseDto.class, Response.Status.OK.getStatusCode());
  }

  public ConflictResponseDto getScopeDeletionConflicts(final String collectionId, final String scopeEntryId) {
    return getRequestExecutor()
      .buildGetScopeDeletionConflictsRequest(collectionId, scopeEntryId)
      .execute(ConflictResponseDto.class, Response.Status.OK.getStatusCode());
  }

  public void assertCollectionIsDeleted(final String idToDelete) {
    getRequestExecutor()
      .buildGetCollectionRequest(idToDelete)
      .execute(Response.Status.NOT_FOUND.getStatusCode());
  }

  private OptimizeRequestExecutor getRequestExecutor() {
    return requestExecutorSupplier.get();
  }
}
