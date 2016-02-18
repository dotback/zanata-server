import React from 'react'
import PureRenderMixin from 'react-addons-pure-render-mixin'
import {
  ButtonLink,
  ButtonRound,
  LoaderText,
  Icon,
  Input,
  Modal
} from 'zanata-ui'
import Actions from '../../actions/GlossaryActions'
import StringUtils from '../../utils/StringUtils'
import GlossaryStore from '../../stores/GlossaryStore'

var NewEntryModal = React.createClass({
  propTypes: {
    className: React.PropTypes.string,
    srcLocale: React.PropTypes.shape({
      locale: React.PropTypes.shape({
        localeId: React.PropTypes.string.isRequired,
        displayName: React.PropTypes.string.isRequired,
        alias: React.PropTypes.string.isRequired
      }).isRequired,
      numberOfTerms: React.PropTypes.number.isRequired
    })
  },

  mixins: [PureRenderMixin],

  getInitialState: function() {
    return this._getState();
  },

  _getState: function() {
    return GlossaryStore.getNewEntryState();
  },

  componentDidMount: function() {
    GlossaryStore.addChangeListener(this._onChange);
  },

  componentWillUnmount: function() {
    GlossaryStore.removeChangeListener(this._onChange);
  },

  _onChange: function() {
    this.setState(this._getState());
  },

  _showModal: function () {
    this.setState({show: true})
  },

  _closeModal: function () {
    this.setState(this.getInitialState());
  },

  _save: function () {
    Actions.saveGlossary(this.props.srcLocale.locale.localeId, this.state.term, this.state.pos, this.state.description);
  },

  _onTermChange: function(event) {
    this._setTermValue(event.target.value);
  },

  _onTermReset: function(event) {
    this._setTermValue('');
  },

  _setTermValue: function(value) {
    value = StringUtils.trimLeadingSpace(value);
    var isAllowSave = !StringUtils.isEmptyOrNull(value);
    this.setState({term: value, isAllowSave: isAllowSave});
  },

  _onPosChange: function(event) {
    this.setState({pos: event.target.value});
  },

  _onPosReset: function(event) {
    this.setState({pos: ''});
  },

  _onDescChange: function(event) {
    this.setState({description: event.target.value});
  },

  _onDescReset: function(event) {
    this.setState({description: ''});
  },

  render: function () {
    return (
      <div className={this.props.className}>
        <ButtonLink onClick={this._showModal}>
          <Icon name='plus' className='Mend(re)'/><span className='Hidden--lesm'>New Term</span>
        </ButtonLink>
        <Modal show={this.state.show} onHide={this._closeModal}>
          <Modal.Header>
            <Modal.Title>New Term</Modal.Title>
          </Modal.Header>
          <Modal.Body className='tal'>
            <Input
              margin='mb1/2'
              border='underline'
              label='Term'
              value={this.state.term}
              placeholder='The new term'
              onChange={this._onTermChange}
              onReset={this._onTermReset}
              autoFocus
              />

            <Input
              margin='mb1/2'
              border='underline'
              label='Part of speech'
              value={this.state.pos}
              placeholder='Noun, Verb, etc'
              maxLength='255'
              onChange={this._onPosChange}
              onReset={this._onPosReset}
              />

            <Input
              margin='mb1/2'
              border='underline'
              label='Description'
              value={this.state.description}
              placeholder='The definition of this term'
              maxLength='255'
              onChange={this._onDescChange}
              onReset={this._onDescReset}
              />
          </Modal.Body>
          <Modal.Footer>
            <ButtonLink
              theme={{base: {m: 'Mend(r1)'}}}
              disabled={this.state.isSaving}
              onClick={this._closeModal}>
              Cancel
            </ButtonLink>
            <ButtonRound
              type='primary'
              disabled={!this.state.isAllowSave || this.state.isSaving}
              onClick={this._save}>
              <LoaderText loading={this.state.isSaving} loadingText='Saving'>
                Save
              </LoaderText>
            </ButtonRound>
          </Modal.Footer>
        </Modal>
      </div>);
  }
});

export default NewEntryModal;