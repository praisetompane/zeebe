import React from 'react';
import PropTypes from 'prop-types';

import Panel from 'modules/components/Panel';

import {applyOperation} from 'modules/api/instances';
import {BADGE_TYPE, DIRECTION, OPERATION_TYPE} from 'modules/constants';
import {getSelectionById} from 'modules/utils/selection';

import SelectionList from './SelectionList';

import * as Styled from './styled';

export default class Selections extends React.Component {
  static propTypes = {
    openSelection: PropTypes.number,
    selections: PropTypes.array,
    rollingSelectionIndex: PropTypes.number,
    selectionCount: PropTypes.number,
    instancesInSelectionsCount: PropTypes.number,
    onStateChange: PropTypes.func.isRequired,
    storeStateLocally: PropTypes.func.isRequired,
    filter: PropTypes.object
  };

  executeBatchOperation = async (openSelectionId, operation) => {
    const {selections} = this.props;
    const {queries} = getSelectionById(selections, openSelectionId);

    try {
      await applyOperation(operation, queries);
    } catch (e) {
      console.log(e);
    }
  };

  handleToggleSelection = selectionId => {
    this.props.onStateChange({
      openSelection:
        selectionId !== this.props.openSelection ? selectionId : null
    });
  };

  handleDeleteSelection = async deleteId => {
    const {selections, instancesInSelectionsCount, selectionCount} = this.props;

    const selectionToRemove = getSelectionById(selections, deleteId);

    // remove the selection
    selections.splice(selectionToRemove.index, 1);

    await this.props.onStateChange({
      selections,
      instancesInSelectionsCount:
        instancesInSelectionsCount - selectionToRemove.totalCount,
      selectionCount: selectionCount - 1 || 0
    });
    this.props.storeStateLocally({
      selections,
      instancesInSelectionsCount: this.props.instancesInSelectionsCount,
      selectionCount: this.props.selectionCount
    });
  };

  handleRetrySelection = openSelectionId => {
    this.executeBatchOperation(openSelectionId, OPERATION_TYPE.UPDATE_RETRIES);
  };

  handleCancelSelection = openSelectionId => {
    this.executeBatchOperation(openSelectionId, OPERATION_TYPE.CANCEL);
  };

  render() {
    return (
      <Styled.Selections>
        <Panel isRounded>
          <Styled.SelectionHeader isRounded>
            <span>Selections</span>
            <Styled.Badge
              type={BADGE_TYPE.COMBOSELECTION}
              badgeContent={this.props.instancesInSelectionsCount}
              circleContent={this.props.selectionCount}
            />
          </Styled.SelectionHeader>
          <Panel.Body>
            <SelectionList
              selections={this.props.selections}
              openSelection={this.props.openSelection}
              onToggleSelection={this.handleToggleSelection}
              onDeleteSelection={this.handleDeleteSelection}
              onRetrySelection={this.handleRetrySelection}
              onCancelSelection={this.handleCancelSelection}
            />
          </Panel.Body>
          <Styled.RightExpandButton
            direction={DIRECTION.RIGHT}
            isExpanded={true}
          />
          <Panel.Footer />
        </Panel>
      </Styled.Selections>
    );
  }
}
