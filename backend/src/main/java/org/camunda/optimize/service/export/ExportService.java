/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.export;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import org.camunda.optimize.dto.optimize.rest.AuthorizedReportEvaluationResult;
import org.camunda.optimize.dto.optimize.rest.pagination.PaginationDto;
import org.camunda.optimize.service.es.report.AuthorizationCheckReportEvaluationHandler;
import org.camunda.optimize.service.es.report.ReportEvaluationInfo;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.springframework.stereotype.Component;

import javax.ws.rs.NotFoundException;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Component
@Slf4j
public class ExportService {

  public static final Integer DEFAULT_RECORD_LIMIT = 1_000;

  private final AuthorizationCheckReportEvaluationHandler reportEvaluationHandler;
  private final ConfigurationService configurationService;

  public Optional<byte[]> getCsvBytesForEvaluatedReportResult(final String userId,
                                                              final String reportId,
                                                              final ZoneId timezone) {
    log.debug("Exporting report with id [{}] as csv.", reportId);
    Integer exportCsvLimit = Optional.ofNullable(configurationService.getExportCsvLimit()).orElse(DEFAULT_RECORD_LIMIT);

    try {
      ReportEvaluationInfo evaluationInfo = ReportEvaluationInfo.builder(reportId)
        .userId(userId)
        .timezone(timezone)
        .pagination(buildExportPaginationDto(exportCsvLimit))
        .isExport(true)
        .build();
      final AuthorizedReportEvaluationResult reportResult = reportEvaluationHandler.evaluateReport(evaluationInfo);
      final List<String[]> resultAsCsv = reportResult.getEvaluationResult().getResultAsCsv(exportCsvLimit, 0, timezone);
      return Optional.ofNullable(CSVUtils.mapCsvLinesToCsvBytes(resultAsCsv));
    } catch (NotFoundException e) {
      log.debug("Could not find report with id {} to export the result to csv!", reportId, e);
      return Optional.empty();
    } catch (Exception e) {
      log.error("Could not evaluate report with id {} to export the result to csv!", reportId, e);
      throw e;
    }
  }

  public byte[] getCsvBytesForEvaluatedReportResult(final String userId,
                                                    final ReportDefinitionDto<?> reportDefinition,
                                                    final ZoneId timezone) {
    log.debug("Exporting provided report definition as csv.");
    Integer exportCsvLimit = Optional.ofNullable(configurationService.getExportCsvLimit()).orElse(DEFAULT_RECORD_LIMIT);

    try {
      ReportEvaluationInfo evaluationInfo = ReportEvaluationInfo.builder(reportDefinition)
        .userId(userId)
        .timezone(timezone)
        .pagination(buildExportPaginationDto(exportCsvLimit))
        .isExport(true)
        .build();
      final AuthorizedReportEvaluationResult reportResult =
        reportEvaluationHandler.evaluateReport(evaluationInfo);
      final List<String[]> resultAsCsv = reportResult.getEvaluationResult()
        .getResultAsCsv(exportCsvLimit, 0, timezone);
      return CSVUtils.mapCsvLinesToCsvBytes(resultAsCsv);
    } catch (Exception e) {
      log.error("Could not evaluate report to export the result to csv!", e);
      throw e;
    }
  }

  private PaginationDto buildExportPaginationDto(final Integer exportCsvLimit) {
    final PaginationDto paginationDto = new PaginationDto();
    paginationDto.setOffset(0);
    paginationDto.setLimit(exportCsvLimit);
    return paginationDto;
  }

}
