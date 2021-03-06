import React, { PropTypes } from 'react';
import "./CardInfoItem.scss"

class CardInfoItem extends React.Component {

  static propTypes = {
    title: PropTypes.string.isRequired,
    value: PropTypes.number,
    givenClass: PropTypes.string
  };

  constructor({ givenClass, title, value }) {
    super();
    this.givenClass = givenClass;
    this.title = title;
    this.value = value;
  };

  get className() {
    const baseClassName = "card-info-item";
    if (this.givenClass)
      return baseClassName + " " + this.givenClass;
    else
      return baseClassName;
  }

  render() {
    return <div className={this.className}>
      <span>{this.title}</span>
      <div className="info-oval">
        <span>{this.value !== null ? this.value : "\u00a0"}</span>
      </div>
    </div>
  }

};

export default CardInfoItem;


