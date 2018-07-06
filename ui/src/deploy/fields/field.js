import React from "react";
import String from "./string";
import Select from "./select";
import Integer from "./integer";
import PropTypes from 'prop-types';
import './field.css';
import AddButton from "./add-button";

const PARAM_TO_CLASS = new Map();
PARAM_TO_CLASS.set("string", String);
PARAM_TO_CLASS.set("integer", Integer);
PARAM_TO_CLASS.set("enumeration", Select);

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

    renderFieldByParamType() {
        let field = null;
        for (let [paramType, classType] of PARAM_TO_CLASS) {
            if (this.props.param.type === paramType) {
                let value = this.props.param.value[this.props.index];
                field = React.createElement(classType, {
                    param: this.props.param,
                    value: value,
                    onChange: this.handleChange(),
                    onAdd: this.handleAdd,
                    canRemove: this.canRemoveField(),
                    onRemove: this.handleRemove,
                });
            }
        }
        return field;
    }

    canRemoveField() {
        return this.props.param.canRemove && !Array.isArray(this.props.param.params);
    }

    renderAddButton(field) {
        if (!this.hasAddButton(field)) return null;
        return <AddButton
            canAdd={this.props.param.canAdd}
            onAdd={this.handleAdd}
        />;
    }

    render() {
        let field = this.renderFieldByParamType();
        return (
            <div className="param-field">
                {this.renderAddButton(field)}
                <span>
                    <label>{this.props.bare ? null : this.props.param.label}</label>
                    <p>{this.props.bare ? null : this.props.param.description}</p>
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