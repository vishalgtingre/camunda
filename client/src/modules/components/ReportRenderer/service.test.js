/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {processResult} from './service';

it('should process duration reports', () => {
  expect(
    processResult({
      data: {
        groupBy: {},
        view: {
          property: 'duration',
          entity: 'processInstance'
        },
        configuration: {}
      },
      result: {
        type: 'durationMap',
        data: [{key: '2015-03-25T12:00:00Z', value: 4}, {key: '2015-03-26T12:00:00Z', value: 8}]
      }
    })
  ).toEqual({
    data: [{key: '2015-03-25T12:00:00Z', value: 4}, {key: '2015-03-26T12:00:00Z', value: 8}],
    type: 'durationMap'
  });
});

it('should filter hidden flow nodes', () => {
  expect(
    processResult({
      result: {
        data: [{key: 'foo', value: 123}, {key: 'bar', value: 5}]
      },
      data: {
        configuration: {xml: 'fooXml', hiddenNodes: ['foo']},
        visualization: 'line',
        groupBy: {
          type: 'flowNodes',
          value: ''
        },
        view: {property: ''}
      }
    })
  ).toEqual({
    data: [{key: 'bar', value: 5}]
  });
});
