/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package org.camunda.optimize.service.db.repository.os;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.datasource.DataSourceDto;
import org.camunda.optimize.dto.optimize.index.AllEntitiesBasedImportIndexDto;
import org.camunda.optimize.dto.optimize.index.ImportIndexDto;
import org.camunda.optimize.dto.optimize.index.PositionBasedImportIndexDto;
import org.camunda.optimize.dto.optimize.index.TimestampBasedImportIndexDto;
import org.camunda.optimize.service.db.os.OptimizeOpenSearchClient;
import org.camunda.optimize.service.db.repository.ImportRepository;
import org.camunda.optimize.service.db.schema.OptimizeIndexNameService;
import org.camunda.optimize.service.util.DatabaseHelper;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.camunda.optimize.service.util.configuration.condition.OpenSearchCondition;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static org.camunda.optimize.service.db.DatabaseConstants.IMPORT_INDEX_INDEX_NAME;
import static org.camunda.optimize.service.db.DatabaseConstants.LIST_FETCH_LIMIT;
import static org.camunda.optimize.service.db.DatabaseConstants.POSITION_BASED_IMPORT_INDEX_NAME;
import static org.camunda.optimize.service.db.DatabaseConstants.TIMESTAMP_BASED_IMPORT_INDEX_NAME;
import static org.camunda.optimize.service.db.os.externalcode.client.dsl.QueryDSL.stringTerms;
import static org.camunda.optimize.service.db.schema.index.index.TimestampBasedImportIndex.DB_TYPE_INDEX_REFERS_TO;

@Slf4j
@Component
@AllArgsConstructor
@Conditional(OpenSearchCondition.class)
public class ImportRepositoryOS implements ImportRepository {
  private final OptimizeOpenSearchClient osClient;
  private final ConfigurationService configurationService;
  private final OptimizeIndexNameService indexNameService;

  @Override
  public List<TimestampBasedImportIndexDto> getAllTimestampBasedImportIndicesForTypes(List<String> indexTypes) {
    log.debug("Fetching timestamp based import indices of types '{}'", indexTypes);

    final SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
      .index(indexNameService.getOptimizeIndexAliasForIndex(TIMESTAMP_BASED_IMPORT_INDEX_NAME))
      .query(stringTerms(DB_TYPE_INDEX_REFERS_TO, indexTypes))
      .size(LIST_FETCH_LIMIT);

    return osClient.searchValues(requestBuilder, TimestampBasedImportIndexDto.class);
  }

  @Override
  public <T extends ImportIndexDto<D>, D extends DataSourceDto> Optional<T> getImportIndex(
    final String indexName,
    final String indexType,
    final Class<T> importDTOClass,
    final String typeIndexComesFrom,
    final D dataSourceDto
  ) {
    log.debug("Fetching {} import index of type '{}'", indexType, typeIndexComesFrom);
    final GetResponse<T> response = osClient.get(
      indexNameService.getOptimizeIndexAliasForIndex(indexName),
      DatabaseHelper.constructKey(typeIndexComesFrom, dataSourceDto),
      importDTOClass,
      format("Could not fetch %s import index", indexType)
    );

    if(response.found()) {
      return Optional.ofNullable(response.source());
    } else {
      log.debug(
        "Was not able to retrieve {} import index for type [{}] and engine [{}] from opensearch.",
        indexType,
        typeIndexComesFrom,
        dataSourceDto
      );
      return Optional.empty();
    }
  }

  @Override
  public void importPositionBasedIndices(final String importItemName, final List<PositionBasedImportIndexDto> importIndexDtos) {
    osClient.doImportBulkRequestWithList(
      importItemName,
      importIndexDtos,
      this::addPositionBasedImportIndexRequest,
      configurationService.getSkipDataAfterNestedDocLimitReached()
    );
  }

  private BulkOperation addPositionBasedImportIndexRequest(PositionBasedImportIndexDto optimizeDto) {
    log.debug(
      "Writing position based import index of type [{}] with position [{}] to opensearch",
      optimizeDto.getEsTypeIndexRefersTo(), optimizeDto.getPositionOfLastEntity()
    );
    // leaving the prefix "es" although it is valid for ES and OS,
    // since changing this would require data migration and the cost/benefit of the change is not worth the effort
    return new BulkOperation.Builder()
      .index(
        new IndexOperation.Builder<PositionBasedImportIndexDto>()
          .index(indexNameService.getOptimizeIndexAliasForIndex(POSITION_BASED_IMPORT_INDEX_NAME))
          .id(DatabaseHelper.constructKey(optimizeDto.getEsTypeIndexRefersTo(), optimizeDto.getDataSource()))
          .document(optimizeDto)
          .build()
      )
      .build();
  }

  @Override
  public Optional<AllEntitiesBasedImportIndexDto> getImportIndex(final String id) {
    final GetResponse<AllEntitiesBasedImportIndexDto> response = osClient.get(
      indexNameService.getOptimizeIndexAliasForIndex(IMPORT_INDEX_INDEX_NAME),
      id,
      AllEntitiesBasedImportIndexDto.class,
      format("Was not able to retrieve import index of [%s].", id)
    );

    if(response.found()) {
      return Optional.ofNullable(response.source());
    } else {
      log.debug(
        "Was not able to retrieve import index for type '{}' from Opensearch. Desired index does not exist.",
        id
      );
      return Optional.empty();
    }
  }
}
