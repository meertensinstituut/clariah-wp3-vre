import React from "react";
import String from "./string";
import Select from "./select";
import Integer from "./integer";
import PropTypes from 'prop-types';
import {Button} from "react-bootstrap";
import './field.css';

const PARAM_TO_ClASS = new Map();
PARAM_TO_ClASS.set("string", String);
PARAM_TO_ClASS.set("integer", Integer);
PARAM_TO_ClASS.set("enumeration", Select);

export default class Field extends React.Component {

    constructor(props) {
        super(props);
        this.state = {};
    }

    handleChange = () => (value) => {
        this.props.param.value[this.props.index] = value;
        this.props.onChange(this.props.param);
    };

    handleAdd = () => () => {
        this.props.onAdd(this.props.param);
    };

    hasAddButton(field) {
        return this.props.bare === false
            && this.props.canAdd === true
            && field !== null;
    }

    renderAddButton(field) {
        let button = null;
        if (this.hasAddButton(field)) {
            button = <Button
                bsSize="xsmall"
                bsStyle="success"
                type="button"
                className="pull-right add-btn"
                onClick={this.handleAdd()}
            >
                Add <i className="fa fa-plus-square-o fa-lg"/>
            </Button>;
        }
        return button;
    }

    renderLabels() {
        let labels = null;
        if (this.props.bare === false) {
            labels = <span>
                <label>{this.props.param.label}</label>
                <p>{this.props.param.description}</p>
            </span>;
        }
        return labels;
    }

    renderField() {
        let field = null;
        for (let [paramType, classType] of PARAM_TO_ClASS) {
            if (this.props.param.type === paramType) {
                let value = this.props.param.value[this.props.index];
                field = React.createElement(classType, {
                    param: this.props.param,
                    value: value,
                    onChange: this.handleChange(),
                });
            }
        }
        return field;
    }

    render() {
        let field = this.renderField();
        return (
            <div className="param-field">
                {this.renderAddButton(field)}
                {this.renderLabels()}
                {field}
            </div>
        );
    }
}

Field.propTypes = {
    param: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    onAdd: PropTypes.func.isRequired
};

Field.defaultProps = {
    canAdd: true,
    bare: false
};