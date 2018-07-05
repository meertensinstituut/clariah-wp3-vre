import React from "react";
import String from "./string";
import Select from "./select";
import Integer from "./integer";
import PropTypes from 'prop-types';
import './field.css';
import RemoveButton from "./remove-button";
import AddButton from "./add-button";
// import RemoveButton from "./remove-button";

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

    handleRemove = () => () => {
        this.props.onRemove(this.props.param);
    };

    hasAddButton(field) {
        return this.props.bare === false
            && field !== null;
    }

    renderField() {
        let field = null;
        for (let [paramType, classType] of PARAM_TO_ClASS) {
            if (this.props.param.type === paramType) {
                let value = this.props.param.value[this.props.index];
                field = React.createElement(classType, {
                    param: this.props.param,
                    value: value,
                    onChange: this.handleChange()
                });
            }
        }
        return field;
    }

    renderAddButton(field) {
        if (!this.hasAddButton(field)) return null;
        return <AddButton
            canAdd={this.props.param.canAdd}
            onAdd={this.handleAdd}
        />;
    }

    render() {
        let field = this.renderField();
        return (
            <div className="param-field">
                <RemoveButton
                    canRemove={this.props.param.canRemove}
                    onRemove={this.handleRemove}
                />
                {this.renderAddButton(field)}
                <span>
                    <label>{this.props.bare ? null : this.props.param.label}</label>
                    <p>{this.props.param.description}</p>
                </span>
                {field}
            </div>
        );
    }
}

Field.propTypes = {
    param: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    onAdd: PropTypes.func.isRequired,
    onRemove: PropTypes.func.isRequired
};

Field.defaultProps = {
    canAdd: true,
    bare: false
};