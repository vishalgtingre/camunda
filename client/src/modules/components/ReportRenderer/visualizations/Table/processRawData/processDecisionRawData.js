/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {sortColumns, cockpitLink, getNoDataMessage, isVisibleColumn} from './service';
import {t} from 'translation';

export default function processDecisionRawData(
  {
    report: {
      data: {
        configuration: {
          tableColumns,
          columnOrder = {instanceProps: [], variables: [], inputVariables: [], outputVariables: []},
        },
      },
      result: {data: result},
    },
  },
  endpoints = {}
) {
  const instanceProps = Object.keys(result[0]).filter(
    (entry) =>
      entry !== 'inputVariables' &&
      entry !== 'outputVariables' &&
      isVisibleColumn(entry, tableColumns)
  );

  const inputVariables = Object.keys(result[0].inputVariables).filter((entry) =>
    isVisibleColumn('input:' + entry, tableColumns)
  );
  const outputVariables = Object.keys(result[0].outputVariables).filter((entry) =>
    isVisibleColumn('output:' + entry, tableColumns)
  );

  if (instanceProps.length + inputVariables.length + outputVariables.length === 0) {
    return getNoDataMessage();
  }

  const body = result.map((instance) => {
    const propertyValues = instanceProps.map((entry) => {
      if (entry === 'decisionInstanceId') {
        return cockpitLink(endpoints, instance, 'decision');
      }
      return instance[entry];
    });
    const inputVariableValues = inputVariables.map((entry) => {
      const value = instance.inputVariables[entry].value;
      if (value === null) {
        return '';
      }
      return value.toString();
    });
    const outputVariableValues = outputVariables.map((entry) => {
      const output = instance.outputVariables[entry];
      if (output && output.values) {
        return output.values.join(', ');
      }
      return '';
    });

    return [...propertyValues, ...inputVariableValues, ...outputVariableValues];
  });

  const head = instanceProps.map((key) => t('report.table.rawData.' + key));

  if (inputVariables.length > 0) {
    head.push({
      label: t('report.variables.input'),
      columns: inputVariables.map((key) => {
        const {name, id} = result[0].inputVariables[key];
        return {label: name || id, id: key};
      }),
    });
  }
  if (outputVariables.length > 0) {
    head.push({
      label: t('report.variables.output'),
      columns: outputVariables.map((key) => {
        const {name, id} = result[0].outputVariables[key];
        return {label: name || id, id: key};
      }),
    });
  }

  const {sortedHead, sortedBody} = sortColumns(head, body, columnOrder);

  return {head: sortedHead, body: sortedBody};
}
