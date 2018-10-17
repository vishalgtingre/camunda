import React from 'react';

import Filter from './Filter';

import {mount} from 'enzyme';

jest.mock('components', () => {
  const Dropdown = ({children}) => <p id="dropdown">Dropdown: {children}</p>;
  Dropdown.Option = props => <button {...props}>{props.children}</button>;

  return {
    Dropdown,
    Labeled: props => (
      <div>
        <label id={props.id}>{props.label}</label>
        {props.children}
      </div>
    )
  };
});

jest.mock('./modals', () => {
  return {
    DateFilter: () => 'DateFilter',
    VariableFilter: () => 'VariableFilter',
    NodeFilter: () => 'NodeFilter'
  };
});

jest.mock('./FilterList', () => () => 'FilterList');

it('should contain a list of filters', () => {
  const node = mount(<Filter data={[]} />);

  expect(node).toIncludeText('FilterList');
});

it('should contain a dropdown', () => {
  const node = mount(<Filter data={[]} />);

  expect(node).toIncludeText('Dropdown');
});

it('should not contain any filter modal when no newFilter is selected', () => {
  const node = mount(<Filter data={[]} />);

  expect(node).not.toIncludeText('DateFilter');
  expect(node).not.toIncludeText('VariableFilter');
  expect(node).not.toIncludeText('NodeFilter');
});

it('should contain a filter modal when a newFilter should be created', () => {
  const node = mount(<Filter data={[]} />);

  node.instance().openNewFilterModal('startDate')();

  expect(node).toIncludeText('DateFilter');
});

it('should contain an edit filter modal when a filter should be edited', () => {
  const node = mount(<Filter data={[{type: 'startDate'}]} />);

  node.instance().openEditFilterModal(
    {
      data: {
        operator: 'bar',
        type: 'baz',
        value: 'foo'
      },
      type: 'startDate'
    },
    {
      data: {
        operator: 'foo1',
        type: 'bar1',
        value: 'baz1'
      },
      type: 'startDate'
    }
  )();

  expect(node).toIncludeText('DateFilter');
});

it('should contain a FilterModal component based on the selected new Filter', () => {
  const node = mount(<Filter data={[]} />);

  node.instance().openNewFilterModal('variable')();

  expect(node).toIncludeText('VariableFilter');
  expect(node).not.toIncludeText('DateFilter');
});

it('should contain a EditFilterModal component based on the Filter selected for edition', () => {
  const node = mount(<Filter data={[{type: 'variable'}]} />);

  node.instance().openEditFilterModal({
    data: {
      operator: 'bar',
      type: 'baz',
      value: 'foo'
    },
    type: 'variable'
  })();
  expect(node).toIncludeText('VariableFilter');
  expect(node).not.toIncludeText('DateFilter');
});

it('should add a filter to the list of filters', () => {
  const spy = jest.fn();
  const sampleFilter = {
    data: {
      operator: 'bar',
      type: 'baz',
      value: 'foo'
    },
    type: 'qux'
  };
  const previousFilters = [sampleFilter];

  const node = mount(<Filter data={previousFilters} onChange={spy} />);

  node.instance().addFilter('Filter 2');

  expect(spy.mock.calls[0][0].filter).toEqual([sampleFilter, 'Filter 2']);
});

it('should edit the edited filter', () => {
  const spy = jest.fn();
  const sampleFilter = {
    data: {
      operator: 'bar',
      type: 'baz',
      value: 'foo'
    },
    type: 'qux'
  };

  const filters = [sampleFilter, 'foo'];
  const node = mount(<Filter data={filters} onChange={spy} />);

  node.instance().setState({
    editFilter: sampleFilter
  });

  node.instance().editFilter('bar');

  expect(spy.mock.calls[0][0].filter).toEqual(['bar', 'foo']);
});

it('should remove a filter from the list of filters', () => {
  const spy = jest.fn();
  const previousFilters = ['Filter 1', 'Filter 2', 'Filter 3'];

  const node = mount(<Filter data={previousFilters} onChange={spy} />);

  node.instance().deleteFilter('Filter 2');

  expect(spy.mock.calls[0][0].filter).toEqual(['Filter 1', 'Filter 3']);
});

it('should disable variable and executed flow node filter if no process definition is available', () => {
  const node = mount(<Filter />);

  const buttons = node.find('#dropdown button');
  expect(buttons.find('[children="Start Date"]').prop('disabled')).toBeFalsy();
  expect(buttons.find('[children="Variable"]').prop('disabled')).toBeTruthy();
  expect(buttons.find('[children="Flow Node"]').prop('disabled')).toBeTruthy();
});

it('should remove any previous startDate filters when adding a new date filter', () => {
  const spy = jest.fn();
  const previousFilters = [{type: 'startDate'}];

  const node = mount(<Filter data={previousFilters} onChange={spy} />);

  node.instance().addFilter({type: 'startDate', value: 'new date'});

  expect(spy.mock.calls[0][0].filter).toEqual([{type: 'startDate', value: 'new date'}]);
});

it('should remove any completed/running instances only filters when adding a new completed instances only filter', () => {
  const spy = jest.fn();
  const previousFilters = [{type: 'runningInstancesOnly'}, {type: 'completedInstancesOnly'}];

  const node = mount(<Filter data={previousFilters} onChange={spy} />);

  node.instance().addFilter({type: 'completedInstancesOnly'});

  expect(spy.mock.calls[0][0].filter).toEqual([{type: 'completedInstancesOnly'}]);
});

it('should remove any completed/running instances only filters when adding a new running instances only filter', () => {
  const spy = jest.fn();
  const previousFilters = [{type: 'runningInstancesOnly'}, {type: 'completedInstancesOnly'}];

  const node = mount(<Filter data={previousFilters} onChange={spy} />);

  node.instance().addFilter({type: 'runningInstancesOnly'});

  expect(spy.mock.calls[0][0].filter).toEqual([{type: 'runningInstancesOnly'}]);
});

it('should show the number of process instances in the current Filter', () => {
  const node = mount(<Filter data={[]} />);

  expect(node).not.toIncludeText('in current filter');

  node.setProps({instanceCount: 12});

  expect(node).toIncludeText('12 instances in current filter');
});

it('should remove flow node and variable filter after changing ProcDef', async () => {
  const data = [
    {
      data: 'foo',
      type: 'bar'
    },
    {
      data: 'foo',
      type: 'executedFlowNodes'
    },
    {
      data: 'foo',
      type: 'variable'
    }
  ];
  const spy = jest.fn();
  const node = mount(
    <Filter data={data} processDefinitionKey="key" processDefinitionVersion="1" onChange={spy} />
  );

  node.setProps({
    processDefinitionKey: 'other key'
  });

  expect(spy).toHaveBeenCalledWith({filter: [{data: 'foo', type: 'bar'}]});
});
