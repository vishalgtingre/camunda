/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {useEffect, useState} from 'react';
import {Form, Field} from 'react-final-form';
import {useHistory} from 'react-router-dom';
import {isEqual} from 'lodash';
import {
  FiltersForm,
  Row,
  VariableHeader,
  ResetButtonContainer,
  Fields,
  StatesHeader,
  InstanceStates,
  ProcessHeader,
} from './styled';
import {ProcessField} from './ProcessField';
import {ProcessVersionField} from './ProcessVersionField';
import {FlowNodeField} from './FlowNodeField';
import {CheckboxGroup} from './CheckboxGroup';
import Button from 'modules/components/Button';
import {AutoSubmit} from './AutoSubmit';
import {
  validateDateCharacters,
  validateDateComplete,
  validateOperationIdCharacters,
  validateOperationIdComplete,
  validateVariableNameComplete,
  validateVariableValueComplete,
  validateIdsCharacters,
  validateIdsNotTooLong,
  validatesIdsComplete,
  validateParentInstanceIdComplete,
  validateParentInstanceIdNotTooLong,
  validateParentInstanceIdCharacters,
} from './validators';
import {mergeValidators} from 'modules/utils/validators/mergeValidators';
import {
  getFilters,
  updateFiltersSearchString,
  FiltersType,
} from 'modules/utils/filter';
import {storeStateLocally} from 'modules/utils/localStorage';
import {FiltersPanel} from './FiltersPanel';
import {JSONEditorModal} from 'modules/components/JSONEditorModal';
import {TextField} from 'modules/components/TextField';

