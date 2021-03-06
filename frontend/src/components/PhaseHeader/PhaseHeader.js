import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import TextField from 'material-ui/TextField';
import { changeWip } from '../../actions';

class PhaseHeader extends React.Component {
  static propTypes = {
    name: PropTypes.string.isRequired,
    wipLimit: PropTypes.number,
    style: PropTypes.object
  };

  constructor({ id, name, wipLimit, titleStyle, borderBottomStyle }) {
    super();
    this.state = {value: wipLimit};
    this.id = id;
    this.titleStyle = titleStyle;
    this.borderBottomStyle = borderBottomStyle;
    this.name = name;
    this.wipLimit = wipLimit;

    this.handleChange = this.handleChange.bind(this);
  };

  handleChange(event) {
    this.setState({value: event.target.value});
    const newWipLimit = event.target.value;
    if (newWipLimit !== '') {
      this.props.dispatch(changeWip(this.id, event.target.value));
    }
  }

  get inputStyles() {
    return {
      mainElem: {
        width: 50,
        height: 35,
        marginLeft: 20,
        border: '2px dotted'
      },
      textField: {
        textAlign: 'center',
        fontWeight: 700,
        fontSize: '1.3em'
      },
      hint: {
        bottom: 3,
        left: 8
      }
    };
  }

  render() {
    return (
      <div className="phase-header" style={this.borderBottomStyle}>
        <div className="align-wrapper">
          <div className="phase-name" style={this.titleStyle}>{this.name}</div>
          { !this.wipLimit && <div className="wip-limit">No WIP limit</div> }
          { this.wipLimit &&
          <form>
            <label>WIP Limit</label>
            <TextField
              name={this.name}
              underlineShow={false}
              hintText="WIP"
              value={this.state.value} onChange={this.handleChange}
              style={this.inputStyles.mainElem}
              inputStyle={this.inputStyles.textField}
              hintStyle={this.inputStyles.hint}
            />
          </form>
          }
        </div>
      </div>
    )
  }

}

export default connect()(PhaseHeader);
