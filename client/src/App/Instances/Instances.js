/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React, {Component} from 'react';
import PropTypes from 'prop-types';

import {
  DEFAULT_FILTER,
  DEFAULT_FILTER_CONTROLLED_VALUES
} from 'modules/constants';

// import Diagram from 'modules/components/Diagram';
import VisuallyHiddenH1 from 'modules/components/VisuallyHiddenH1';
import {
  SelectionProvider,
  SelectionConsumer
} from 'modules/contexts/SelectionContext';
import {getInstancesIdsFromSelections} from 'modules/contexts/SelectionContext/service';
import {InstancesPollProvider} from 'modules/contexts/InstancesPollContext';

import Header from '../Header';
import DiagramPanel from './DiagramPanel';
import ListView from './ListView';
import Filters from './Filters';
import Selections from './Selections';

import {
  getWorkflowByVersionFromFilter,
  getWorkflowNameFromFilter
} from './service';

import {getFlowNodes} from 'modules/utils/flowNodes';
import * as Styled from './styled.js';

export default class Instances extends Component {
  static propTypes = {
    filter: PropTypes.shape({
      workflow: PropTypes.string,
      version: PropTypes.string,
      active: PropTypes.bool,
      ids: PropTypes.string,
      startDate: PropTypes.string,
      endDate: PropTypes.string,
      errorMessage: PropTypes.string,
      incidents: PropTypes.bool,
      canceled: PropTypes.bool,
      completed: PropTypes.bool,
      activityId: PropTypes.string,
      variable: PropTypes.shape({
        name: PropTypes.string,
        value: PropTypes.string
      })
    }).isRequired,
    filterCount: PropTypes.number.isRequired,
    groupedWorkflows: PropTypes.object.isRequired,
    workflowInstances: PropTypes.array.isRequired,
    firstElement: PropTypes.number.isRequired,
    onFirstElementChange: PropTypes.func.isRequired,
    sorting: PropTypes.object.isRequired,
    onSort: PropTypes.func.isRequired,
    onFilterChange: PropTypes.func.isRequired,
    onFilterReset: PropTypes.func.isRequired,
    onFlowNodeSelection: PropTypes.func.isRequired,
    diagramModel: PropTypes.shape({
      bpmnElements: PropTypes.object,
      definitions: PropTypes.object
    }).isRequired,
    statistics: PropTypes.array.isRequired,
    onWorkflowInstancesRefresh: PropTypes.func
  };

  render() {
    const {filter, groupedWorkflows} = this.props;
    const currentWorkflowByVersion = getWorkflowByVersionFromFilter({
      filter,
      groupedWorkflows
    });

    const workflowName = getWorkflowNameFromFilter({filter, groupedWorkflows});
    const {ids: selectableIds, flowNodes: selectableFlowNodes} = getFlowNodes(
      this.props.diagramModel.bpmnElements
    );

    return (
      <SelectionProvider
        groupedWorkflows={this.props.groupedWorkflows}
        filter={this.props.filter}
      >
        <SelectionConsumer>
          {selections => (
            <InstancesPollProvider
              onWorkflowInstancesRefresh={this.props.onWorkflowInstancesRefresh}
              onSelectionsRefresh={selections.onInstancesInSelectionsRefresh}
              visibleIdsInListView={this.props.workflowInstances.map(x => x.id)}
              visibleIdsInSelections={getInstancesIdsFromSelections(
                selections.selections
              )}
            >
              <Header
                active="instances"
                filter={this.props.filter}
                filterCount={this.props.filterCount}
                onFilterReset={this.props.onFilterReset}
              />
              <Styled.Instances>
                <VisuallyHiddenH1>Camunda Operate Instances</VisuallyHiddenH1>
                <Styled.Content>
                  <Styled.FilterSection>
                    <Filters
                      selectableFlowNodes={selectableFlowNodes}
                      groupedWorkflows={this.props.groupedWorkflows}
                      filter={{
                        ...DEFAULT_FILTER_CONTROLLED_VALUES,
                        ...this.props.filter
                      }}
                      filterCount={this.props.filterCount}
                      onFilterReset={() =>
                        this.props.onFilterReset(DEFAULT_FILTER)
                      }
                      onFilterChange={this.props.onFilterChange}
                    />
                  </Styled.FilterSection>
                  <Styled.SplitPane
                    titles={{top: 'Workflow', bottom: 'Instances'}}
                  >
                    <Styled.Pane>
                      <DiagramPanel
                        workflowName={workflowName}
                        onFlowNodeSelection={this.props.onFlowNodeSelection}
                        noWorkflowSelected={!filter.workflow}
                        noVersionSelected={filter.version === 'all'}
                        definitions={this.props.diagramModel.definitions}
                        flowNodesStatistics={this.props.statistics}
                        selectedFlowNodeId={this.props.filter.activityId}
                        selectableFlowNodes={selectableIds}
                        currentWorkflowByVersion={currentWorkflowByVersion}
                      />
                    </Styled.Pane>
                    <ListView
                      instances={this.props.workflowInstances}
                      filter={this.props.filter}
                      filterCount={this.props.filterCount}
                      onSort={this.props.onSort}
                      sorting={this.props.sorting}
                      firstElement={this.props.firstElement}
                      onFirstElementChange={this.props.onFirstElementChange}
                      onWorkflowInstancesRefresh={
                        this.props.onWorkflowInstancesRefresh
                      }
                    />
                  </Styled.SplitPane>
                </Styled.Content>
                <Selections />
              </Styled.Instances>
            </InstancesPollProvider>
          )}
        </SelectionConsumer>
      </SelectionProvider>
    );
  }
}