const Filters: React.FC = () => {
  const history = useHistory();
  const [isModalVisible, setIsModalVisible] = useState(false);

  const initialValues: FiltersType = {
    active: true,
    incidents: true,
  };

  function setFiltersToURL(filters: FiltersType) {
    history.push({
      ...history.location,
      search: updateFiltersSearchString(history.location.search, filters),
    });
  }

  useEffect(() => {
    storeStateLocally({
      filters: getFilters(history.location.search),
    });
  }, [history.location.search]);

  return (
    <FiltersPanel>
      <Form<FiltersType>
        onSubmit={(values) => {
          setFiltersToURL(values);
        }}
        initialValues={getFilters(history.location.search)}
      >
        {({handleSubmit, form, values}) => (
          <FiltersForm onSubmit={handleSubmit}>
            <Fields>
              <AutoSubmit
                fieldsToSkipTimeout={[
                  'process',
                  'version',
                  'flowNodeId',
                  'active',
                  'incidents',
                  'completed',
                  'canceled',
                ]}
              />
              <ProcessHeader appearance="emphasis">Process</ProcessHeader>
              <Row>
                <ProcessField />
              </Row>
              <Row>
                <ProcessVersionField />
              </Row>
              <Row>
                <FlowNodeField />
              </Row>
              <Row>
                <Field
                  name="ids"
                  validate={mergeValidators(
                    validateIdsCharacters,
                    validateIdsNotTooLong,
                    validatesIdsComplete
                  )}
                >
                  {({input}) => (
                    <TextField
                      {...input}
                      type="multiline"
                      data-testid="filter-instance-ids"
                      label="Instance Id(s)"
                      placeholder="separated by space or comma"
                      rows={1}
                      shouldDebounceError={false}
                    />
                  )}
                </Field>
              </Row>
              <Row>
                <Field
                  name="parentInstanceId"
                  validate={mergeValidators(
                    validateParentInstanceIdCharacters,
                    validateParentInstanceIdNotTooLong,
                    validateParentInstanceIdComplete
                  )}
                >
                  {({input}) => (
                    <TextField
                      {...input}
                      type="text"
                      data-testid="filter-parent-instance-id"
                      label="Parent Instance Id"
                      shouldDebounceError={false}
                    />
                  )}
                </Field>
              </Row>
              <Row>
                <Field name="errorMessage">
                  {({input}) => (
                    <TextField
                      {...input}
                      type="text"
                      data-testid="filter-error-message"
                      label="Error Message"
                      shouldDebounceError={false}
                    />
                  )}
                </Field>
              </Row>
              <Row>
                <Field
                  name="startDate"
                  validate={mergeValidators(
                    validateDateCharacters,
                    validateDateComplete
                  )}
                >
                  {({input}) => (
                    <TextField
                      {...input}
                      type="text"
                      data-testid="filter-start-date"
                      label="Start Date"
                      placeholder="YYYY-MM-DD hh:mm:ss"
                      shouldDebounceError={false}
                    />
                  )}
                </Field>
              </Row>
              <Row>
                <Field
                  name="endDate"
                  validate={mergeValidators(
                    validateDateCharacters,
                    validateDateComplete
                  )}
                >
                  {({input}) => (
                    <TextField
                      {...input}
                      type="text"
                      data-testid="filter-end-date"
                      label="End Date"
                      placeholder="YYYY-MM-DD hh:mm:ss"
                      shouldDebounceError={false}
                    />
                  )}
                </Field>
              </Row>
              <VariableHeader appearance="emphasis">Variable</VariableHeader>
              <Row>
                <Field
                  name="variableName"
                  validate={validateVariableNameComplete}
                >
                  {({input, meta}) => (
                    <TextField
                      {...input}
                      type="text"
                      data-testid="filter-variable-name"
                      label="Name"
                      shouldDebounceError={!meta.dirty && form.getState().dirty}
                    />
                  )}
                </Field>
              </Row>
              <Row>
                <Field
                  name="variableValue"
                  validate={validateVariableValueComplete}
                >
                  {({input, meta}) => (
                    <TextField
                      {...input}
                      type="text"
                      placeholder="in JSON format"
                      data-testid="filter-variable-value"
                      label="Value"
                      fieldSuffix={{
                        type: 'icon',
                        icon: 'window',
                        press: () => {
                          setIsModalVisible(true);
                        },
                        tooltip: 'Open JSON editor modal',
                      }}
                      shouldDebounceError={!meta.dirty && form.getState().dirty}
                    />
                  )}
                </Field>
              </Row>
              <JSONEditorModal
                title={`Edit Variable Value`}
                value={values?.variableValue}
                onClose={() => {
                  setIsModalVisible(false);
                }}
                onSave={(value) => {
                  form.change('variableValue', value);
                  setIsModalVisible(false);
                }}
                isModalVisible={isModalVisible}
              />
              <Row>
                <Field
                  name="operationId"
                  validate={mergeValidators(
                    validateOperationIdCharacters,
                    validateOperationIdComplete
                  )}
                >
                  {({input}) => (
                    <TextField
                      {...input}
                      type="text"
                      data-testid="filter-operation-id"
                      label="Operation Id"
                      shouldDebounceError={false}
                    />
                  )}
                </Field>
              </Row>
              <InstanceStates>
                <StatesHeader appearance="emphasis">
                  Instance States
                </StatesHeader>
                <CheckboxGroup
                  groupLabel="Running Instances"
                  dataTestId="filter-running-instances"
                  items={[
                    {
                      label: 'Active',
                      name: 'active',
                      icon: {icon: 'state:ok', color: 'success'},
                    },
                    {
                      label: 'Incidents',
                      name: 'incidents',
                      icon: {icon: 'state:incident', color: 'danger'},
                    },
                  ]}
                />
                <CheckboxGroup
                  groupLabel="Finished Instances"
                  dataTestId="filter-finished-instances"
                  items={[
                    {
                      label: 'Completed',
                      name: 'completed',
                      icon: {icon: 'state:completed', color: 'medium'},
                    },
                    {
                      label: 'Canceled',
                      name: 'canceled',
                      icon: {icon: 'stop', color: 'dark'},
                    },
                  ]}
                />
              </InstanceStates>
            </Fields>
            <ResetButtonContainer>
              <Button
                title="Reset Filters"
                size="small"
                disabled={isEqual(initialValues, values)}
                type="reset"
                onClick={() => {
                  form.reset();
                  setFiltersToURL(initialValues);
                }}
              >
                Reset Filters
              </Button>
            </ResetButtonContainer>
          </FiltersForm>
        )}
      </Form>
    </FiltersPanel>
  );
};

export {Filters};
